package autograder.autotester.execution;

import java.util.List;

public record RandomizedResult(boolean passed, int passedCount, int totalCount,
                               long seed, List<RandomizedFailure> failures, String errorReason) {
    public RandomizedResult {
        failures = List.copyOf(failures);
        errorReason = errorReason == null ? "" : errorReason;
    }
}
