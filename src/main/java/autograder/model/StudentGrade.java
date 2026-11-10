package autograder.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The graded outcome of one submission: its case results and scores. */
public final class StudentGrade {
    private final SubmissionResult submission;
    private final List<TestCaseResult> testResults;

    public StudentGrade(SubmissionResult submission, List<TestCaseResult> testResults) {
        this.submission = Objects.requireNonNull(submission);
        this.testResults = List.copyOf(testResults);
        Set<String> testIds = new HashSet<>();
        for (TestCaseResult result : this.testResults) {
            if (!testIds.add(result.getTestCase().getId())) {
                throw new IllegalArgumentException("duplicate test result: "
                        + result.getTestCase().getId());
            }
        }
    }

    public SubmissionResult getSubmission() {
        return submission;
    }
    public List<TestCaseResult> getTestResults() {
        return testResults;
    }

    public double getTotalScore() {
        BigDecimal total = BigDecimal.ZERO;
        for (TestCaseResult result : testResults) {
            total = total.add(BigDecimal.valueOf(result.getScore()));
        }
        return total.doubleValue();
    }

    public double getQuestionScore(String questionId) {
        BigDecimal total = BigDecimal.ZERO;
        for (TestCaseResult result : testResults) {
            if (result.getTestCase().getQuestionId().equals(questionId)) {
                total = total.add(BigDecimal.valueOf(result.getScore()));
            }
        }
        return total.doubleValue();
    }
}
