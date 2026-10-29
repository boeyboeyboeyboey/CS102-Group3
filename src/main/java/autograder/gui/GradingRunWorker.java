package autograder.gui;

import autograder.GradingService;
import autograder.RunListener;
import autograder.RunOutcome;
import autograder.config.AppConfig;
import autograder.config.RunSettings;
import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs one grading run in the background. Stopping interrupts the running thread
 * instead of cancelling the worker, so done() still fires with the real outcome.
 */
final class GradingRunWorker extends SwingWorker<RunOutcome, GradingRunWorker.Update> {
    /** One progress step; total is zero when the length is unknown. */
    record Update(String text, int completed, int total) {
    }

    private final RunSettings settings;
    private final Consumer<Update> onProgress;
    private final Consumer<RunOutcome> onFinished;
    private volatile Thread runner;
    private volatile boolean stopRequested;
    private final List<String> reportProblems = Collections.synchronizedList(new ArrayList<>());

    GradingRunWorker(RunSettings settings, Consumer<Update> onProgress,
                     Consumer<RunOutcome> onFinished) {
        this.settings = settings;
        this.onProgress = onProgress;
        this.onFinished = onFinished;
    }

    /** Report failure messages collected so far; safe from any thread. */
    List<String> reportProblems() {
        synchronized (reportProblems) {
            return List.copyOf(reportProblems);
        }
    }

    /** Asks the run to stop; safe from the event thread. */
    void stop() {
        stopRequested = true;
        Thread thread = runner;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    protected RunOutcome doInBackground() {
        runner = Thread.currentThread();
        if (stopRequested) {
            runner.interrupt();
        }
        try {
            return new GradingService().run(settings, new Relay());
        } catch (RuntimeException exception) {
            return new RunOutcome(RunOutcome.Status.FAILED, RunOutcome.Stage.GRADING,
                    exception, List.of(), null, 1);
        } finally {
            runner = null;
        }
    }

    @Override
    protected void process(List<Update> updates) {
        onProgress.accept(updates.get(updates.size() - 1));
    }

    @Override
    protected void done() {
        try {
            onFinished.accept(get());
        } catch (InterruptedException | ExecutionException | RuntimeException exception) {
            onFinished.accept(new RunOutcome(RunOutcome.Status.FAILED, RunOutcome.Stage.GRADING,
                    exception instanceof ExecutionException && exception.getCause() instanceof Exception cause
                            ? cause : exception, List.of(), null, 1));
        }
    }

    private final class Relay implements RunListener {
        @Override
        public void preparing(String assessmentName) {
            publish(new Update("Preparing " + assessmentName, 0, 0));
        }

        @Override
        public void onProgress(String stage, String item, int completed, int total) {
            publish(new Update(OutcomeText.intake(item, completed, total), completed, total));
        }

        @Override
        public void prepared(List<SubmissionResult> results, AppConfig app) {
            // grading progress follows
        }

        @Override
        public void gradingProgress(String item, int completed, int total) {
            publish(new Update(OutcomeText.grading(item, completed, total), completed, total));
        }

        @Override
        public void reportFailed(String message) {
            reportProblems.add(message);
        }

        @Override
        public void graded(GradingRun run, RunSettings runSettings) {
            publish(new Update(OutcomeText.WRITING_REPORTS, 0, 0));
        }
    }
}
