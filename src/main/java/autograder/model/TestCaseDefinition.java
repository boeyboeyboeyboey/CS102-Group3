package autograder.model;

import java.util.List;

/** One test case to run: the runner class, its arguments and its points. */
public final class TestCaseDefinition {
    private final String id;
    private final String questionId;
    private final String runnerClass;
    private final List<String> arguments;
    private final double maximumScore;

    public TestCaseDefinition(String id, String questionId, String runnerClass,
                              List<String> arguments, double maximumScore) {
        if (id == null || id.isBlank() || questionId == null || questionId.isBlank()
                || runnerClass == null || runnerClass.isBlank()) {
            throw new IllegalArgumentException("test identifiers and runner class are required");
        }
        if (!Double.isFinite(maximumScore) || maximumScore <= 0) {
            throw new IllegalArgumentException("test maximum score must be finite and positive");
        }
        this.id = id;
        this.questionId = questionId;
        this.runnerClass = runnerClass;
        this.arguments = List.copyOf(arguments);
        this.maximumScore = maximumScore;
    }

    public String getId() {
        return id;
    }
    public String getQuestionId() {
        return questionId;
    }
    public String getRunnerClass() {
        return runnerClass;
    }
    public List<String> getArguments() {
        return arguments;
    }
    public double getMaximumScore() {
        return maximumScore;
    }
}
