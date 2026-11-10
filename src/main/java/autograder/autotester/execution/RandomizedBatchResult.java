package autograder.autotester.execution;

import java.nio.file.Path;
import java.util.List;

/** Results of grading every ZIP in a folder, with the output folder and summary file. */
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
