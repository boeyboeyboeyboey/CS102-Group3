package autograder;

import autograder.config.FeedbackConfig;
import autograder.config.HtmlReportConfig;
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
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void helpDoesNotRequireConfiguration() {
        assertEquals(0, App.run(new String[] {"--help"}));
    }

    @Test
    void workersFlagRejectsBadValuesAndWrongArgumentCounts() {
        String config = temporaryDirectory.resolve("missing.properties").toString();
        for (String value : new String[] {"0", "33", "-1", "abc", ""}) {
            assertEquals(1, App.run(new String[] {"--workers", value, config}), value);
        }
        assertEquals(1, App.run(new String[] {"--workers"}));
        assertEquals(1, App.run(new String[] {"--workers", "2"}));
        assertEquals(1, App.run(new String[] {"--workers", "2", config, "extra"}));
    }

    @Test
    void workersFlagWithValidValueStillFailsCleanlyOnMissingConfig() {
        String config = temporaryDirectory.resolve("missing.properties").toString();
        assertEquals(1, App.run(new String[] {"--workers", "2", config}));
        assertEquals(1, App.run(new String[] {"--workers", "AUTO", config}));
    }

    @Test
    void missingArgumentsAndMissingConfigFailCleanly() {
        assertEquals(1, App.run(new String[] {}));
        assertEquals(1, App.run(new String[] {
                temporaryDirectory.resolve("missing.properties").toString()}));
    }

    @Test
    void reportsHtmlFailureAsNonzeroAndPreservesCompletedCsvEvidence() throws Exception {
        Files.writeString(temporaryDirectory.resolve("scorereport.csv"), "existing scores\n");
        Files.writeString(temporaryDirectory.resolve("cases.csv"), "existing cases\n");
        Files.writeString(temporaryDirectory.resolve("submissions.csv"), "existing decisions\n");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream original = System.err;
        try (PrintStream capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            int exit = App.generateHtmlReport(run(), new HtmlReportConfig(true, "dashboard.html"),
                    (context, directory) -> { throw new IOException("disk unavailable"); });
            assertEquals(1, exit);
        } finally {
            System.setErr(original);
        }
        String error = errors.toString(StandardCharsets.UTF_8);
        assertTrue(error.contains("disk unavailable"));
        assertTrue(error.contains(temporaryDirectory.resolve("dashboard.html").toString()));
        assertTrue(error.contains("Grading and CSV exports are complete"));
        assertEquals("existing scores\n", Files.readString(temporaryDirectory.resolve("scorereport.csv")));
        assertEquals("existing cases\n", Files.readString(temporaryDirectory.resolve("cases.csv")));
        assertEquals("existing decisions\n", Files.readString(temporaryDirectory.resolve("submissions.csv")));
    }

    @Test
    void disabledHtmlDoesNotInvokeGenerator() {
        assertEquals(0, App.generateHtmlReport(run(), new HtmlReportConfig(false, "dashboard.html"),
                (context, directory) -> { throw new AssertionError("disabled generator was invoked"); }));
    }

    @Test
    void oneFeedbackFailureDoesNotStopTheRemainingStudents() {
        GradingRun run = run(List.of(grade("first.zip"), grade("second.zip")));
        List<String> written = new ArrayList<>();

        int exitCode = App.generateFeedback(run, new FeedbackConfig(true, "feedback", 10),
                (grade, directory) -> {
                    String archive = grade.getSubmission().getArchivePath().toString();
                    if (archive.equals("first.zip")) {
                        throw new IOException("disk full");
                    }
                    written.add(archive);
                    return directory.resolve(archive + ".md");
                });

        assertEquals(1, exitCode);
        assertEquals(List.of("second.zip"), written);
    }

    @Test
    void disabledFeedbackWritesNothing() {
        GradingRun run = run(List.of(grade("first.zip")));

        int exitCode = App.generateFeedback(run, new FeedbackConfig(false, "feedback", 10),
                (grade, directory) -> {
                    throw new IOException("should not be called");
                });

        assertEquals(0, exitCode);
        assertFalse(Files.exists(temporaryDirectory.resolve("feedback")));
    }

    private GradingRun run() {
        return run(List.of());
    }

    @Test
    void starterFindingsReachHtmlWithoutChangingScoresWithholdingOrExistingCsvs() throws Exception {
        GradingRun original = run(List.of(grade("first.zip"), grade("second.zip")));
        for (String file : List.of("scorereport.csv", "cases.csv", "submissions.csv")) {
            Files.writeString(temporaryDirectory.resolve(file), "original " + file);
        }
        var config = new autograder.config.BoilerplateConfig(true, temporaryDirectory, 0.98, 1024, 100);
        GradingRun analyzed = StarterAnalysis.analyze(original, config, (submission, starter) -> {
            if (submission.getArchivePath().toString().equals("first.zip")) {
                throw new IOException("unreadable starter");
            }
            return List.of(new autograder.model.AnalysisFinding("UNCHANGED_STARTER", Path.of("answer.java"),
                    "second.zip: <unchanged> code; marks unchanged"));
        }).run();
        assertEquals(original.batch().getGrades(), analyzed.batch().getGrades());
        assertEquals(original.withheld(), analyzed.withheld());
        assertEquals(2, analyzed.batch().getFindings().size());
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", analyzed.batch().getFindings().get(0).getCode());
        assertTrue(Files.readString(temporaryDirectory.resolve("starter-analysis.csv")).contains("second.zip"));
        Path html = new autograder.report.html.OfflineHtmlReportGenerator("dashboard.html").generate(
                new autograder.model.GradingReportContext(analyzed.batch(), analyzed.withheld()), temporaryDirectory);
        assertTrue(Files.readString(html).contains("&lt;unchanged&gt;"));
        for (String file : List.of("scorereport.csv", "cases.csv", "submissions.csv")) {
            assertEquals("original " + file, Files.readString(temporaryDirectory.resolve(file)));
        }
        assertEquals(original, StarterAnalysis.analyze(original,
                new autograder.config.BoilerplateConfig(false, temporaryDirectory, 0.98, 1024, 100),
                (submission, starter) -> { throw new AssertionError("disabled detector invoked"); }).run());
    }

    private GradingRun run(List<StudentGrade> grades) {
        QuestionDefinition question = new QuestionDefinition("Q1a", "Q1", "Q1a.java", Path.of("Q1aTester.java"),
                1, 1, List.of(), List.of(), List.of());
        BatchResult batch = new BatchResult(new AssessmentDefinition("exam", "Exam", List.of(question)),
                grades, List.of(), List.of());
        return new GradingRun(batch, temporaryDirectory, Map.of());
    }

    @Test
    void starterCsvFailureStillWritesHtmlAndMarkdownAndReturnsNonzero() throws Exception {
        GradingRun original = run(List.of(grade("first.zip")));
        for (String file : List.of("scorereport.csv", "cases.csv", "submissions.csv", "similarity.csv")) {
            Files.writeString(temporaryDirectory.resolve(file), "original " + file);
        }
        Files.createDirectory(temporaryDirectory.resolve("starter-analysis.csv"));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previous = System.err;
        try (PrintStream capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            int exit = App.generateReports(original,
                    new autograder.config.BoilerplateConfig(true, temporaryDirectory, 0.98, 1024, 100),
                    (submission, starter) -> List.of(new autograder.model.AnalysisFinding(
                            "UNCHANGED_STARTER", Path.of("answer.java"), "first.zip: <unchanged>")),
                    new HtmlReportConfig(true, "dashboard.html"),
                    new autograder.report.html.OfflineHtmlReportGenerator("dashboard.html"),
                    new FeedbackConfig(true, "feedback", 10),
                    new autograder.report.feedback.MarkdownFeedbackGenerator(
                            original.batch().getAssessment(), 2000, 10, "#", "_"));
            assertEquals(1, exit);
        } finally {
            System.setErr(previous);
        }
        String error = errors.toString(StandardCharsets.UTF_8);
        assertTrue(error.contains("could not write starter analysis"));
        assertTrue(error.contains("starter-analysis.csv"));
        assertTrue(error.contains("Grading and CSV exports are complete"));
        String html = Files.readString(temporaryDirectory.resolve("dashboard.html"));
        assertTrue(html.contains("UNCHANGED_STARTER"));
        assertTrue(html.contains("&lt;unchanged&gt;"));
        assertTrue(Files.readString(temporaryDirectory.resolve("feedback/first.md")).contains("Total: 1 / 1"));
        for (String file : List.of("scorereport.csv", "cases.csv", "submissions.csv", "similarity.csv")) {
            assertEquals("original " + file, Files.readString(temporaryDirectory.resolve(file)));
        }
        assertEquals(1, original.batch().getGrades().get(0).getTotalScore());
        assertTrue(original.withheld().isEmpty());
    }

    @Test
    void sanityCsvFailureStillWritesHtmlAndMarkdownAndReturnsNonzero() throws Exception {
        GradingRun original = run(List.of(grade("first.zip")));
        Files.createDirectory(temporaryDirectory.resolve("sanity-checks.csv"));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previous = System.err;
        try (PrintStream capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            int exit = App.generateReports(original,
                    new autograder.config.BoilerplateConfig(false, temporaryDirectory, 0.98, 1024, 100),
                    (submission, starter) -> List.of(),
                    new autograder.config.SanityConfig(true, 1, 90, 80, 50, 40),
                    new HtmlReportConfig(true, "dashboard.html"),
                    new autograder.report.html.OfflineHtmlReportGenerator("dashboard.html"),
                    new FeedbackConfig(true, "feedback", 10),
                    new autograder.report.feedback.MarkdownFeedbackGenerator(
                            original.batch().getAssessment(), 2000, 10, "#", "_"));
            assertEquals(1, exit);
        } finally {
            System.setErr(previous);
        }
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("could not write sanity checks"));
        assertTrue(Files.exists(temporaryDirectory.resolve("dashboard.html")));
        assertTrue(Files.exists(temporaryDirectory.resolve("feedback/first.md")));
    }

    @Test
    void disabledSanityChecksWriteNoFile() {
        SanityAnalysis.Result result = SanityAnalysis.analyze(run(List.of(grade("first.zip"))),
                autograder.config.SanityConfig.disabled());
        assertEquals(0, result.exitCode());
        assertFalse(Files.exists(temporaryDirectory.resolve("sanity-checks.csv")));
    }

    @Test
    void starterAndHtmlFailuresStillAttemptMarkdown() throws Exception {
        GradingRun original = run(List.of(grade("first.zip")));
        Files.createDirectory(temporaryDirectory.resolve("starter-analysis.csv"));
        List<String> attempted = new ArrayList<>();
        int exit = App.generateReports(original,
                new autograder.config.BoilerplateConfig(true, temporaryDirectory, 0.98, 1024, 100),
                (submission, starter) -> List.of(), new HtmlReportConfig(true, "dashboard.html"),
                (context, directory) -> {
                    attempted.add("html");
                    throw new IOException("HTML destination unavailable");
                }, new FeedbackConfig(true, "feedback", 10), (grade, directory) -> {
                    attempted.add("markdown");
                    Files.createDirectories(directory);
                    return Files.writeString(directory.resolve("first.md"), "feedback still generated");
                });
        assertEquals(1, exit);
        assertEquals(List.of("html", "markdown"), attempted);
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("feedback/first.md")));
    }

    @Test
    void existingStarterDestinationIsPreservedAndLaterReportsStillReceiveFindings() throws Exception {
        GradingRun original = run(List.of(grade("first.zip")));
        Path destination = temporaryDirectory.resolve("starter-analysis.csv");
        byte[] scoreBytes = "Username,Score\r\nfirst,1\r\n".getBytes(StandardCharsets.UTF_8);
        Files.write(destination, scoreBytes);
        int exit = App.generateReports(original,
                new autograder.config.BoilerplateConfig(true, temporaryDirectory, 0.98, 1024, 100),
                (submission, starter) -> List.of(new autograder.model.AnalysisFinding(
                        "UNCHANGED_STARTER", Path.of("answer.java"), "first.zip: original starter")),
                new HtmlReportConfig(true, "dashboard.html"),
                new autograder.report.html.OfflineHtmlReportGenerator("dashboard.html"),
                new FeedbackConfig(true, "feedback", 10),
                new autograder.report.feedback.MarkdownFeedbackGenerator(
                        original.batch().getAssessment(), 2000, 10, "#", "_"));
        assertEquals(1, exit);
        assertArrayEquals(scoreBytes, Files.readAllBytes(destination));
        assertTrue(Files.readString(temporaryDirectory.resolve("dashboard.html")).contains("UNCHANGED_STARTER"));
        assertTrue(Files.readString(temporaryDirectory.resolve("feedback/first.md")).contains("Total: 1 / 1"));
        assertEquals(1, original.batch().getGrades().get(0).getTotalScore());
    }

    @Test
    void disabledStarterAnalysisDoesNotWriteAReportOrInvokeTheDetector() {
        StarterAnalysis.Result result = StarterAnalysis.analyze(run(List.of(grade("first.zip"))),
                new autograder.config.BoilerplateConfig(false, temporaryDirectory, 0.98, 1024, 100),
                (submission, starter) -> { throw new AssertionError("disabled detector invoked"); });
        assertEquals(0, result.exitCode());
        assertFalse(Files.exists(temporaryDirectory.resolve("starter-analysis.csv")));
        assertFalse(Files.exists(temporaryDirectory.resolve("starter-comparison")));
    }

    @Test
    void methodReportFailurePreservesCsvAndStillAttemptsOtherReports() throws Exception {
        GradingRun original = run(List.of(grade("first.zip")));
        Files.writeString(temporaryDirectory.resolve("starter-comparison"), "keep this file");
        Files.writeString(temporaryDirectory.resolve("scorereport.csv"), "keep scores");
        List<String> attempted = new ArrayList<>();
        int exit = App.generateReports(original,
                new autograder.config.BoilerplateConfig(true, temporaryDirectory, 0.98, 1024, 100),
                (submission, starter) -> List.of(), new HtmlReportConfig(true, "dashboard.html"),
                (context, destination) -> {
                    attempted.add("html");
                    return destination.resolve("dashboard.html");
                }, new FeedbackConfig(true, "feedback", 10), (grade, destination) -> {
                    attempted.add("markdown");
                    return destination.resolve("first.md");
                });
        assertEquals(1, exit);
        assertEquals(List.of("html", "markdown"), attempted);
        assertEquals("keep scores", Files.readString(temporaryDirectory.resolve("scorereport.csv")));
        assertEquals("keep this file", Files.readString(temporaryDirectory.resolve("starter-comparison")));
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("starter-analysis.csv")));
        assertEquals(1, original.batch().getGrades().get(0).getTotalScore());
    }

    @Test
    void missingConfigurationFileReturnsOneWithoutThrowing() {
        assertEquals(1, App.run(new String[] {temporaryDirectory.resolve("absent.properties").toString()}));
    }

    private StudentGrade grade(String archive) {
        StudentIdentity identity = new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                null, "test fixture", List.of());
        SubmissionResult submission = new SubmissionResult(Path.of(archive), null, null, null, identity,
                List.of(), List.of(), List.of(), SubmissionStatus.READY, true);
        TestCaseDefinition test = new TestCaseDefinition("Q1a-1", "Q1a", "Runner", List.of(), 1);
        return new StudentGrade(submission, List.of(new TestCaseResult(test, TestStatus.PASSED, 1, "", "")));
    }
}
