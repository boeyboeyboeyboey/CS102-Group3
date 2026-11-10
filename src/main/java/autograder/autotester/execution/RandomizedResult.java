package autograder.autotester.execution;

import java.util.List;

/** Outcome of grading one submission: counts, seed, shown failures and any error reason. */
public record RandomizedResult(boolean passed, int passedCount, int totalCount,
                               long seed, List<RandomizedFailure> failures, String errorReason) {
    public RandomizedResult {
        failures = List.copyOf(failures);
        errorReason = errorReason == null ? "" : errorReason;
    }
}
