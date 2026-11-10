package autograder.api;

/** Receives stage-by-stage progress while submissions are being prepared. */
public interface ProgressListener {
    /** Called as each item of a stage completes; completed counts items done out of total. */
    void onProgress(String stage, String item, int completed, int total);
}
