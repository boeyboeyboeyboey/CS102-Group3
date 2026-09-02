package autograder.service.api;

import autograder.model.Anomaly;
import autograder.model.StudentRecord;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Resolves a submission to one rostered student using ordered evidence. */
public interface IdentityResolver {
    Optional<StudentRecord> resolve(Path archive, Path extractedRoot,
            List<StudentRecord> roster, List<Anomaly> anomalies) throws IOException;
}
