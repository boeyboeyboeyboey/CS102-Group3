package autograder.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class SimilarityFinding {
    private final Path firstArchive;
    private final Path secondArchive;
    private final double similarity;
    private final List<String> evidence;
    private final String question;
    private final String algorithm;

    public SimilarityFinding(Path firstArchive, Path secondArchive, double similarity,
                              List<String> evidence) {
        this(firstArchive, secondArchive, similarity, evidence, "", "");
    }

    public SimilarityFinding(Path firstArchive, Path secondArchive, double similarity,
                              List<String> evidence, String question, String algorithm) {
        if (!Double.isFinite(similarity) || similarity < 0 || similarity > 1) {
            throw new IllegalArgumentException("similarity must be a fraction between zero and one");
        }
        this.firstArchive = Objects.requireNonNull(firstArchive);
        this.secondArchive = Objects.requireNonNull(secondArchive);
        this.similarity = similarity;
        this.evidence = List.copyOf(evidence);
        this.question = Objects.requireNonNull(question);
        this.algorithm = Objects.requireNonNull(algorithm);
    }

    public Path getFirstArchive() {
        return firstArchive;
    }
    public Path getSecondArchive() {
        return secondArchive;
    }
    public double getSimilarity() {
        return similarity;
    }
    public List<String> getEvidence() {
        return evidence;
    }
    public String getQuestion() {
        return question;
    }
    public String getAlgorithm() {
        return algorithm;
    }
}
