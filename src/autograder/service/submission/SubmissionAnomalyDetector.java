package autograder.service.submission;

import autograder.model.Anomaly;
import autograder.model.Severity;
import autograder.model.StudentRecord;
import autograder.service.api.AnomalyDetector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Detects common packaging mistakes and ignored generated artifacts. */
public final class SubmissionAnomalyDetector implements AnomalyDetector {
    private final int searchDepth;

    public SubmissionAnomalyDetector(int searchDepth) {
        this.searchDepth = searchDepth;
    }

    @Override
    public List<Anomaly> detect(Path extractedRoot, StudentRecord student) throws IOException {
        List<Anomaly> anomalies = new ArrayList<>();
        List<Path> topLevelDirectories = new ArrayList<>();
        try (Stream<Path> paths = Files.list(extractedRoot)) {
            paths.filter(Files::isDirectory).forEach(topLevelDirectories::add);
        }
        boolean flat = topLevelDirectories.stream().anyMatch(this::isQuestionDirectory);
        if (flat) {
            anomalies.add(new Anomaly("FLAT_SUBMISSION", Severity.WARNING,
                    "Question folders were submitted without a student root folder; corrected logically"));
        } else if (topLevelDirectories.size() == 1) {
            String rootName = topLevelDirectories.get(0).getFileName().toString();
            if (!rootName.equalsIgnoreCase(student.getUsername())
                    && !rootName.equalsIgnoreCase(student.getOrganizationId())) {
                anomalies.add(new Anomaly("ROOT_FOLDER_MISMATCH", Severity.WARNING,
                        "Root folder '" + rootName + "' does not identify "
                                + student.getUsername() + "; corrected logically"));
            }
        }

        int classFiles = 0;
        int generatedArtifacts = 0;
        try (Stream<Path> paths = Files.walk(extractedRoot, searchDepth)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".class")) {
                    classFiles++;
                } else if (name.equals(".ds_store") || name.endsWith(".html")) {
                    generatedArtifacts++;
                }
            }
        }
        if (classFiles > 0) {
            anomalies.add(new Anomaly("SUBMITTED_CLASS_FILES", Severity.INFO,
                    "Ignored " + classFiles + " submitted class file(s)"));
        }
        if (generatedArtifacts > 0) {
            anomalies.add(new Anomaly("GENERATED_ARTIFACTS", Severity.INFO,
                    "Ignored " + generatedArtifacts + " generated or metadata file(s)"));
        }
        return anomalies;
    }

    private boolean isQuestionDirectory(Path path) {
        String name = path.getFileName().toString();
        return name.matches("(?i)Q\\d+");
    }
}
