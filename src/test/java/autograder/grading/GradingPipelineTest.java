package autograder.grading;

import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.csv.CsvCodec;
import autograder.model.GradingReportContext;
import autograder.model.SimilarityFinding;
import autograder.model.StudentIdentity;
import autograder.model.StudentRecord;
import autograder.model.StudentGrade;
import autograder.model.TestCaseResult;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestStatus;
import autograder.report.html.OfflineHtmlReportGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingPipelineTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String LOOPING_Q1A = """
            import java.util.ArrayList;
            public class Q1a {
                public static ArrayList<String> getIsogramWords(ArrayList<String> inputs) {
                    if (inputs.contains("ellen")) { while (true) {} }
                    ArrayList<String> result = new ArrayList<>();
                    for (String word : inputs) {
                        java.util.HashSet<Character> seen = new java.util.HashSet<>();
                        boolean valid = true;
                        for (char letter : word.toLowerCase().toCharArray()) {
                            if (!Character.isLetter(letter) || !seen.add(letter)) { valid = false; break; }
                        }
                        if (valid) { result.add(word); }
                    }
                    return result;
                }
            }
            """;
    private static final String PASSING_Q1B = """
            import java.util.ArrayList;
            public class Q1b {
                public static int getSumOfEvenIntegers(ArrayList<Object> inputs) {
                    int sum = 0;
                    for (Object value : inputs) {
                        if (value instanceof Integer number && number % 2 == 0) { sum += number; }
                    }
                    return sum;
                }
            }
            """;

    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsPassLoopPassMarksAndIsolatesCompilationFailureToItsQuestion() throws Exception {
        AppConfig app = AppConfig.load(PROJECT_ROOT.resolve("config.properties"));
        Map<Path, byte[]> protectedInputs = snapshotInputs();
        SubmissionResult looping = submission("looping", student("#01400001", "looping"), true,
                LOOPING_Q1A).withReviewAnomaly("review warning from HTML fixture");
        SubmissionResult broken = submission("broken", student("#01400002", "broken"), true,
                "public class Q1a { broken }");

        GradingRun run = new GradingPipeline(app, execution()).grade(List.of(looping, broken));
        byte[] scoreCsv = Files.readAllBytes(run.directory().resolve("grade-report.csv"));
        byte[] casesCsv = Files.readAllBytes(run.directory().resolve("cases.csv"));
        byte[] submissionsCsv = Files.readAllBytes(run.directory().resolve("submissions.csv"));
        byte[] similarityCsv = Files.readAllBytes(
                run.directory().resolve(GradingPipeline.SIMILARITY_REPORT));
        assertTrue(run.batch().getSimilarities().isEmpty());
        assertEquals(1, CsvCodec.parse(new String(similarityCsv, StandardCharsets.UTF_8)).size());
        Path htmlReport = new OfflineHtmlReportGenerator("dashboard.html").generate(
                new GradingReportContext(run.batch(), run.withheld()), run.directory());
        String html = Files.readString(htmlReport);
        assertTrue(html.contains("looping.zip"));
        assertTrue(html.contains("review warning from HTML fixture"));
        assertTrue(html.contains("REVIEW_REQUIRED"));
        assertTrue(html.contains("EXPORTED"));
        assertTrue(html.contains("TIMED_OUT"));
        assertTrue(html.contains("COMPILE_ERROR"));
        assertTrue(html.contains("href=\"#submission-1-question-1\""));
        assertTrue(html.contains("id=\"submission-1-question-1\""));
        assertTrue(html.contains("href=\"#submission-2-question-1\""));
        assertTrue(html.contains("id=\"submission-2-question-1\""));
        assertTrue(java.util.Arrays.equals(scoreCsv,
                Files.readAllBytes(run.directory().resolve("grade-report.csv"))));
        assertTrue(java.util.Arrays.equals(casesCsv,
                Files.readAllBytes(run.directory().resolve("cases.csv"))));
        assertTrue(java.util.Arrays.equals(submissionsCsv,
                Files.readAllBytes(run.directory().resolve("submissions.csv"))));
        assertTrue(java.util.Arrays.equals(similarityCsv,
                Files.readAllBytes(run.directory().resolve(GradingPipeline.SIMILARITY_REPORT))));

        Map<String, TestStatus> loopingCases = statuses(run, "looping");
        assertEquals(TestStatus.PASSED, loopingCases.get("Q1a-1"));
        assertEquals(TestStatus.TIMED_OUT, loopingCases.get("Q1a-2"));
        assertEquals(TestStatus.PASSED, loopingCases.get("Q1a-3"));
        var loopingGrade = run.batch().getGrades().stream()
                .filter(grade -> grade.getSubmission() == looping).findFirst().orElseThrow();
        // Q1a's three marks are shared evenly across its fixed and dynamic cases.
        assertEquals(0.75, fixedQuestionScore(loopingGrade, "Q1a"));
        assertEquals(3.0, questionMaximum(loopingGrade, "Q1a"));
        assertEquals(3.0, questionMaximum(loopingGrade, "Q1b"));
        assertEquals(5.0, questionMaximum(loopingGrade, "Q2a"));
        assertEquals(5.0, questionMaximum(loopingGrade, "Q2b"));
        assertEquals(4.0, questionMaximum(loopingGrade, "Q3"));
        assertEquals(20.0, totalMaximum(loopingGrade));
        assertTrue(loopingGrade.getTotalScore() <= 20.0);
        // Dynamic cases now run (previously they all errored before this fix).
        assertTrue(loopingCases.get("Q1a-dynamic-1") != TestStatus.NOT_RUN);
        assertTrue(loopingCases.get("Q1a-dynamic-1") != TestStatus.COMPILE_ERROR);
        assertEquals(TestStatus.PASSED, loopingCases.get("Q1b-1"));
        Map<String, TestStatus> brokenCases = statuses(run, "broken");
        assertEquals(TestStatus.COMPILE_ERROR, brokenCases.get("Q1a-1"));
        assertEquals(TestStatus.COMPILE_ERROR, brokenCases.get("Q1a-3"));
        assertEquals(TestStatus.PASSED, brokenCases.get("Q1b-1"));
        // Each submission has 45 results, but the original 20-point maximum is preserved.
        assertEquals(45, run.batch().getGrades().get(0).getTestResults().size());
        assertEquals(45, run.batch().getGrades().get(1).getTestResults().size());

        List<List<String>> submissionRows = CsvCodec.parse(
                Files.readString(run.directory().resolve("submissions.csv")));
        assertEquals(3, submissionRows.size());
        List<List<String>> similarityRows = CsvCodec.parse(
                Files.readString(run.directory().resolve(GradingPipeline.SIMILARITY_REPORT)));
        assertEquals(List.of("FirstArchive", "FirstUsername", "SecondArchive", "SecondUsername",
                "Similarity", "Evidence"), similarityRows.get(0));
        List<List<String>> reportRows = CsvCodec.parse(
                Files.readString(run.directory().resolve("grade-report.csv")));
        int idColumn = reportRows.get(0).indexOf("OrgDefinedId");
        int scoreColumn = reportRows.get(0).indexOf("Calculated Final Grade Numerator");
        List<String> exportedLooping = reportRows.stream().filter(row -> row.get(idColumn).equals("#01400001"))
                .findFirst().orElseThrow();
        assertEquals(Double.toString(loopingGrade.getTotalScore()), exportedLooping.get(scoreColumn));
        assertEquals("", protectedScore(reportRows, "#01400005"));
        assertProtectedInputsUnchanged(protectedInputs);
    }

    @Test
    void withholdsDuplicateAndUnresolvedIdentitiesAndRecordsMissingSourceAndRuntimeErrors() throws Exception {
        AppConfig app = AppConfig.load(PROJECT_ROOT.resolve("config.properties"));
        SubmissionResult duplicateA = submission("duplicate-a", student("#01400005", "duplicate-a"), true, null);
        SubmissionResult duplicateB = submission("duplicate-b", student("#01400005", "duplicate-b"), true, null);
        SubmissionResult unresolved = submission("unresolved", null, true, null);
        SubmissionResult missing = submission("missing-source", student("#01400003", "missing"), false, null);
        SubmissionResult runtime = submission("runtime", student("#01400004", "runtime"), true,
                "public class Q1a { public static java.util.ArrayList<String> getIsogramWords("
                        + "java.util.ArrayList<String> inputs) { System.exit(0); return inputs; } }");

        GradingRun run = new GradingPipeline(app, execution()).grade(
                List.of(duplicateA, duplicateB, unresolved, missing, runtime));
        byte[] scoreCsv = Files.readAllBytes(run.directory().resolve("grade-report.csv"));
        byte[] casesCsv = Files.readAllBytes(run.directory().resolve("cases.csv"));
        byte[] submissionsCsv = Files.readAllBytes(run.directory().resolve("submissions.csv"));
        Path htmlReport = new OfflineHtmlReportGenerator("dashboard.html").generate(
                new GradingReportContext(run.batch(), run.withheld()), run.directory());
        String html = Files.readString(htmlReport);
        assertTrue(html.contains("duplicate-a.zip"));
        assertTrue(html.contains("duplicate-b.zip"));
        assertTrue(html.contains("unresolved.zip"));
        assertTrue(html.contains("WITHHELD"));
        assertTrue(html.contains("duplicate submissions for the same student"));
        assertTrue(html.contains("NOT_RUN"));
        assertTrue(html.contains("missing-source.zip"));
        assertTrue(html.contains("COMPILE_ERROR"));
        assertTrue(html.contains("runtime.zip"));
        assertTrue(html.contains("RUNTIME_ERROR"));
        for (int ordinal = 1; ordinal <= 3; ordinal++) {
            assertTrue(html.contains("href=\"#submission-" + ordinal + "-question-1\""));
            assertTrue(html.contains("id=\"submission-" + ordinal + "-question-1\""));
        }
        assertTrue(java.util.Arrays.equals(scoreCsv,
                Files.readAllBytes(run.directory().resolve("grade-report.csv"))));
        assertTrue(java.util.Arrays.equals(casesCsv,
                Files.readAllBytes(run.directory().resolve("cases.csv"))));
        assertTrue(java.util.Arrays.equals(submissionsCsv,
                Files.readAllBytes(run.directory().resolve("submissions.csv"))));

        assertTrue(run.withheld().containsKey(duplicateA));
        assertTrue(run.withheld().containsKey(duplicateB));
        assertTrue(run.withheld().containsKey(unresolved));
        for (var grade : run.batch().getGrades()) {
            assertEquals(45, grade.getTestResults().size());
        }
        assertEquals(TestStatus.NOT_RUN, statuses(run, "duplicate-a").get("Q1a-1"));
        assertEquals(TestStatus.COMPILE_ERROR, statuses(run, "missing").get("Q1a-1"));
        assertEquals(TestStatus.RUNTIME_ERROR, statuses(run, "runtime").get("Q1a-1"));

        List<List<String>> rows = CsvCodec.parse(Files.readString(run.directory().resolve("submissions.csv")));
        assertEquals(6, rows.size());
        long withheldRows = rows.stream().skip(1).filter(row -> row.get(2).equals("WITHHELD")).count();
        assertEquals(3, withheldRows);
        List<List<String>> reportRows = CsvCodec.parse(
                Files.readString(run.directory().resolve("grade-report.csv")));
        assertEquals(7, reportRows.size());
        assertEquals("", protectedScore(reportRows, "#01400005"));
    }

    @Test
    void injectedSimilarityFindingsReachBatchCsvAndHtmlWithoutChangingWithholding() throws Exception {
        AppConfig app = AppConfig.load(PROJECT_ROOT.resolve("config.properties"));
        SubmissionResult first = submission("similar-first", null, false, null);
        SubmissionResult second = submission("similar-second", null, false, null);
        List<SubmissionResult> submissions = List.of(first, second);
        SimilarityFinding finding = new SimilarityFinding(first.getArchivePath(), second.getArchivePath(),
                0.87654, List.of("Q1a.java:4-8 <shared>"));

        GradingRun run = new GradingPipeline(app, execution(), (actualSubmissions, assessment) -> {
            assertEquals(submissions, actualSubmissions);
            assertEquals(app.getAssessment(), assessment);
            return List.of(finding);
        }).grade(submissions);

        assertEquals(List.of(finding), run.batch().getSimilarities());
        assertEquals(2, run.withheld().size());
        List<List<String>> similarityRows = CsvCodec.parse(
                Files.readString(run.directory().resolve(GradingPipeline.SIMILARITY_REPORT)));
        assertEquals(List.of("FirstArchive", "FirstUsername", "SecondArchive", "SecondUsername",
                "Similarity", "Evidence"), similarityRows.get(0));
        assertEquals(List.of("similar-first.zip", "", "similar-second.zip", "", "0.877",
                "Q1a.java:4-8 <shared>"), similarityRows.get(1));

        Path html = new OfflineHtmlReportGenerator("dashboard.html").generate(
                new GradingReportContext(run.batch(), run.withheld()), run.directory());
        String report = Files.readString(html);
        assertTrue(report.contains("Similarity: 87.654%"));
        assertTrue(report.contains("Q1a.java:4-8 &lt;shared&gt;"));
        assertFalse(report.contains("Q1a.java:4-8 <shared>"));
    }

    @Test
    void detectorFailuresRemainAdvisoryAndLeaveGradeDiagnosticsAvailable() throws Exception {
        AppConfig app = AppConfig.load(PROJECT_ROOT.resolve("config.properties"));
        SubmissionResult first = submission("failed-detector-first", null, false, null);
        SubmissionResult second = submission("failed-detector-second", null, false, null);
        List<SubmissionResult> submissions = List.of(first, second);
        for (boolean runtimeFailure : List.of(false, true)) {
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            PrintStream originalError = System.err;
            System.setErr(new PrintStream(errors, true, StandardCharsets.UTF_8));
            GradingRun run;
            try {
                run = new GradingPipeline(app, execution(), (actualSubmissions, assessment) -> {
                    if (runtimeFailure) {
                        throw new IllegalStateException("detector unavailable");
                    }
                    throw new IOException("detector unavailable");
                }).grade(submissions);
            } finally {
                System.setErr(originalError);
            }

            assertTrue(errors.toString(StandardCharsets.UTF_8).contains(
                    "similarity check skipped: detector unavailable"));
            assertTrue(run.batch().getSimilarities().isEmpty());
            assertEquals(2, run.withheld().size());
            List<List<String>> similarityRows = CsvCodec.parse(
                    Files.readString(run.directory().resolve(GradingPipeline.SIMILARITY_REPORT)));
            assertEquals(1, similarityRows.size());
            List<List<String>> submissionsRows = CsvCodec.parse(
                    Files.readString(run.directory().resolve("submissions.csv")));
            assertEquals(3, submissionsRows.size());
            assertEquals(List.of("WITHHELD", "WITHHELD"), submissionsRows.subList(1, 3).stream()
                    .map(row -> row.get(2)).toList());
            assertTrue(Files.exists(run.directory().resolve("cases.csv")));
            assertTrue(Files.exists(run.directory().resolve("grade-report.csv")));
        }
    }

    private ExecutionConfig execution() throws Exception {
        Path output = temporaryDirectory.resolve("grading");
        return new ExecutionConfig(output, "grade-report.csv",
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                15000, 1000, 512);
    }

    private SubmissionResult submission(String archiveName, StudentRecord student,
                                        boolean includeQ1a, String q1aSource) throws Exception {
        Path root = temporaryDirectory.resolve(archiveName + "-working");
        Files.createDirectories(root.resolve("Q1"));
        Files.createDirectories(root.resolve("Q2"));
        Files.createDirectories(root.resolve("Q3"));
        if (includeQ1a) {
            String source = q1aSource == null
                    ? Files.readString(PROJECT_ROOT.resolve("Project/RenameToYourUsername/Q1/Q1a.java"))
                    : q1aSource;
            Files.writeString(root.resolve("Q1/Q1a.java"), source);
        }
        copyReference(root, "Q1/Q1b.java");
        Files.writeString(root.resolve("Q1/Q1b.java"), PASSING_Q1B);
        copyReference(root, "Q2/Q2a.java");
        copyReference(root, "Q2/Q2b.java");
        copyReference(root, "Q2/persons.txt");
        copyReference(root, "Q2/students.txt");
        copyReference(root, "Q3/Q3.java");
        copyReference(root, "Q3/ShapeComparator.java");
        Path archive = temporaryDirectory.resolve(archiveName + ".zip");
        Files.write(archive, new byte[0]);
        StudentIdentity identity = student == null
                ? new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                        null, "test", List.of())
                : new StudentIdentity(student.getFirstName() + " " + student.getLastName(),
                        student.getEmail(), student.getUsername(), List.of(), List.of(), List.of(),
                        student, "test", List.of("test identity"));
        return new SubmissionResult(archive, root, root, root, identity,
                List.of("Q1", "Q2", "Q3"), List.of(), List.of(), SubmissionStatus.READY, true);
    }

    private void copyReference(Path root, String relative) throws Exception {
        Path source = PROJECT_ROOT.resolve("Project/RenameToYourUsername").resolve(relative);
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.copy(source, target);
    }

    private static StudentRecord student(String id, String username) {
        return new StudentRecord(id, username, "Test", "Student", username + "@example.test");
    }

    private Map<Path, byte[]> snapshotInputs() throws Exception {
        List<Path> paths = List.of(
                PROJECT_ROOT.resolve("Project/Tester-Files/Q1aTester.java"),
                PROJECT_ROOT.resolve("Project/Tester-Files/personstester.txt"),
                PROJECT_ROOT.resolve("Project/RenameToYourUsername/Q3/Shape.class"),
                PROJECT_ROOT.resolve("Project/IS442-ScoreSheet.csv"));
        java.util.LinkedHashMap<Path, byte[]> snapshots = new java.util.LinkedHashMap<>();
        for (Path path : paths) {
            snapshots.put(path, Files.readAllBytes(path));
        }
        return snapshots;
    }

    private static void assertProtectedInputsUnchanged(Map<Path, byte[]> snapshots) throws Exception {
        for (Map.Entry<Path, byte[]> entry : snapshots.entrySet()) {
            assertTrue(java.util.Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())),
                    "grading modified supplied input " + entry.getKey());
        }
    }

    private static String protectedScore(List<List<String>> rows, String studentId) {
        int idColumn = rows.get(0).indexOf("OrgDefinedId");
        int scoreColumn = rows.get(0).indexOf("Calculated Final Grade Numerator");
        return rows.stream().filter(row -> row.get(idColumn).equals(studentId))
                .findFirst().orElseThrow().get(scoreColumn);
    }

    private static double fixedQuestionScore(StudentGrade grade, String questionId) {
        double total = 0;
        for (TestCaseResult result : grade.getTestResults()) {
            String id = result.getTestCase().getId();
            if (id.startsWith(questionId + "-") && !id.contains("dynamic")) {
                total += result.getScore();
            }
        }
        return total;
    }

    private static double questionMaximum(StudentGrade grade, String questionId) {
        BigDecimal total = BigDecimal.ZERO;
        for (TestCaseResult result : grade.getTestResults()) {
            if (result.getTestCase().getQuestionId().equals(questionId)) {
                total = total.add(BigDecimal.valueOf(result.getTestCase().getMaximumScore()));
            }
        }
        return total.doubleValue();
    }

    private static double totalMaximum(StudentGrade grade) {
        BigDecimal total = BigDecimal.ZERO;
        for (TestCaseResult result : grade.getTestResults()) {
            total = total.add(BigDecimal.valueOf(result.getTestCase().getMaximumScore()));
        }
        return total.doubleValue();
    }

    private static Map<String, TestStatus> statuses(GradingRun run, String username) {
        return run.batch().getGrades().stream()
                .filter(grade -> grade.getSubmission().getIdentity().getUsername().equals(username))
                .findFirst().orElseThrow().getTestResults().stream()
                .collect(Collectors.toMap(result -> result.getTestCase().getId(),
                        result -> result.getStatus()));
    }
}
