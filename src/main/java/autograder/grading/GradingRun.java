package autograder.grading;

import autograder.model.BatchResult;
import autograder.model.SubmissionResult;
import java.nio.file.Path;
import java.util.Map;

/** A finished grading run: its results, run directory and the submissions withheld from export. */
public record GradingRun(BatchResult batch, Path directory,
                         Map<SubmissionResult, String> withheld) {
    public GradingRun {
        withheld = Map.copyOf(withheld);
    }
}
