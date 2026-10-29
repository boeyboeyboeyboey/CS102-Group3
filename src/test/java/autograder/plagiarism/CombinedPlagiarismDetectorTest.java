package autograder.plagiarism;

import autograder.api.PlagiarismDetector;
import autograder.config.PlagiarismConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.SimilarityFinding;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedPlagiarismDetectorTest {
    private static final AssessmentDefinition ASSESSMENT = new AssessmentDefinition("test", "Test",
            List.of(new QuestionDefinition("Q1a", "Q1", "Q1a.java", Path.of("Q1aTester.java"), 1, 1.0,
                    List.of(), List.of(), List.of())));

    @Test
    void keepsFindingsFromWorkingAlgorithmsWhenAnotherFails() throws Exception {
        Map<String, PlagiarismDetector> detectors = new LinkedHashMap<>();
        detectors.put("broken", (submissions, assessment) -> {
            throw new IOException("library unavailable");
        });
        detectors.put("working", (submissions, assessment) -> List.of(finding("Q1a", 0.9, "working")));

        List<SimilarityFinding> findings = new CombinedPlagiarismDetector(detectors).compare(List.of(), ASSESSMENT);

        assertEquals(1, findings.size());
        assertEquals("working", findings.get(0).getAlgorithm());
    }

    @Test
    void ordersFindingsByQuestionThenHighestSimilarity() throws Exception {
        Map<String, PlagiarismDetector> detectors = new LinkedHashMap<>();
        detectors.put("n-gram", (submissions, assessment) -> List.of(finding("Q2a", 0.95, "n-gram"),
                finding("Q1a", 0.70, "n-gram")));
        detectors.put("JPlag", (submissions, assessment) -> List.of(finding("Q1a", 0.85, "JPlag")));

        List<SimilarityFinding> findings = new CombinedPlagiarismDetector(detectors).compare(List.of(), ASSESSMENT);

        assertEquals(List.of("Q1a JPlag", "Q1a n-gram", "Q2a n-gram"), findings.stream()
                .map(finding -> finding.getQuestion() + " " + finding.getAlgorithm()).toList());
    }

    @Test
    void factoryReturnsNoFindingsWhenDisabledAndCombinesConfiguredAlgorithms() throws Exception {
        PlagiarismConfig disabled = new PlagiarismConfig(false, List.of(PlagiarismConfig.NGRAM), 5, 0.8, 20, 0.6, 9,
                Path.of("."));
        PlagiarismConfig both = new PlagiarismConfig(true, List.of(PlagiarismConfig.NGRAM, PlagiarismConfig.JPLAG),
                5, 0.8, 20, 0.6, 9, Path.of("."));

        assertTrue(PlagiarismDetectorFactory.create(disabled).compare(List.of(), ASSESSMENT).isEmpty());
        assertInstanceOf(CombinedPlagiarismDetector.class, PlagiarismDetectorFactory.create(both));
    }

    private static SimilarityFinding finding(String question, double similarity, String algorithm) {
        return new SimilarityFinding(Path.of("a.zip"), Path.of("b.zip"), similarity, List.of(question),
                question, algorithm);
    }
}
