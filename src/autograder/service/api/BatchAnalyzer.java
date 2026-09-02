package autograder.service.api;

import autograder.model.Anomaly;
import autograder.model.NormalizedSubmission;
import java.util.List;

/** Extension point for analysis that compares multiple submissions. */
public interface BatchAnalyzer {
    List<Anomaly> analyze(List<NormalizedSubmission> submissions);
}
