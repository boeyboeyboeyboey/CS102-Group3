package autograder;

import autograder.api.ProgressListener;
import autograder.config.AppConfig;
import autograder.config.RunSettings;
import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;

import java.util.List;

/**
 * Receives the milestones of one grading run. Intake progress arrives through
 * the inherited onProgress callback.
 */
public interface RunListener extends ProgressListener {
    void preparing(String assessmentName);

    void prepared(List<SubmissionResult> results, AppConfig app);

    default void gradingProgress(String item, int completed, int total) {
    }

    void graded(GradingRun run, RunSettings settings);

    default void reportFailed(String message) {
    }
}
