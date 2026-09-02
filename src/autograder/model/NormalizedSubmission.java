package autograder.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A submission whose identity and logical question locations are resolved. */
public final class NormalizedSubmission {
    private final StudentRecord student;
    private final Path archive;
    private final Path extractedRoot;
    private final Map<String, QuestionSource> questionSources;
    private final List<Anomaly> anomalies;

    public NormalizedSubmission(StudentRecord student, Path archive, Path extractedRoot,
            Map<String, QuestionSource> questionSources, List<Anomaly> anomalies) {
        this.student = student;
        this.archive = archive;
        this.extractedRoot = extractedRoot;
        this.questionSources = Collections.unmodifiableMap(new LinkedHashMap<>(questionSources));
        this.anomalies = new ArrayList<>(anomalies);
    }

    public StudentRecord getStudent() {
        return student;
    }

    public Path getArchive() {
        return archive;
    }

    public Path getExtractedRoot() {
        return extractedRoot;
    }

    public Map<String, QuestionSource> getQuestionSources() {
        return questionSources;
    }

    public List<Anomaly> getAnomalies() {
        return anomalies;
    }

    public void addAnomaly(Anomaly anomaly) {
        anomalies.add(anomaly);
    }
}
