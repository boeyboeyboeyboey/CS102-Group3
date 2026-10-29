package autograder.autotester;

import java.util.List;

public record RandomizedResult(boolean passed, int passedCount, int totalCount,
                               long seed, List<Failure> failures, String errorReason) {
    public RandomizedResult {
        failures = List.copyOf(failures);
        errorReason = errorReason == null ? "" : errorReason;
    }

    public record Failure(String testCaseId, int runNumber, String input,
                          String expected, String actual) { }
}
