package autograder.model;

/** Score and status for one isolated tester case. */
public final class TestCaseResult {
    public enum Status {
        PASSED,
        FAILED,
        TIMED_OUT,
        EXECUTION_ERROR,
        INVALID_OUTPUT
    }

    private final int number;
    private final double score;
    private final Status status;
    private final String detail;

    public TestCaseResult(int number, double score, Status status, String detail) {
        this.number = number;
        this.score = score;
        this.status = status;
        this.detail = detail;
    }

    public int getNumber() {
        return number;
    }

    public double getScore() {
        return score;
    }

    public Status getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }
}
