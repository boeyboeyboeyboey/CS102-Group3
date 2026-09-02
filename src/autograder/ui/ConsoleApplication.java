package autograder.ui;

import autograder.config.AppConfig;
import autograder.model.BatchResult;
import autograder.model.QuestionResult;
import autograder.model.StudentGrade;
import autograder.report.CsvCodec;
import autograder.report.CsvScoreSheetReportGenerator;
import autograder.report.ScoreSheetRepository;
import autograder.service.api.AnomalyDetector;
import autograder.service.api.BatchAnalyzer;
import autograder.service.api.ProgressListener;
import autograder.service.grading.BatchGradingService;
import autograder.service.grading.DefaultGradingEngine;
import autograder.service.process.ExternalProcessRunner;
import autograder.service.process.JavaCompilerService;
import autograder.service.submission.PrecedenceIdentityResolver;
import autograder.service.submission.SecureZipExtractor;
import autograder.service.submission.SubmissionAnomalyDetector;
import autograder.service.submission.SubmissionNormalizer;
import autograder.service.testing.IsolatedJvmTestExecutor;
import autograder.service.testing.LegacyJavaTesterProvider;
import autograder.service.testing.SentinelScoreParser;
import autograder.util.FileTree;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Composition root and console entry point for the auto-grading application. */
public final class ConsoleApplication {
    private ConsoleApplication() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        Path configPath;
        try {
            configPath = parseConfigPath(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Configuration error: " + exception.getMessage());
            printUsage();
            return 2;
        }

        Path batchWorkspace = null;
        try {
            AppConfig config = AppConfig.load(configPath);
            validateInputs(config);
            Path workspaceRoot = config.getPath("working.directory");
            Files.createDirectories(workspaceRoot);
            batchWorkspace = Files.createTempDirectory(workspaceRoot, "run-");

            ProgressListener progress = new ConsoleProgressListener();
            CsvCodec csvCodec = new CsvCodec();
            ScoreSheetRepository rosterRepository = new ScoreSheetRepository(
                    config.getPath("scoresheet.input"), csvCodec);
            ExternalProcessRunner processRunner = new ExternalProcessRunner(
                    config.getPositiveInt("process.output.max.bytes"));
            JavaCompilerService compiler = new JavaCompilerService(
                    config.getRequired("java.compiler.command"), processRunner);
            IsolatedJvmTestExecutor testExecutor = new IsolatedJvmTestExecutor(
                    config.getRequired("java.runtime.command"), processRunner,
                    new SentinelScoreParser());
            DefaultGradingEngine gradingEngine = new DefaultGradingEngine(config,
                    batchWorkspace, compiler, new LegacyJavaTesterProvider(), testExecutor);

            List<AnomalyDetector> detectors = List.of(new SubmissionAnomalyDetector(
                    config.getPositiveInt("submission.search.max.depth")));
            List<BatchAnalyzer> batchAnalyzers = new ArrayList<>();
            CsvScoreSheetReportGenerator reportGenerator = new CsvScoreSheetReportGenerator(
                    config.getPath("scoresheet.input"), config.getPath("scoresheet.output"),
                    csvCodec);
            BatchGradingService batchService = new BatchGradingService(config,
                    rosterRepository.loadRoster(),
                    new SecureZipExtractor(config.getPositiveInt("zip.max.entries"),
                            config.getPositiveLong("zip.max.uncompressed.bytes")),
                    new PrecedenceIdentityResolver(
                            config.getPositiveInt("submission.search.max.depth")),
                    detectors,
                    new SubmissionNormalizer(
                            config.getPositiveInt("submission.search.max.depth")),
                    gradingEngine, batchAnalyzers, reportGenerator, progress, batchWorkspace);

            BatchResult result = batchService.run();
            printSummary(result);
            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Grading was interrupted.");
            return 130;
        } catch (Exception exception) {
            System.err.println("Fatal error: " + exception.getMessage());
            return 1;
        } finally {
            if (batchWorkspace != null) {
                cleanupWorkspace(configPath, batchWorkspace);
            }
        }
    }

    private static Path parseConfigPath(String[] args) {
        if (args.length == 0) {
            return Path.of("config.properties");
        }
        if (args.length == 2 && "--config".equals(args[0])) {
            return Path.of(args[1]);
        }
        throw new IllegalArgumentException("Expected no arguments or --config <file>");
    }

    private static void validateInputs(AppConfig config) {
        if (!Files.isDirectory(config.getPath("submissions.directory"))) {
            throw new IllegalArgumentException("Submission directory not found");
        }
        if (!Files.isDirectory(config.getPath("testers.directory"))) {
            throw new IllegalArgumentException("Tester directory not found");
        }
        if (!Files.isRegularFile(config.getPath("scoresheet.input"))) {
            throw new IllegalArgumentException("Input scoresheet not found");
        }
    }

    private static void cleanupWorkspace(Path configPath, Path batchWorkspace) {
        try {
            AppConfig config = AppConfig.load(configPath);
            if (config.getBoolean("working.cleanup")) {
                Path root = config.getPath("working.directory").toAbsolutePath().normalize();
                Path target = batchWorkspace.toAbsolutePath().normalize();
                if (target.startsWith(root) && !target.equals(root)) {
                    FileTree.deleteRecursively(target);
                }
            }
        } catch (Exception exception) {
            System.err.println("Warning: could not clean temporary workspace: "
                    + exception.getMessage());
        }
    }

    private static void printSummary(BatchResult result) {
        System.out.println();
        System.out.println("Final results");
        System.out.println("-------------");
        for (StudentGrade grade : result.getGrades()) {
            StringBuilder line = new StringBuilder(grade.getStudent().getUsername());
            line.append(": ");
            for (QuestionResult question : grade.getQuestionResults().values()) {
                line.append(question.getQuestionId()).append('=')
                        .append(format(question.getScore())).append("  ");
            }
            line.append("Total=").append(format(grade.getTotalScore()));
            System.out.println(line);
        }
        System.out.println("Scoresheet: " + result.getReportPath().toAbsolutePath());
    }

    private static String format(double score) {
        return String.format(Locale.ROOT, "%.1f", score);
    }

    private static void printUsage() {
        System.err.println("Usage: ./run.sh [application arguments are not required]");
    }
}
