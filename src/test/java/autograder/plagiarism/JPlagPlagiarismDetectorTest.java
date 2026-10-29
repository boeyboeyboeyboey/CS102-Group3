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

class JPlagPlagiarismDetectorTest {
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
        SubmissionResult original = submission("original", ORIGINAL);
        SubmissionResult copy = submission("copy", RENAMED_COPY);
        SubmissionResult unrelated = submission("unrelated", UNRELATED);

        List<SimilarityFinding> findings = detector(starterDirectory(), 0.6)
                .compare(List.of(original, copy, unrelated), assessment(List.of()));

        assertEquals(1, findings.size(), findings.toString());
        SimilarityFinding finding = findings.get(0);
        assertEquals(original.getArchivePath(), finding.getFirstArchive());
        assertEquals(copy.getArchivePath(), finding.getSecondArchive());
        assertEquals("Q1a", finding.getQuestion());
        assertEquals("JPlag", finding.getAlgorithm());
        assertTrue(finding.getSimilarity() > 0.9, "renamed copy scored " + finding.getSimilarity());
        assertTrue(finding.getEvidence().get(0).startsWith("Q1a (JPlag): "), finding.getEvidence().get(0));
        assertTrue(finding.getEvidence().get(0).contains("starter code excluded"));
        assertTrue(finding.getEvidence().get(1).matches(
                "first Q1a\\.java lines \\d+-\\d+ matches second Q1a\\.java lines \\d+-\\d+ \\(\\d+ tokens\\)"),
                finding.getEvidence().get(1));
    }

    @Test
    void sharedStarterCodeAloneIsNotFlagged() throws Exception {
        List<SubmissionResult> pair = List.of(submission("first", ORIGINAL), submission("second", UNRELATED));
        Path emptyStarter = Files.createDirectories(temporaryDirectory.resolve("no-starter"));

        List<SimilarityFinding> withoutExclusion = detector(emptyStarter, 0.3)
                .compare(pair, assessment(List.of()));
        List<SimilarityFinding> withExclusion = detector(starterDirectory(), 0.3)
                .compare(pair, assessment(List.of()));

        assertEquals(1, withoutExclusion.size());
        assertTrue(withoutExclusion.get(0).getEvidence().get(0).contains("no starter file found"));
        assertTrue(withExclusion.isEmpty(), withExclusion.toString());
    }

    @Test
    void unparsableSubmissionIsSkippedWhileOthersAreCompared() throws Exception {
        SubmissionResult broken = submission("broken", "public class Q1a { int x = ; for ( }");
        SubmissionResult original = submission("original", ORIGINAL);
        SubmissionResult copy = submission("copy", RENAMED_COPY);

        List<SimilarityFinding> findings = detector(starterDirectory(), 0.6)
                .compare(List.of(broken, original, copy), assessment(List.of()));

        assertEquals(1, findings.size());
        assertEquals(original.getArchivePath(), findings.get(0).getFirstArchive());
    }

    @Test
    void fewerThanTwoComparableSubmissionsAreSkippedWithoutFailing() throws Exception {
        SubmissionResult present = submission("present", ORIGINAL);
        SubmissionResult missingFile = submission("missing", null);
        SubmissionResult noWorkingCopy = new SubmissionResult(temporaryDirectory.resolve("failed.zip"),
                temporaryDirectory, null, null, identity(), List.of(), List.of(), List.of(),
                SubmissionStatus.FAILED, false);

        List<SimilarityFinding> findings = detector(starterDirectory(), 0.1)
                .compare(List.of(present, missingFile, noWorkingCopy), assessment(List.of()));

        assertTrue(findings.isEmpty());
    }

    @Test
    void companionFilesAreComparedWithTheAnswer() throws Exception {
        String helper = "public class Helper {\n" + ORIGINAL.substring(ORIGINAL.indexOf("    public static int solve"),
                ORIGINAL.indexOf("    public static void main")) + "}\n";
        SubmissionResult first = submission("first", STARTER);
        SubmissionResult second = submission("second", STARTER);
        Files.writeString(first.getWorkingRoot().resolve("Q1/Helper.java"), helper);
        Files.writeString(second.getWorkingRoot().resolve("Q1/Helper.java"),
                helper.replace("best", "top").replace("value", "item"));

        List<SimilarityFinding> findings = detector(starterDirectory(), 0.6)
                .compare(List.of(first, second), assessment(List.of("Helper.java")));

        assertEquals(1, findings.size());
        assertTrue(findings.get(0).getEvidence().get(1).contains("Helper.java"), findings.get(0).getEvidence().toString());
    }

    private JPlagPlagiarismDetector detector(Path starter, double threshold) {
        return new JPlagPlagiarismDetector(new PlagiarismConfig(true, List.of(PlagiarismConfig.JPLAG), 5, 0.8, 10,
                threshold, 9, starter));
    }

    private AssessmentDefinition assessment(List<String> companions) {
        QuestionDefinition question = new QuestionDefinition("Q1a", "Q1", "Q1a.java",
                temporaryDirectory.resolve("Q1aTester.java"), 1, 1.0, List.of(), List.of(), companions);
        return new AssessmentDefinition("test", "Test", List.of(question));
    }

    private Path starterDirectory() throws Exception {
        Path root = temporaryDirectory.resolve("starter");
        Files.createDirectories(root.resolve("Q1"));
        Files.writeString(root.resolve("Q1/Q1a.java"), STARTER);
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
