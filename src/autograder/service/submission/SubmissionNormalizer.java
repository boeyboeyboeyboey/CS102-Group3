package autograder.service.submission;

import autograder.model.Anomaly;
import autograder.model.NormalizedSubmission;
import autograder.model.QuestionSource;
import autograder.model.QuestionSpec;
import autograder.model.Severity;
import autograder.model.StudentRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Maps irregular submission layouts onto the configured logical question layout. */
public final class SubmissionNormalizer {
    private final int searchDepth;

    public SubmissionNormalizer(int searchDepth) {
        this.searchDepth = searchDepth;
    }

    public NormalizedSubmission normalize(StudentRecord student, Path archive,
            Path extractedRoot, List<QuestionSpec> questions, List<Anomaly> anomalies)
            throws IOException {
        Map<String, QuestionSource> sources = new LinkedHashMap<>();
        for (QuestionSpec question : questions) {
            Selection selection = select(extractedRoot, question.getSourceFileName(),
                    question.getFolderName());
            boolean fallback = false;
            if (selection == null && !question.getFallbackSourceFileName().isEmpty()) {
                selection = select(extractedRoot, question.getFallbackSourceFileName(),
                        question.getFolderName());
                fallback = selection != null;
            }
            if (selection == null) {
                anomalies.add(new Anomaly("MISSING_SOURCE", Severity.ERROR,
                        question.getId() + " source file is missing"));
                continue;
            }
            if (selection.candidateCount > 1) {
                anomalies.add(new Anomaly("DUPLICATE_SOURCE", Severity.WARNING,
                        "Found " + selection.candidateCount + " candidates for "
                                + question.getId() + "; selected " + selection.path));
            }
            if (!selection.path.getFileName().toString()
                    .equals(question.getSourceFileName())) {
                String message = fallback
                        ? question.getId() + " will use fallback " + selection.path.getFileName()
                        : question.getId() + " filename case will be normalized from "
                                + selection.path.getFileName();
                anomalies.add(new Anomaly(fallback ? "Q2_FALLBACK" : "FILENAME_MISMATCH",
                        Severity.WARNING, message));
            }
            sources.put(question.getId(), new QuestionSource(question, selection.path, fallback));
        }
        return new NormalizedSubmission(student, archive, extractedRoot, sources, anomalies);
    }

    private Selection select(Path root, String fileName, String expectedFolder)
            throws IOException {
        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root, searchDepth)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                    .forEach(candidates::add);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator
                .comparing((Path path) -> !path.getParent().getFileName().toString()
                        .equalsIgnoreCase(expectedFolder))
                .thenComparingInt(Path::getNameCount)
                .thenComparing(Path::toString));
        return new Selection(candidates.get(0), candidates.size());
    }

    private static final class Selection {
        private final Path path;
        private final int candidateCount;

        private Selection(Path path, int candidateCount) {
            this.path = path;
            this.candidateCount = candidateCount;
        }
    }
}
