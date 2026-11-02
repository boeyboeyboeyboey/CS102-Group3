package autograder;

import autograder.config.SanityConfig;
import autograder.grading.GradingRun;
import autograder.model.AssessmentDefinition;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanityAnalysisTest {
    private static final SanityConfig CONFIG = new SanityConfig(true, 5, 90, 80, 50, 40);

    @TempDir
    Path temporaryDirectory;

    @Test
    void blockedCsvStillPrintsFindingsAndReportsNotSaved() throws Exception {
        Files.createDirectory(temporaryDirectory.resolve(SanityAnalysis.REPORT));
        GradingRun run = run(allCompileErrors(5), temporaryDirectory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        SanityAnalysis.Result result;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            result = SanityAnalysis.analyze(run, CONFIG);
        } finally {
            System.setOut(previous);
        }
        String text = output.toString(StandardCharsets.UTF_8);
        assertEquals(1, result.exitCode());
        assertFalse(result.findings().isEmpty());
        assertTrue(text.contains("ALL_COMPILE_ERRORS"));
        assertTrue(text.contains("not saved"));
        assertFalse(text.contains("Sanity checks: none"));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve(SanityAnalysis.REPORT)));
    }

    @Test
    void crashedCheckWritesNoCsvAndDoesNotReadAsClean() throws Exception {
        GradingRun run = new GradingRun(null, temporaryDirectory, Map.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        SanityAnalysis.Result result;
        try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setOut(out);
            System.setErr(err);
            result = SanityAnalysis.analyze(run, CONFIG);
        } finally {
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
        assertEquals(1, result.exitCode());
        assertTrue(result.findings().isEmpty());
        assertFalse(Files.exists(temporaryDirectory.resolve(SanityAnalysis.REPORT)));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Sanity checks: none"));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("could not run sanity checks"));
    }

    private static List<StudentGrade> allCompileErrors(int count) {
        List<StudentGrade> grades = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StudentIdentity identity = new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                    null, "fixture", List.of());
            SubmissionResult submission = new SubmissionResult(Path.of("s" + i + ".zip"), null, null, null,
                    identity, List.of(), List.of(), List.of(), SubmissionStatus.READY, true);
            TestCaseDefinition definition = new TestCaseDefinition("Q1-1", "Q1", "Runner", List.of(), 1);
            List<TestCaseResult> results = List.of(new TestCaseResult(definition, TestStatus.COMPILE_ERROR,
                    1, "", ""));
            grades.add(new StudentGrade(submission, results));
        }
        return grades;
    }

    private static GradingRun run(List<StudentGrade> grades, Path directory) {
        QuestionDefinition question = new QuestionDefinition("Q1", "Q1", "A.java", Path.of("T.java"), 1, 1,
                List.of(), List.of(), List.of());
        AssessmentDefinition assessment = new AssessmentDefinition("a", "A", List.of(question));
        return new GradingRun(new BatchResult(assessment, grades, List.of(), List.of()), directory, Map.of());
    }
}
