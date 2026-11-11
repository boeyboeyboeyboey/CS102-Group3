package autograder;

import autograder.autotester.question.QuestionRegistry;
import autograder.api.BoilerplateDetector;
import autograder.api.FeedbackGenerator;
import autograder.api.HtmlReportGenerator;
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
import autograder.util.ElapsedTime;
import autograder.util.ExceptionMessages;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Command-line entry point: parses the arguments, runs one grading session and maps the outcome to an exit code. */
public final class App {
    private static final int CANCELLED_EXIT_CODE = 130;
    private static final String CANCELLED_MESSAGE = "grading cancelled";

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
            return RandomizedCommands.runRandomized(args);
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
        if (args.length < 3) {
            System.err.println(WorkersOption.FLAG + (args.length == 1
                    ? " needs a value and a configuration file"
                    : " " + args[1] + " needs a configuration file after it"));
            printUsage();
            return 1;
        }
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
            long started = System.nanoTime();
            RunOutcome outcome;
            try {
                outcome = new GradingService().run(settings, listener);
            } finally {
                System.out.println("Run time: " + ElapsedTime.describe(
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
            }
            int exitCode = report(outcome);
            if (outcome.run() != null) {
                System.out.println("Run folder: " + outcome.run().directory());
            }
            return exitCode;
        } catch (Exception exception) {
            if (exception instanceof InterruptedException
                    || exception instanceof CancellationException
                    || Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                System.err.println(CANCELLED_MESSAGE);
                return CANCELLED_EXIT_CODE;
            }
            System.err.println("could not complete grading: " + ExceptionMessages.describe(exception));
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
            System.err.println(CANCELLED_MESSAGE);
            return CANCELLED_EXIT_CODE;
        }
        if (outcome.status() == RunOutcome.Status.NO_SUBMISSIONS) {
            System.err.println("No submission archives matched the configured input directory and pattern");
        } else if (outcome.failure() != null) {
            System.err.println("could not complete grading: " + ExceptionMessages.describe(outcome.failure()));
        }
        return outcome.exitCode();
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
                        + grade.getSubmission().getArchivePath().getFileName() + ": "
                        + ExceptionMessages.describe(exception));
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
                    + ": " + ExceptionMessages.describe(exception));
            problems.accept("Grading and CSV exports are complete; reports remain in " + run.directory());
            return 1;
        }
    }

    static void printUsage() {
        System.out.println("Usage: java -jar <application.jar> [--workers <N|auto>] <configuration.properties>");
        System.out.println("Prepares, compiles and grades submissions using timed instructor cases");
        System.out.println("Usage: java -jar <application.jar> --randomized-resources <question-id> "
                + "<configuration.properties>");
        System.out.println("Usage: java -jar <application.jar> --randomized-grade <question-id> <submission-folder> "
                + "<configuration.properties> [seed]");
        System.out.println("Usage: java -jar <application.jar> --randomized-grade-batch <question-id> "
                + "<zip-directory> <configuration.properties> [seed]");
        System.out.println("Registered randomized questions: " + String.join(", ", QuestionRegistry.questionIds()));
    }
}
