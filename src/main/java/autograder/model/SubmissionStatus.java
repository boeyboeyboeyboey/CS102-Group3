package autograder.model;

/** Whether a prepared submission can be graded. */
public enum SubmissionStatus {
    READY,
    REVIEW_REQUIRED,
    FAILED
}
