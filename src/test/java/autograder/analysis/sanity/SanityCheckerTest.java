package autograder.analysis.sanity;

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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SanityCheckerTest {
    private static final SanityConfig CONFIG = new SanityConfig(true, 5, 90, 80, 50, 40);

    private final SanityChecker checker = new SanityChecker();

    @Test
    void ignoresEverythingBelowMinimumGradedCount() {
        GradingRun run = run(4, i -> List.of(result("Q1-1", TestStatus.COMPILE_ERROR, "", "")), Map.of());
        assertTrue(checker.check(run, CONFIG).isEmpty());
    }

    @Test
    void withheldSubmissionsDoNotCountAsGraded() {
        List<StudentGrade> grades = grades(5, i -> List.of(result("Q1-1", TestStatus.COMPILE_ERROR, "", "")));
        Map<SubmissionResult, String> withheld = new HashMap<>();
        withheld.put(grades.get(0).getSubmission(), "unresolved");
        assertTrue(checker.check(run(grades, withheld), CONFIG).isEmpty());
    }

    @Test
    void allCompileErrorsFiresAndSuppressesMostlyZero() {
        GradingRun run = run(5, i -> List.of(result("Q1-1", TestStatus.COMPILE_ERROR, "", "")), Map.of());
        List<SanityFinding> findings = checker.check(run, CONFIG);
        assertEquals(1, findings.size());
        assertEquals(SanityCheck.ALL_COMPILE_ERRORS, findings.get(0).getCheck());
        assertEquals(5, findings.get(0).getAffected());
    }

    @Test
    void mostlyZeroFiresAtThresholdButNotJustBelow() {
        IntFunction<List<TestCaseResult>> nineOfTenZero = i -> List.of(result("Q1-1",
                i == 0 ? TestStatus.PASSED : TestStatus.TIMED_OUT, "", ""));
        assertTrue(has(checker.check(run(10, nineOfTenZero, Map.of()), CONFIG), SanityCheck.QUESTION_MOSTLY_ZERO));
        IntFunction<List<TestCaseResult>> eightOfTenZero = i -> List.of(result("Q1-1",
                i < 2 ? TestStatus.PASSED : TestStatus.RUNTIME_ERROR, "e" + i, ""));
        assertFalse(has(checker.check(run(10, eightOfTenZero, Map.of()), CONFIG), SanityCheck.QUESTION_MOSTLY_ZERO));
    }

    @Test
    void mostlyZeroFiresWhenOnlyFreePassCaseIsExcluded() {
        IntFunction<List<TestCaseResult>> onePasses = i -> List.of(
                result("Q1-1", TestStatus.PASSED, "", ""),
                result("Q1-2", i == 0 ? TestStatus.PASSED : TestStatus.TIMED_OUT, "", ""));
        SanityFinding finding = checker.check(run(10, onePasses, Map.of()), CONFIG).stream()
                .filter(f -> f.getCheck() == SanityCheck.QUESTION_MOSTLY_ZERO)
                .findFirst().orElseThrow();
        assertEquals(9, finding.getAffected());
        assertEquals(10, finding.getGraded());
    }

    @Test
    void mostlyZeroDoesNotFireWhenEveryCaseIsFreePass() {
        IntFunction<List<TestCaseResult>> allPass = i -> List.of(
                result("Q1-1", TestStatus.PASSED, "", ""),
                result("Q1-2", TestStatus.PASSED, "", ""));
        assertFalse(has(checker.check(run(10, allPass, Map.of()), CONFIG), SanityCheck.QUESTION_MOSTLY_ZERO));
    }

    @Test
    void casePassedByOnlySomeStudentsIsNotFreePass() {
        // Q1-1 is passed by 9 of 10, so excluding it would wrongly zero everyone
        IntFunction<List<TestCaseResult>> almostAllPass = i -> List.of(
                result("Q1-1", i < 9 ? TestStatus.PASSED : TestStatus.FAILED, "o" + i, ""),
                result("Q1-2", TestStatus.TIMED_OUT, "", ""));
        assertFalse(has(checker.check(run(10, almostAllPass, Map.of()), CONFIG), SanityCheck.QUESTION_MOSTLY_ZERO));
    }

    @Test
    void sharedFailureNeedsIdenticalStatusAndOutputAtThreshold() {
        IntFunction<List<TestCaseResult>> fourOfFive = i -> List.of(result("Q1-1",
                i < 4 ? TestStatus.FAILED : TestStatus.PASSED, " same line\nsecond ", ""));
        List<SanityFinding> findings = checker.check(run(5, fourOfFive, Map.of()), CONFIG);
        SanityFinding finding = findings.stream().filter(f -> f.getCheck() == SanityCheck.SHARED_FAILURE)
                .findFirst().orElseThrow();
        assertEquals("Q1-1", finding.getCaseId());
        assertEquals(4, finding.getAffected());
        assertTrue(finding.getMessage().contains("FAILED") && finding.getMessage().contains("same line"));
        assertFalse(finding.getMessage().contains("second"));
        IntFunction<List<TestCaseResult>> threeOfFive = i -> List.of(result("Q1-1",
                i < 3 ? TestStatus.FAILED : TestStatus.PASSED, "same", ""));
        assertFalse(has(checker.check(run(5, threeOfFive, Map.of()), CONFIG), SanityCheck.SHARED_FAILURE));
        IntFunction<List<TestCaseResult>> differing = i -> List.of(result("Q1-1", TestStatus.FAILED, "out" + i, ""));
        assertFalse(has(checker.check(run(5, differing, Map.of()), CONFIG), SanityCheck.SHARED_FAILURE));
        IntFunction<List<TestCaseResult>> silent = i -> List.of(result("Q1-1", TestStatus.FAILED, " ", ""));
        assertFalse(has(checker.check(run(5, silent, Map.of()), CONFIG), SanityCheck.SHARED_FAILURE));
    }

    @Test
    void clusteredTimeoutsFireAtHalf() {
        IntFunction<List<TestCaseResult>> half = i -> List.of(result("Q1-1",
                i < 3 ? TestStatus.TIMED_OUT : TestStatus.PASSED, "", ""));
        SanityConfig six = new SanityConfig(true, 6, 90, 80, 50, 40);
        assertTrue(has(checker.check(run(6, half, Map.of()), six), SanityCheck.CLUSTERED_TIMEOUTS));
        IntFunction<List<TestCaseResult>> below = i -> List.of(result("Q1-1",
                i < 2 ? TestStatus.TIMED_OUT : TestStatus.PASSED, "", ""));
        assertFalse(has(checker.check(run(6, below, Map.of()), six), SanityCheck.CLUSTERED_TIMEOUTS));
    }

    @Test
    void gapFiresOnlyWhenFixedPassesMoreThanDynamicByThePoints() {
        IntFunction<List<TestCaseResult>> fixedBetter = i -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                result("Q1-dynamic-1", i < 2 ? TestStatus.PASSED : TestStatus.FAILED, "o" + i, ""));
        List<SanityFinding> findings = checker.check(run(5, fixedBetter, Map.of()), CONFIG);
        SanityFinding gap = findings.stream().filter(f -> f.getCheck() == SanityCheck.FIXED_DYNAMIC_GAP)
                .findFirst().orElseThrow();
        assertEquals("", gap.getCaseId());
        assertEquals(3, gap.getAffected());
        IntFunction<List<TestCaseResult>> small = i -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                result("Q1-dynamic-1", i < 4 ? TestStatus.PASSED : TestStatus.FAILED, "o" + i, ""));
        assertFalse(has(checker.check(run(5, small, Map.of()), CONFIG), SanityCheck.FIXED_DYNAMIC_GAP));
        IntFunction<List<TestCaseResult>> dynamicBetter = i -> List.of(
                result("Q1-1", i < 2 ? TestStatus.PASSED : TestStatus.FAILED, "o" + i, ""),
                result("Q1-dynamic-1", TestStatus.PASSED, "", ""));
        assertFalse(has(checker.check(run(5, dynamicBetter, Map.of()), CONFIG), SanityCheck.FIXED_DYNAMIC_GAP));
    }

    @Test
    void gapCountsStudentsNotCaseResults() {
        IntFunction<List<TestCaseResult>> students = i -> switch (i) {
            case 0 -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                    result("Q1-dynamic-1", TestStatus.PASSED, "", ""),
                    result("Q1-dynamic-2", TestStatus.PASSED, "", ""));
            case 4 -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                    result("Q1-dynamic-1", TestStatus.COMPILE_ERROR, "", ""),
                    result("Q1-dynamic-2", TestStatus.COMPILE_ERROR, "", ""));
            case 5 -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                    result("Q1-dynamic-1", TestStatus.PASSED, "", ""),
                    result("Q1-dynamic-2", TestStatus.FAILED, "o", ""));
            default -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                    result("Q1-dynamic-1", TestStatus.FAILED, "o" + i, ""),
                    result("Q1-dynamic-2", TestStatus.FAILED, "p" + i, ""));
        };
        SanityFinding gap = checker.check(run(6, students, Map.of()), CONFIG).stream()
                .filter(f -> f.getCheck() == SanityCheck.FIXED_DYNAMIC_GAP)
                .findFirst().orElseThrow();
        // 10 non-compile dynamic case results, 3 passed; 5 graded students, 4 with a non-passing dynamic result
        assertEquals(4, gap.getAffected());
        assertEquals(5, gap.getGraded());
    }

    @Test
    void compileErrorCasesAreExcludedFromGapRates() {
        IntFunction<List<TestCaseResult>> mixed = i -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                result("Q1-dynamic-1", i < 4 ? TestStatus.COMPILE_ERROR : TestStatus.FAILED, "o", ""));
        SanityConfig low = new SanityConfig(true, 5, 100, 100, 100, 40);
        assertTrue(has(checker.check(run(5, mixed, Map.of()), low), SanityCheck.FIXED_DYNAMIC_GAP));
        IntFunction<List<TestCaseResult>> allCompile = i -> List.of(result("Q1-1", TestStatus.PASSED, "", ""),
                result("Q1-dynamic-1", TestStatus.COMPILE_ERROR, "", ""));
        assertFalse(has(checker.check(run(5, allCompile, Map.of()), low), SanityCheck.FIXED_DYNAMIC_GAP));
    }

    @Test
    void csvHasHeaderOnlyWhenEmptyAndOneRowPerFinding() {
        assertEquals("Check,Question,Case,Affected,Graded,Message", SanityCsvWriter.format(List.of()).strip());
        String csv = SanityCsvWriter.format(List.of(
                new SanityFinding(SanityCheck.SHARED_FAILURE, "Q1", "Q1-1", 4, 5, "a, b")));
        assertTrue(csv.contains("SHARED_FAILURE,Q1,Q1-1,4,5,\"a, b\""));
    }

    @Test
    void sharedFailureExcerptStripsControlCharacters() {
        String noisy = "\u001b[31mwrong\u0007 answer";
        IntFunction<List<TestCaseResult>> fourOfFive = i -> List.of(result("Q1-1",
                i < 4 ? TestStatus.FAILED : TestStatus.PASSED, noisy, ""));
        SanityFinding finding = checker.check(run(5, fourOfFive, Map.of()), CONFIG).stream()
                .filter(f -> f.getCheck() == SanityCheck.SHARED_FAILURE)
                .findFirst().orElseThrow();
        String message = finding.getMessage();
        assertTrue(message.contains("wrong"));
        assertFalse(message.contains("\u001b"));
        for (char character : message.toCharArray()) {
            assertFalse(character < 0x20, "control character in message: " + (int) character);
        }
    }

    private static boolean has(List<SanityFinding> findings, SanityCheck check) {
        return findings.stream().anyMatch(f -> f.getCheck() == check);
    }

    private static TestCaseResult result(String id, TestStatus status, String out, String err) {
        String question = id.substring(0, id.indexOf('-'));
        TestCaseDefinition definition = new TestCaseDefinition(id, question, "Runner", List.of(), 1);
        return new TestCaseResult(definition, status, 1, out, err);
    }

    private static List<StudentGrade> grades(int count, IntFunction<List<TestCaseResult>> results) {
        List<StudentGrade> grades = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StudentIdentity identity = new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                    null, "fixture", List.of());
            SubmissionResult submission = new SubmissionResult(Path.of("s" + i + ".zip"), null, null, null,
                    identity, List.of(), List.of(), List.of(), SubmissionStatus.READY, true);
            grades.add(new StudentGrade(submission, results.apply(i)));
        }
        return grades;
    }

    private static GradingRun run(int count, IntFunction<List<TestCaseResult>> results,
                                  Map<SubmissionResult, String> withheld) {
        return run(grades(count, results), withheld);
    }

    private static GradingRun run(List<StudentGrade> grades, Map<SubmissionResult, String> withheld) {
        QuestionDefinition question = new QuestionDefinition("Q1", "Q1", "A.java", Path.of("T.java"), 1, 1,
                List.of(), List.of(), List.of());
        AssessmentDefinition assessment = new AssessmentDefinition("a", "A", List.of(question));
        return new GradingRun(new BatchResult(assessment, grades, List.of(), List.of()), Path.of("."), withheld);
    }
}
