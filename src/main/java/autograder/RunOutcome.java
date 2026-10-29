package autograder;

import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;

import java.util.List;

/**
 * Result of one grading run, including where and why it stopped when it did
 * not complete.
 */
public record RunOutcome(Status status, Stage failedStage, Exception failure,
                         List<SubmissionResult> preparation, GradingRun run,
                         int exitCode) {
    public enum Status { COMPLETED, REPORT_FAILURES, NO_SUBMISSIONS, FAILED, INTERRUPTED }

    public enum Stage { ENVIRONMENT, PREPARATION, GRADING, REPORTS }

    public RunOutcome {
        preparation = List.copyOf(preparation);
    }

    public boolean graded() {
        return run != null;
    }
}
