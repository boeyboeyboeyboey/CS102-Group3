package autograder.service.api;

/** Receives human-readable grading progress without coupling services to a UI. */
public interface ProgressListener {
    void onProgress(String message);
}
