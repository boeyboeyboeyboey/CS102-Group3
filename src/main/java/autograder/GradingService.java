package autograder;

import autograder.analysis.boilerplate.StarterCodeDetector;
import autograder.api.PlagiarismDetector;
import autograder.api.ProgressListener;
import autograder.config.AppConfig;
import autograder.config.RunSettings;
import autograder.grading.GradingPipeline;
import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;
import autograder.plagiarism.PlagiarismDetectorFactory;
import autograder.report.feedback.MarkdownFeedbackGenerator;
import autograder.report.html.OfflineHtmlReportGenerator;
import autograder.submission.SubmissionProcessor;

import java.nio.channels.ClosedByInterruptException;
import java.util.List;

/**
 * Runs preparation, grading and report generation for one configuration.
 * Contains no user interface code and never exits the JVM.
 */
public final class GradingService {
    private static final String GRADING_STAGE = "grading";
    private static final String REPORTING_STAGE = "reporting";

    public RunOutcome run(RunSettings settings, RunListener suppliedListener) {
        RunListener listener = new GuardedListener(suppliedListener);
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.ENVIRONMENT, new InterruptedException("grading stopped"));
        }
        GradingPipeline pipeline;
        try {
            pipeline = new GradingPipeline(settings.app(), settings.execution(), detector(settings),
                    gradingAdapter(listener));
            pipeline.validateEnvironment();
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.ENVIRONMENT, exception, List.of(), null);
        }
        return prepareAndGrade(settings, listener, pipeline);
    }

    private RunOutcome prepareAndGrade(RunSettings settings, RunListener listener, GradingPipeline pipeline) {
        AppConfig app = settings.app();
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.PREPARATION, new InterruptedException("grading stopped"));
        }
        List<SubmissionResult> results;
        try {
            listener.preparing(app.getAssessment().getName());
            results = new SubmissionProcessor(app, listener).processBatch();
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.PREPARATION, exception, List.of(), null);
        }
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.PREPARATION, new InterruptedException("grading stopped"));
        }
        if (results.isEmpty()) {
            return new RunOutcome(RunOutcome.Status.NO_SUBMISSIONS, null, null, results, null, 1);
        }
        listener.prepared(results, app);
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.GRADING, new InterruptedException("grading stopped"));
        }
        return gradeAndReport(settings, listener, pipeline, results);
    }

    private RunOutcome gradeAndReport(RunSettings settings, RunListener listener, GradingPipeline pipeline,
                                      List<SubmissionResult> results) {
        GradingRun run;
        try {
            run = pipeline.grade(results);
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.GRADING, exception, results, null);
        }
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.GRADING,
                    new InterruptedException("grading stopped"), results, run);
        }
        listener.graded(run, settings);
        listener.reportingProgress("Generating optional reports", 1, 2);
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.REPORTS,
                    new InterruptedException("grading stopped"), results, run);
        }
        int reportExit;
        try {
            reportExit = reports(settings, run, listener);
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.REPORTS, exception, results, run);
        }
        listener.reportingProgress(reportExit == 0 ? "Reports ready"
                : "Reports completed with errors", 2, 2);
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.REPORTS,
                    new InterruptedException("grading stopped"), results, run);
        }
        if (reportExit == 0) {
            return new RunOutcome(RunOutcome.Status.COMPLETED, null, null, results, run, 0);
        }
        return new RunOutcome(RunOutcome.Status.REPORT_FAILURES, null, null, results, run, reportExit);
    }

    private static int reports(RunSettings settings, GradingRun run, RunListener listener) {
        AppConfig app = settings.app();
        return App.generateReports(run, settings.boilerplate(),
                new StarterCodeDetector(app.getAssessment(), settings.boilerplate()),
                settings.sanity(), settings.html(), new OfflineHtmlReportGenerator(settings.html().file()),
                settings.feedback(), new MarkdownFeedbackGenerator(
                app.getAssessment(), settings.execution().caseTimeoutMillis(),
                settings.feedback().maxDiagnosticLines(), app.getRosterPrefix(),
                app.getRosterLastNamePlaceholder()), message -> {
                    System.err.println(message);
                    listener.reportFailed(message);
                });
    }

    private static PlagiarismDetector detector(RunSettings settings) {
        return PlagiarismDetectorFactory.create(settings.plagiarism());
    }

    private static ProgressListener gradingAdapter(RunListener listener) {
        return (stage, item, completed, total) -> {
            if (GRADING_STAGE.equals(stage)) {
                listener.gradingProgress(item, completed, total);
            } else if (REPORTING_STAGE.equals(stage)) {
                listener.reportingProgress(item, completed, total);
            }
        };
    }

    private static RunOutcome failed(RunOutcome.Stage stage, Exception exception,
                                     List<SubmissionResult> preparation, GradingRun run) {
        if (exception instanceof InterruptedException || stoppedByInterrupt(exception)) {
            return interrupted(stage, exception, preparation, run);
        }
        return new RunOutcome(RunOutcome.Status.FAILED, stage, exception, preparation, run, 1);
    }

    private static boolean stoppedByInterrupt(Throwable exception) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ClosedByInterruptException) {
                return true;
            }
        }
        return false;
    }

    private static RunOutcome interrupted(RunOutcome.Stage stage, Exception exception) {
        return interrupted(stage, exception, List.of(), null);
    }

    private static RunOutcome interrupted(RunOutcome.Stage stage, Exception exception,
                                          List<SubmissionResult> preparation, GradingRun run) {
        Thread.currentThread().interrupt();
        return new RunOutcome(RunOutcome.Status.INTERRUPTED, stage, exception, preparation, run, 1);
    }

    /** Keeps a failing progress display from discarding the run it is watching. */
    private static final class GuardedListener implements RunListener {
        private final RunListener delegate;

        GuardedListener(RunListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onProgress(String stage, String item, int completed, int total) {
            guard(() -> delegate.onProgress(stage, item, completed, total));
        }

        @Override
        public void preparing(String assessmentName) {
            guard(() -> delegate.preparing(assessmentName));
        }

        @Override
        public void prepared(List<SubmissionResult> results, AppConfig app) {
            guard(() -> delegate.prepared(results, app));
        }

        @Override
        public void gradingProgress(String item, int completed, int total) {
            guard(() -> delegate.gradingProgress(item, completed, total));
        }

        @Override
        public void reportingProgress(String item, int completed, int total) {
            guard(() -> delegate.reportingProgress(item, completed, total));
        }

        @Override
        public void reportFailed(String message) {
            guard(() -> delegate.reportFailed(message));
        }

        @Override
        public void graded(GradingRun run, RunSettings settings) {
            guard(() -> delegate.graded(run, settings));
        }

        private static void guard(Runnable callback) {
            try {
                callback.run();
            } catch (RuntimeException exception) {
                String message = exception.getMessage();
                System.err.println("Progress display failed: "
                        + (message == null || message.isBlank() ? exception.getClass().getSimpleName() : message));
            }
        }
    }
}
