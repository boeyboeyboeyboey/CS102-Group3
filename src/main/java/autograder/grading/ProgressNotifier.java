package autograder.grading;

import autograder.api.ProgressListener;
import autograder.api.ProgressStages;

import java.util.concurrent.atomic.AtomicInteger;

/** Delivers progress, start and warning events to a listener so that a failing listener cannot stop grading. */
final class ProgressNotifier {
    private final ProgressListener listener;
    private boolean progressEnabled = true;

    ProgressNotifier(ProgressListener listener) {
        this.listener = listener;
    }

    // the count is read under the listener lock so parallel events never go backwards
    synchronized void gradingCount(String item, AtomicInteger finished, int step, int total) {
        progress(ProgressStages.GRADING, item, finished.addAndGet(step), total);
    }

    // a failing listener is disabled for progress updates for the rest of the run
    synchronized void progress(String stage, String item, int completed, int total) {
        if (!progressEnabled) {
            return;
        }
        try {
            listener.onProgress(stage, item, completed, total);
        } catch (RuntimeException exception) {
            warn("progress update failed: " + exception.getMessage());
            progressEnabled = false;
        }
    }

    // start notices are still delivered after a progress failure, as they carry the console lines
    synchronized void submissionStarted(String item, int index, int total) {
        try {
            listener.onSubmissionStarted(item, index, total);
        } catch (RuntimeException exception) {
            warn("progress update failed: " + exception.getMessage());
        }
    }

    void warn(String message) {
        try {
            listener.onWarning(message);
        } catch (RuntimeException exception) {
            // the listener is the thing that failed, so there is nowhere else to report this
        }
    }
}
