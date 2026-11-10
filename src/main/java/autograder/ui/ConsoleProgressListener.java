package autograder.ui;

import autograder.api.ProgressListener;

/** Prints each progress update as a plain line on standard output. */
public final class ConsoleProgressListener implements ProgressListener {
    @Override
    public void onProgress(String stage, String item, int completed, int total) {
        System.out.println("[" + completed + "/" + total + "] " + stage + ": " + item);
    }
}
