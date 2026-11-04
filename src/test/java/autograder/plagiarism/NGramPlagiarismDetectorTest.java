package autograder.plagiarism;

import autograder.config.PlagiarismConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.StudentIdentity;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NGramPlagiarismDetectorTest {
    private static final String STARTER = """
            import java.util.*;
            public class Q1a {
                public static int solve(ArrayList<Integer> inputs) {
                    return 0;
                }
                public static void main(String[] args) {
                    ArrayList<Integer> inputs = new ArrayList<>();
                    inputs.add(1);
                    inputs.add(2);
                    System.out.println("Test 1: " + solve(inputs));
                    inputs = new ArrayList<>();
                    inputs.add(3);
                    System.out.println("Test 2: " + solve(inputs));
                }
            }
            """;
    private static final String ORIGINAL = STARTER.replace("        return 0;\n", """
                    int best = 0;
                    for (int index = 0; index < inputs.size(); index++) {
                        int value = inputs.get(index);
                        if (value % 2 == 0 && value > best) {
                            best = value;
                        }
                    }
                    while (best > 100) {
                        best = best / 10;
                    }
                    return best;
            """);
    private static final String RENAMED_COPY = STARTER.replace("        return 0;\n", """
                    // my own work
                    int largest = 0;
                    for (int i = 0; i < inputs.size(); i++)
                    {
                        int number = inputs.get(i);
                        if (number % 2 == 0 && number > largest) { largest = number; }
                    }
                    while (largest > 250) {
                        largest = largest / 3;
                    }
                    return largest;
            """);
    private static final String UNRELATED = STARTER.replace("        return 0;\n", """
                    Set<Integer> seen = new HashSet<>();
                    int count = 0;
                    for (Integer item : inputs) {
                        if (!seen.contains(item)) {
                            seen.add(item);
                            count += item * item;
                        }
                    }
                    try {
                        return Math.max(count - inputs.size(), -1);
                    } catch (RuntimeException exception) {
                        throw new IllegalStateException("bad input");
                    }
            """);

    @TempDir
    Path temporaryDirectory;

    @Test
    void flagsRenamedCopyWithLineEvidenceButNotUnrelatedWork() throws Exception {
        Path starter = starterDirectory(STARTER);
        SubmissionResult original = submission("original", ORIGINAL);
        SubmissionResult copy = submission("copy", RENAMED_COPY);
        SubmissionResult unrelated = submission("unrelated", UNRELATED);

        List<SimilarityFinding> findings = detector(starter, 0.8)
                .compare(List.of(original, copy, unrelated), assessment());

        assertEquals(1, findings.size());
        SimilarityFinding finding = findings.get(0);
        assertEquals(original.getArchivePath(), finding.getFirstArchive());
        assertEquals(copy.getArchivePath(), finding.getSecondArchive());
        assertEquals(1.0, finding.getSimilarity());
        assertEquals("Q1a", finding.getQuestion());
        assertEquals("n-gram", finding.getAlgorithm());
        assertEquals(finding.getEvidence().size(), new java.util.HashSet<>(finding.getEvidence()).size(),
                "evidence lines should not repeat");
        assertTrue(finding.getEvidence().get(0).startsWith("Q1a (n-gram): 100.0% similar"));
        assertTrue(finding.getEvidence().get(0).contains("starter code excluded"));
        assertTrue(finding.getEvidence().get(1).matches("first Q1a\\.java lines \\d+-\\d+ matches second Q1a\\.java lines \\d+-\\d+"),
                finding.getEvidence().get(1));
    }

    @Test
    void sharedStarterCodeAloneIsNotFlagged() throws Exception {
        SubmissionResult first = submission("first", ORIGINAL);
        SubmissionResult second = submission("second", UNRELATED);
        Path emptyStarter = Files.createDirectories(temporaryDirectory.resolve("no-starter"));

        List<SimilarityFinding> withoutExclusion = detector(emptyStarter, 0.3)
                .compare(List.of(first, second), assessment());
        List<SimilarityFinding> withExclusion = detector(starterDirectory(STARTER), 0.3)
                .compare(List.of(first, second), assessment());

        assertEquals(1, withoutExclusion.size());
        assertTrue(withExclusion.isEmpty());
    }

    @Test
    void untouchedStarterSubmissionsAreTooShortToCompare() throws Exception {
        List<SimilarityFinding> findings = detector(starterDirectory(STARTER), 0.8)
                .compare(List.of(submission("blank-a", STARTER), submission("blank-b", STARTER)), assessment());

        assertTrue(findings.isEmpty());
    }

    @Test
    void missingAnswerFileAndMissingWorkingCopyAreSkipped() throws Exception {
        SubmissionResult present = submission("present", ORIGINAL);
        SubmissionResult missingFile = submission("missing", null);
        SubmissionResult noWorkingCopy = new SubmissionResult(temporaryDirectory.resolve("failed.zip"),
                temporaryDirectory, null, null, identity(), List.of(), List.of(), List.of(),
                SubmissionStatus.FAILED, false);

        List<SimilarityFinding> findings = detector(starterDirectory(STARTER), 0.1)
                .compare(List.of(present, missingFile, noWorkingCopy), assessment());

        assertTrue(findings.isEmpty());
    }

    @Test
    void thresholdFromConfigurationControlsWhichPairsAreReported() throws Exception {
        Path starter = starterDirectory(STARTER);
        List<SubmissionResult> pair = List.of(submission("a", ORIGINAL), submission("b", UNRELATED));
        double score = detector(starter, 0.01).compare(pair, assessment()).get(0).getSimilarity();

        assertTrue(score < 0.8, "unrelated work should score well below the default threshold");
        assertEquals(1, detector(starter, score).compare(pair, assessment()).size());
        assertTrue(detector(starter, Math.min(1.0, score + 0.01)).compare(pair, assessment()).isEmpty());
    }

    private NGramPlagiarismDetector detector(Path starter, double threshold) {
        return new NGramPlagiarismDetector(new PlagiarismConfig(true, List.of(PlagiarismConfig.NGRAM), 5,
                threshold, 10, 0.6, 9, starter));
    }

    private AssessmentDefinition assessment() {
        QuestionDefinition question = new QuestionDefinition("Q1a", "Q1", "Q1a.java",
                temporaryDirectory.resolve("Q1aTester.java"), 1, 1.0, List.of(), List.of(), List.of());
        return new AssessmentDefinition("test", "Test", List.of(question));
    }

    private Path starterDirectory(String source) throws Exception {
        Path root = temporaryDirectory.resolve("starter");
        Files.createDirectories(root.resolve("Q1"));
        Files.writeString(root.resolve("Q1/Q1a.java"), source);
        return root;
    }

    private SubmissionResult submission(String name, String q1aSource) throws Exception {
        Path root = temporaryDirectory.resolve(name);
        Files.createDirectories(root.resolve("Q1"));
        if (q1aSource != null) {
            Files.writeString(root.resolve("Q1/Q1a.java"), q1aSource);
        }
        return new SubmissionResult(temporaryDirectory.resolve(name + ".zip"), root, root, root,
                identity(), List.of("Q1"), List.of(), List.of(), SubmissionStatus.READY, true);
    }

    private StudentIdentity identity() {
        return new StudentIdentity("", "", "", List.of(), List.of(), List.of(), null, "test", List.of());
    }
}
