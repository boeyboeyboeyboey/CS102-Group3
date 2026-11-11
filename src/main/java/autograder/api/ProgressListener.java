package autograder.api;

/** Receives stage-by-stage progress and warnings while submissions are being prepared and graded. */
public interface ProgressListener {
    /** Called as each item of a stage completes; completed counts items done out of total. */
    void onProgress(String stage, String item, int completed, int total);

    /** Called when grading of a submission begins; index counts from 1 in input order. */
    default void onSubmissionStarted(String item, int index, int total) {
    }

    /** Called with a problem that does not stop the run, such as an optional check that was skipped. */
    default void onWarning(String message) {
    }
}
