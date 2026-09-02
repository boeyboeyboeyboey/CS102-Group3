package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Aggregated result for one question. */
public final class QuestionResult {
    private final String questionId;
    private final double score;
    private final double maximumScore;
    private final List<TestCaseResult> testCases;

    public QuestionResult(String questionId, double score, double maximumScore,
            List<TestCaseResult> testCases) {
        this.questionId = questionId;
        this.score = score;
        this.maximumScore = maximumScore;
        this.testCases = Collections.unmodifiableList(new ArrayList<>(testCases));
    }

    public String getQuestionId() {
        return questionId;
    }

    public double getScore() {
        return score;
    }

    public double getMaximumScore() {
        return maximumScore;
    }

    public List<TestCaseResult> getTestCases() {
        return testCases;
    }
}
