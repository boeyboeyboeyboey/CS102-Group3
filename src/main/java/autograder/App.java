package autograder;

import autograder.analysis.boilerplate.StarterCodeDetector;
import autograder.api.BoilerplateDetector;
import autograder.api.FeedbackGenerator;
import autograder.api.HtmlReportGenerator;
import autograder.api.PlagiarismDetector;
import autograder.api.ProgressListener;
import autograder.config.AppConfig;
import autograder.config.BoilerplateConfig;
import autograder.config.ExecutionConfig;
import autograder.config.FeedbackConfig;
import autograder.config.HtmlReportConfig;
import autograder.config.PlagiarismConfig;
import autograder.grading.GradingPipeline;
import autograder.grading.GradingRun;
import autograder.model.GradingReportContext;
import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.plagiarism.NGramPlagiarismDetector;
import autograder.report.feedback.MarkdownFeedbackGenerator;
import autograder.report.html.OfflineHtmlReportGenerator;
import autograder.submission.SubmissionProcessor;
import autograder.ui.DashboardSettings;
import autograder.ui.TerminalCancellation;
import autograder.ui.TerminalDashboard;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args) {
        if (args.length == 1 && args[0].equals("--help")) {
            printUsage();
            return 0;
        }
        if (args.length != 1) {
            printUsage();
            return 1;
        }

        TerminalCancellation cancellation = null;
        try {
            Path configFile = Path.of(args[0]);
            AppConfig config = AppConfig.load(configFile);
            ExecutionConfig execution = ExecutionConfig.load(configFile, config);
            PlagiarismConfig plagiarism = PlagiarismConfig.load(configFile);
            DashboardSettings dashboard = DashboardSettings.load(configFile);
            Console console = System.console();
            ProgressListener progress = new TerminalDashboard(System.out, dashboard.mode(),
                    console != null, System.getenv());
            cancellation = TerminalCancellation.start(console, Thread.currentThread(), System.out);
            PlagiarismDetector detector = (submissions, assessment) -> List.of();
            if (plagiarism.enabled()) {
                detector = new NGramPlagiarismDetector(plagiarism);
            }
            GradingPipeline grading = new GradingPipeline(config, execution, detector, progress);
            HtmlReportConfig html = HtmlReportConfig.load(configFile);
            FeedbackConfig feedback = FeedbackConfig.load(configFile);
            BoilerplateConfig boilerplate = BoilerplateConfig.load(configFile);
            grading.validateEnvironment();
            System.out.println("Preparing submissions for " + config.getAssessment().getName());
            SubmissionProcessor processor = new SubmissionProcessor(config, progress);
            List<SubmissionResult> results = processor.processBatch();
            if (results.isEmpty()) {
                System.err.println("No submission archives matched the configured input directory and pattern");
                return 1;
            }
            printResults(results);
            System.out.println("Diagnostics: " + config.getAnomalyLog());
            System.out.println("Corrections: " + config.getCorrectionLog());
            GradingRun run = grading.grade(results);
            for (StudentGrade grade : run.batch().getGrades()) {
                String reason = run.withheld().get(grade.getSubmission());
                System.out.println(grade.getSubmission().getIdentity().getLabel() + " | "
                        + (reason == null ? "Score: " + grade.getTotalScore()
                        : "WITHHELD: " + reason));
            }
            System.out.println("Score report: " + run.directory().resolve(execution.reportFile()));
            System.out.println("Per-case diagnostics: " + run.directory().resolve("cases.csv"));
            System.out.println("Export decisions and review warnings: "
                    + run.directory().resolve("submissions.csv"));
            System.out.println("Withheld or absent students retain their original score-sheet rows");
            printSimilarity(run, plagiarism);
            checkCancelled();
            reportProgress(progress, "reporting", "Generating optional reports", 1, 2);
            int reportExit = generateReports(run, boilerplate,
                    new StarterCodeDetector(config.getAssessment(), boilerplate),
                    html, new OfflineHtmlReportGenerator(html.file()), feedback, new MarkdownFeedbackGenerator(
                    config.getAssessment(), execution.caseTimeoutMillis(), feedback.maxDiagnosticLines(),
                    config.getRosterPrefix(), config.getRosterLastNamePlaceholder()));
            checkCancelled();
            reportProgress(progress, "reporting", reportExit == 0 ? "Reports ready"
                    : "Reports completed with errors", 2, 2);
            return reportExit;
        } catch (InterruptedException | CancellationException exception) {
            Thread.currentThread().interrupt();
            System.err.println("grading cancelled");
            return 130;
        } catch (Exception exception) {
            System.err.println("could not complete grading: " + exception.getMessage());
            return 1;
        } finally {
            if (cancellation != null) {
                cancellation.close();
            }
        }
    }

    private static void checkCancelled() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("grading cancelled");
        }
    }

    private static void reportProgress(ProgressListener listener, String stage, String item,
                                       int completed, int total) {
        try {
            listener.onProgress(stage, item, completed, total);
        } catch (RuntimeException exception) {
            System.err.println("progress listener failed; continuing without progress updates: "
                    + exception.getMessage());
        }
    }

    static int generateReports(GradingRun run, BoilerplateConfig boilerplate, BoilerplateDetector detector,
                               HtmlReportConfig html, HtmlReportGenerator htmlGenerator,
                               FeedbackConfig feedback, FeedbackGenerator feedbackGenerator) {
        StarterAnalysis.Result analysis = StarterAnalysis.analyze(run, boilerplate, detector);
        int htmlExit = generateHtmlReport(analysis.run(), html, htmlGenerator);
        int feedbackExit = generateFeedback(analysis.run(), feedback, feedbackGenerator);
        return Math.max(analysis.exitCode(), Math.max(htmlExit, feedbackExit));
    }

    static int generateFeedback(GradingRun run, FeedbackConfig config, FeedbackGenerator generator) {
        if (!config.enabled()) {
            return 0;
        }
        Path directory = run.directory().resolve(config.directory());
        int written = 0;
        int failed = 0;
        for (StudentGrade grade : run.batch().getGrades()) {
            try {
                generator.generate(grade, directory);
                written++;
            } catch (IOException | RuntimeException exception) {
                failed++;
                System.err.println("could not write feedback for "
                        + grade.getSubmission().getArchivePath().getFileName() + ": " + exception.getMessage());
            }
        }
        System.out.println("Student feedback: " + written + " Markdown files in " + directory);
        if (failed > 0) {
            System.err.println(failed + " feedback files could not be written; grading and CSV exports "
                    + "are complete in " + run.directory());
            return 1;
        }
        return 0;
    }

    static int generateHtmlReport(GradingRun run, HtmlReportConfig config,
                                 HtmlReportGenerator generator) {
        if (!config.enabled()) {
            return 0;
        }
        try {
            Path report = generator.generate(new GradingReportContext(run.batch(), run.withheld()),
                    run.directory());
            System.out.println("HTML dashboard: " + report);
            return 0;
        } catch (IOException exception) {
            System.err.println("could not write HTML dashboard " + run.directory().resolve(config.file())
                    + ": " + exception.getMessage());
            System.err.println("Grading and CSV exports are complete; reports remain in " + run.directory());
            return 1;
        }
    }

    private static void printResults(List<SubmissionResult> results) {
        int ready = 0;
        int reviewRequired = 0;
        int failed = 0;
        System.out.println("Prepared " + results.size() + " submission archives");
        for (SubmissionResult result : results) {
            if (result.getStatus() == SubmissionStatus.READY) {
                ready++;
            } else if (result.getStatus() == SubmissionStatus.REVIEW_REQUIRED) {
                reviewRequired++;
            } else {
                failed++;
            }
            String identityLabel = result.getIdentity().getLabel();
            if (result.getIdentity().getStudentRecord() != null) {
                identityLabel = result.getIdentity().getStudentRecord().getUsername();
            }
            System.out.println(result.getArchivePath().getFileName() + " | "
                    + identityLabel + " | " + result.getStatus()
                    + " | issues=" + result.getAnomalies().size()
                    + " | corrections=" + result.getCorrections().size());
            if (result.getWorkingRoot() != null) {
                System.out.println("  Working copy: " + result.getWorkingRoot());
            }
        }
        System.out.println("Ready: " + ready + "; needs review: " + reviewRequired
                + "; failed: " + failed);
        System.out.println("READY means ready for the next stage, not a passing grade");
    }

    private static void printSimilarity(GradingRun run, PlagiarismConfig plagiarism) {
        if (!plagiarism.enabled()) {
            System.out.println("Similarity check: disabled in configuration");
            return;
        }
        int pairs = run.batch().getSimilarities().size();
        System.out.println("Similarity review: " + pairs + " question pair(s) at or above "
                + String.format(Locale.ROOT, "%.2f", plagiarism.threshold())
                + " similarity, starter code excluded: "
                + run.directory().resolve(GradingPipeline.SIMILARITY_REPORT));
        if (pairs > 0) {
            System.out.println("Similarity needs instructor judgement and is not proof of copying");
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar <application.jar> <configuration.properties>");
        System.out.println("Prepares, compiles and grades submissions using timed instructor cases");
    }
}
