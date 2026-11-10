package autograder.model;

import java.util.Objects;

/** The outcome of running one test case. */
public final class TestCaseResult {
    private final TestCaseDefinition testCase;
    private final TestStatus status;
    private final long durationMillis;
    private final String standardOutput;
    private final String standardError;

    public TestCaseResult(TestCaseDefinition testCase, TestStatus status, long durationMillis,
                          String standardOutput, String standardError) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("test duration cannot be negative");
        }
        this.testCase = Objects.requireNonNull(testCase);
        this.status = Objects.requireNonNull(status);
        this.durationMillis = durationMillis;
        this.standardOutput = Objects.requireNonNull(standardOutput);
        this.standardError = Objects.requireNonNull(standardError);
    }

    public TestCaseDefinition getTestCase() {
        return testCase;
    }
    public TestStatus getStatus() {
        return status;
    }
    public long getDurationMillis() {
        return durationMillis;
    }
    public String getStandardOutput() {
        return standardOutput;
    }
    public String getStandardError() {
        return standardError;
    }

    public double getScore() {
        if (status == TestStatus.PASSED) {
            return testCase.getMaximumScore();
        }
        return 0;
    }
}
