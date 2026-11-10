package autograder.analysis.sanity;

/** The advisory checks run over a finished grading run. */
public enum SanityCheck {
    /** Every graded submission fails to compile a question, so support files or the file name are suspect. */
    ALL_COMPILE_ERRORS,
    /** Nearly every graded submission scores zero on a question, so the tester or expected output is suspect. */
    QUESTION_MOSTLY_ZERO,
    /** Many submissions fail one case with the same printed output, so the expected output is suspect. */
    SHARED_FAILURE,
    /** Many submissions time out on one case, so the case timeout or tester input is suspect. */
    CLUSTERED_TIMEOUTS,
    /** Dynamic cases pass far less often than fixed cases, so the dynamic test data is suspect. */
    FIXED_DYNAMIC_GAP
}
