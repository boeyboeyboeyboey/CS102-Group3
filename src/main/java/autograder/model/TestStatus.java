package autograder.model;

/** How a test case ended. */
public enum TestStatus {
    PASSED,
    FAILED,
    TIMED_OUT,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    NOT_RUN
}
