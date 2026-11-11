package autograder.ui;

import autograder.api.ProgressListener;

/** Prints the start of each submission's grading on standard output and warnings on standard error. */
public final class ConsoleGradingMessages implements ProgressListener {
    @Override
    public void onProgress(String stage, String item, int completed, int total) {
    }

    @Override
    public void onSubmissionStarted(String item, int index, int total) {
        System.out.println("Grading " + index + "/" + total + ": " + item);
    }

    @Override
    public void onWarning(String message) {
        System.err.println(message);
    }
}
