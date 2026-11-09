package autograder.autotester.execution;

import java.nio.file.Path;
import java.util.List;

public record RandomizedBatchResult(Path outputDirectory, Path summaryFile,
                                    List<RandomizedSubmissionResult> submissions) {
    public RandomizedBatchResult {
        submissions = List.copyOf(submissions);
    }

    public long passedCount() {
        return submissions.stream().filter(submission -> submission.result().passed()).count();
    }

    public long failedCount() {
        return submissions.size() - passedCount();
    }
}
