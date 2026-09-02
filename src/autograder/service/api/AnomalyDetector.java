package autograder.service.api;

import autograder.model.Anomaly;
import autograder.model.StudentRecord;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Extension point for submission-level diagnostic checks. */
public interface AnomalyDetector {
    List<Anomaly> detect(Path extractedRoot, StudentRecord student) throws IOException;
}
