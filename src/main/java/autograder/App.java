package autograder;

import autograder.autotester.Question;
import autograder.autotester.QuestionRegistry;
import autograder.autotester.RandomizedBatchGrader;
import autograder.autotester.RandomizedConfig;
import autograder.autotester.RandomizedGrader;
import autograder.autotester.RandomizedResult;
import autograder.autotester.ResourceGenerator;
import autograder.api.BoilerplateDetector;
import autograder.api.FeedbackGenerator;
import autograder.api.HtmlReportGenerator;
import autograder.config.AppConfig;
import autograder.config.BoilerplateConfig;
import autograder.config.FeedbackConfig;
import autograder.config.HtmlReportConfig;
import autograder.config.RunSettings;
import autograder.config.RuntimePathOverrides;
import autograder.config.SanityConfig;
import autograder.config.WorkerLimits;
import autograder.grading.GradingRun;
import autograder.model.GradingReportContext;
import autograder.model.StudentGrade;
import autograder.ui.DashboardSettings;
import autograder.ui.TerminalCancellation;
import autograder.ui.TerminalDashboard;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.OptionalInt;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

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
        if (args.length > 0 && args[0].startsWith("--randomized-")) {
            return runRandomized(args);
        }
        if (args.length > 0 && args[0].equals(WorkersOption.FLAG)) {
            return runWithWorkersFlag(args);
        }
        if (args.length != 1) {
            printUsage();
            return 1;
        }
        return runGrading(Path.of(args[0]), OptionalInt.empty());
    }

    private static int runWithWorkersFlag(String[] args) {
        if (args.length != 3) {
            printUsage();
            return 1;
        }
        int workers;
        try {
            workers = WorkersOption.parse(args[1], WorkerLimits.system());
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            printUsage();
            return 1;
        }
        return runGrading(Path.of(args[2]), OptionalInt.of(workers));
    }

    private static int runGrading(Path configFile, OptionalInt workersOverride) {
        TerminalCancellation cancellation = null;
        try {
            RunSettings settings = RunSettings.load(configFile, RuntimePathOverrides.none());
            if (workersOverride.isPresent()) {
                settings = settings.withWorkers(workersOverride.getAsInt());
            }
            settings = capWorkers(settings);
            DashboardSettings dashboard = DashboardSettings.load(configFile);
            Console console = System.console();
            TerminalDashboard terminalDashboard = new TerminalDashboard(System.out,
                    dashboard.mode(), console != null, System.getenv());
            cancellation = TerminalCancellation.start(console, Thread.currentThread(), System.out);
            DashboardRunListener listener = new DashboardRunListener(
                    new ConsoleRunListener(), terminalDashboard);
            RunOutcome outcome = new GradingService().run(settings, listener);
            return report(outcome);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException
                    || exception instanceof CancellationException
                    || Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                System.err.println("grading cancelled");
                return 130;
            }
            System.err.println("could not complete grading: " + exception.getMessage());
            return 1;
        } finally {
            if (cancellation != null) {
                cancellation.close();
            }
        }
    }

    private static RunSettings capWorkers(RunSettings settings) {
        WorkerLimits.Capped capped = WorkerLimits.system().cap(settings.execution().workers());
        if (capped.warning().isEmpty()) {
            return settings;
        }
        System.err.println(capped.warning().get());
        return settings.withWorkers(capped.workers());
    }

    static int report(RunOutcome outcome) {
        if (outcome.status() == RunOutcome.Status.INTERRUPTED) {
            System.err.println("grading cancelled");
            return 130;
        }
        if (outcome.status() == RunOutcome.Status.NO_SUBMISSIONS) {
            System.err.println("No submission archives matched the configured input directory and pattern");
        } else if (outcome.failure() != null) {
            System.err.println("could not complete grading: " + outcome.failure().getMessage());
        }
        return outcome.exitCode();
    }

    private static int runRandomized(String[] args) {
        try {
            return switch (args[0]) {
                case "--randomized-resources" -> generateRandomizedResources(args);
                case "--randomized-grade" -> gradeRandomizedSubmission(args);
                case "--randomized-grade-batch" -> gradeRandomizedBatch(args);
                default -> {
                    printUsage();
                    yield 1;
                }
            };
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("randomized autograder error: " + exception.getMessage());
            return 1;
        }
    }

    private static int generateRandomizedResources(String[] args) throws Exception {
        if (args.length != 3) {
            printUsage();
            return 1;
        }
        Question question = QuestionRegistry.find(args[1]);
        RandomizedConfig config = RandomizedConfig.load(Path.of(args[2]));
        new ResourceGenerator().generate(question, config.resourcesDirectory(),
                config.examplesPerCase(), new SecureRandom().nextLong());
        System.out.println("Student stub: " + config.resourcesDirectory().resolve(question.getClassName() + ".java"));
        System.out.println("Student tester: " + config.resourcesDirectory()
                .resolve(question.getClassName() + "Tester.java"));
        return 0;
    }

    private static int gradeRandomizedSubmission(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            printUsage();
            return 1;
        }
        Question question = QuestionRegistry.find(args[1]);
        RandomizedConfig config = RandomizedConfig.load(Path.of(args[3]));
        long seed = args.length == 5 ? Long.parseLong(args[4]) : new SecureRandom().nextLong();
        RandomizedGrader grader = new RandomizedGrader();
        RandomizedResult result = grader.grade(question, Path.of(args[2]), config, seed);
        Path report = grader.writeResult(question, Path.of(args[2]), config, result);
        System.out.println((result.passed() ? "PASS" : "FAIL") + " | " + result.passedCount()
                + "/" + result.totalCount() + " tests | seed=" + result.seed());
        System.out.println("Result: " + report);
        return 0;
    }

    private static int gradeRandomizedBatch(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            printUsage();
            return 1;
        }
        Question question = QuestionRegistry.find(args[1]);
        Path zipDirectory = Path.of(args[2]);
        Path configFile = Path.of(args[3]);
        AppConfig appConfig = AppConfig.load(configFile);
        RandomizedConfig config = RandomizedConfig.load(configFile);
        long seed = args.length == 5 ? Long.parseLong(args[4]) : new SecureRandom().nextLong();
        RandomizedBatchGrader.BatchResult batch = new RandomizedBatchGrader().grade(question, zipDirectory,
                appConfig, config, seed);
        for (RandomizedBatchGrader.SubmissionResult submission : batch.submissions()) {
            RandomizedResult result = submission.result();
            System.out.println(submission.archive() + " | " + (result.passed() ? "PASS" : "FAIL")
                    + " | " + result.passedCount() + "/" + result.totalCount()
                    + " tests | seed=" + result.seed()
                    + (result.errorReason().isBlank() ? "" : " | " + result.errorReason()));
        }
        System.out.println("Randomized batch: " + batch.passedCount() + " passed; "
                + batch.failedCount() + " failed of " + batch.submissions().size());
        System.out.println("Summary: " + batch.summaryFile());
        System.out.println("Per-submission reports: " + batch.outputDirectory());
        return 0;
    }

    static int generateReports(GradingRun run, BoilerplateConfig boilerplate, BoilerplateDetector detector,
                               HtmlReportConfig html, HtmlReportGenerator htmlGenerator,
                               FeedbackConfig feedback, FeedbackGenerator feedbackGenerator) {
        return generateReports(run, boilerplate, detector, SanityConfig.disabled(), html, htmlGenerator,
                feedback, feedbackGenerator);
    }

    static int generateReports(GradingRun run, BoilerplateConfig boilerplate, BoilerplateDetector detector,
                               SanityConfig sanity, HtmlReportConfig html, HtmlReportGenerator htmlGenerator,
                               FeedbackConfig feedback, FeedbackGenerator feedbackGenerator) {
        return generateReports(run, boilerplate, detector, sanity, html, htmlGenerator, feedback,
                feedbackGenerator, System.err::println);
    }

    static int generateReports(GradingRun run, BoilerplateConfig boilerplate, BoilerplateDetector detector,
                               SanityConfig sanity, HtmlReportConfig html, HtmlReportGenerator htmlGenerator,
                               FeedbackConfig feedback, FeedbackGenerator feedbackGenerator,
                               Consumer<String> problems) {
        StarterAnalysis.Result analysis = StarterAnalysis.analyze(run, boilerplate, detector, problems);
        int sanityExit = SanityAnalysis.analyze(analysis.run(), sanity, problems).exitCode();
        int htmlExit = generateHtmlReport(analysis.run(), html, htmlGenerator, problems);
        int feedbackExit = generateFeedback(analysis.run(), feedback, feedbackGenerator, problems);
        return Math.max(Math.max(analysis.exitCode(), sanityExit), Math.max(htmlExit, feedbackExit));
    }

    static int generateFeedback(GradingRun run, FeedbackConfig config, FeedbackGenerator generator) {
        return generateFeedback(run, config, generator, System.err::println);
    }

    static int generateFeedback(GradingRun run, FeedbackConfig config, FeedbackGenerator generator,
                                Consumer<String> problems) {
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
                problems.accept("could not write feedback for "
                        + grade.getSubmission().getArchivePath().getFileName() + ": " + exception.getMessage());
            }
        }
        System.out.println("Student feedback: " + written + " Markdown files in " + directory);
        if (failed > 0) {
            problems.accept(failed + " feedback files could not be written; grading and CSV exports "
                    + "are complete in " + run.directory());
            return 1;
        }
        return 0;
    }

    static int generateHtmlReport(GradingRun run, HtmlReportConfig config,
                                 HtmlReportGenerator generator) {
        return generateHtmlReport(run, config, generator, System.err::println);
    }

    static int generateHtmlReport(GradingRun run, HtmlReportConfig config,
                                 HtmlReportGenerator generator, Consumer<String> problems) {
        if (!config.enabled()) {
            return 0;
        }
        try {
            Path report = generator.generate(new GradingReportContext(run.batch(), run.withheld()),
                    run.directory());
            System.out.println("HTML dashboard: " + report);
            return 0;
        } catch (IOException exception) {
            problems.accept("could not write HTML dashboard " + run.directory().resolve(config.file())
                    + ": " + exception.getMessage());
            problems.accept("Grading and CSV exports are complete; reports remain in " + run.directory());
            return 1;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar <application.jar> [--workers <N|auto>] <configuration.properties>");
        System.out.println("Prepares, compiles and grades submissions using timed instructor cases");
        System.out.println("Usage: java -jar <application.jar> --randomized-resources <question-id> <configuration.properties>");
        System.out.println("Usage: java -jar <application.jar> --randomized-grade <question-id> <submission-folder> "
                + "<configuration.properties> [seed]");
        System.out.println("Usage: java -jar <application.jar> --randomized-grade-batch <question-id> "
                + "<zip-directory> <configuration.properties> [seed]");
        System.out.println("Registered randomized questions: " + String.join(", ", QuestionRegistry.questionIds()));
    }
}
