package autograder.ui;

import autograder.service.api.ProgressListener;

/** Console implementation of grading progress notifications. */
public final class ConsoleProgressListener implements ProgressListener {
    @Override
    public void onProgress(String message) {
        System.out.println(message);
    }
}
