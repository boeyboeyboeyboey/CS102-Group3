package autograder.analysis.boilerplate;

import autograder.config.BoilerplateConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterComparisonReportTest {
    @TempDir
    Path directory;

    @Test
    void reportsAllMethodStatusesAndEscapesSourceWhilePreservingInputs() throws Exception {
        String starter = "class A { int same() { return 0; } "
                + "int changed() { return 0; } void removed() {} }";
        String answer = "class A { int same() { return 0; } "
                + "int changed() { return 1; } String added() { return \"<script>\"; } }";
        prepare("A.java", starter, answer);
        Path csv = Files.writeString(directory.resolve("scores.csv"), "original scores");
        BatchResult batch = batch(List.of("A.java"));
        Path report = StarterComparisonReport.generate(batch, config(), directory);
        String html = Files.readString(report);
        for (String status : List.of("UNCHANGED", "MODIFIED", "ADDED", "REMOVED")) {
            assertTrue(html.contains("data-status=\"" + status + "\""));
        }
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("student&amp;one.zip"));
        assertTrue(html.contains("id=\"search\""));
        assertTrue(html.contains("id=\"expand\""));
        assertFalse(html.contains("<script src="));
        assertFalse(html.contains("<link "));
        assertEquals(starter, Files.readString(directory.resolve("starter/Q/A.java")));
        assertEquals(answer, Files.readString(directory.resolve("answer/Q/A.java")));
        assertEquals("original scores", Files.readString(csv));
        assertEquals(0, batch.getGrades().get(0).getTotalScore());
    }

    @Test
    void continuesAfterMissingOrMalformedFilesAndDistinguishesNoMethods() throws Exception {
        prepare("A.java", "class A { void f( }", "class A {} ");
        prepare("B.java", "class B { int field; }", "class B { int field=1; }");
        prepare("C.java", "class C { void f() {} }", "class C { void f() {} }");
        var batch = batch(List.of("A.java", "missing.java", "B.java", "C.java"));
        String html = Files.readString(StarterComparisonReport.generate(batch, config(), directory));
        assertEquals(2, html.split("data-status=UNAVAILABLE", -1).length - 1);
        assertTrue(html.contains("data-status=NO_METHODS"));
        assertTrue(html.contains("<td>No explicit methods</td>"));
        assertTrue(html.contains("<td>Unavailable: "));
        assertTrue(html.contains("C#f()"));
        assertTrue(html.contains("data-status=\"UNCHANGED\""));
    }

    @Test
    void respectsBoundsAndNeverOverwritesExistingReportDestinations() throws Exception {
        prepare("A.java", "class A { void f() {} }", "class A {} ");
        var tiny = new BoilerplateConfig(true, directory.resolve("starter"), 0.98, 5, 1000);
        Path report = StarterComparisonReport.generate(batch(List.of("A.java")), tiny, directory);
        String original = Files.readString(report);
        assertTrue(original.contains("source exceeds configured byte limit"));
        assertThrows(IOException.class, () -> StarterComparisonReport.generate(
                batch(List.of("A.java")), config(), directory));
        assertEquals(original, Files.readString(report));
    }

    @Test
    void unavailableWorkingCopyDoesNotSuppressOtherStudents() throws Exception {
        prepare("A.java", "class A { void f() {} }", "class A { void f() {} }");
        BatchResult original = batch(List.of("A.java"));
        SubmissionResult missing = new SubmissionResult(Path.of("missing.zip"),
                null, null, null, null, List.of(), List.of(), List.of(),
                SubmissionStatus.FAILED, false);
        BatchResult mixed = new BatchResult(original.getAssessment(),
                List.of(new StudentGrade(missing, List.of()), original.getGrades().get(0)),
                List.of(), List.of());
        String html = Files.readString(StarterComparisonReport.generate(mixed, config(), directory));
        assertTrue(html.contains("No working copy is available"));
        assertTrue(html.contains("data-status=\"UNCHANGED\""));
    }

    @Test
    void overviewLinksAreUniqueAcrossStudentsAndCountsMatchMethodEvidence() throws Exception {
        prepare("A.java", "class A { void same() {} int f() { return 0; } }",
                "class A { void same() {} int f() { return 1; } }");
        BatchResult original = batch(List.of("A.java"));
        var first = original.getGrades().get(0).getSubmission();
        var second = new SubmissionResult(Path.of("other.zip"), first.getWorkingRoot(),
                first.getWorkingRoot(), first.getWorkingRoot(), null, List.of(), List.of(),
                List.of(), SubmissionStatus.READY, true);
        var mixed = new BatchResult(original.getAssessment(),
                List.of(original.getGrades().get(0), new StudentGrade(second, List.of())),
                List.of(), List.of());
        String html = Files.readString(StarterComparisonReport.generate(mixed, config(), directory));
        assertTrue(html.indexOf("id=overview") < html.indexOf("<main>"));
        for (int i = 1; i <= 2; i++) {
            assertTrue(html.contains("href=\"#comparison-" + i + "\""));
            assertEquals(1, html.split("id=\"comparison-" + i + "\"", -1).length - 1);
        }
        assertEquals(2, html.split("<td>Compared</td><td>1</td><td>1</td><td>0</td><td>0</td>", -1).length - 1);
        assertTrue(html.contains("aria-label=\"View why: student&amp;one.zip"));
        assertTrue(html.contains("token(s) inserted"));
        try (var entries = Files.list(directory.resolve("starter-comparison"))) {
            assertEquals(List.of("index.html"), entries.map(path -> path.getFileName().toString()).toList());
        }
    }

    private void prepare(String name, String starter, String answer) throws IOException {
        Files.createDirectories(directory.resolve("starter/Q"));
        Files.createDirectories(directory.resolve("answer/Q"));
        Files.writeString(directory.resolve("starter/Q/" + name), starter);
        Files.writeString(directory.resolve("answer/Q/" + name), answer);
    }

    private BoilerplateConfig config() {
        return new BoilerplateConfig(true, directory.resolve("starter"), 0.98, 10000, 1000);
    }

    private BatchResult batch(List<String> files) {
        QuestionDefinition question = new QuestionDefinition("Q", "Q", files.get(0),
                Path.of("Tester.java"), 1, 1, List.of(), List.of(),
                files.subList(1, files.size()));
        var assessment = new AssessmentDefinition("exam", "Example", List.of(question));
        Path root = directory.resolve("answer");
        SubmissionResult submission = new SubmissionResult(Path.of("student&one.zip"),
                root, root, root, null, List.of(), List.of(), List.of(), SubmissionStatus.READY, true);
        return new BatchResult(assessment, List.of(new StudentGrade(submission, List.of())),
                List.of(), List.of());
    }
}
