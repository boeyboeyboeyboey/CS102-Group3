package autograder.grading;

import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.csv.CsvCodec;
import autograder.model.ScoreSheet;
import autograder.model.SimilarityFinding;
import autograder.model.StudentGrade;
import autograder.model.StudentRecord;
import autograder.model.SubmissionResult;
import autograder.model.TestCaseResult;
import autograder.report.CsvScoreReportWriter;
import autograder.roster.ScoreSheetRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class GradingDiagnosticsWriter {
    private final AppConfig app;
    private final ExecutionConfig execution;

    GradingDiagnosticsWriter(AppConfig app, ExecutionConfig execution) {
        this.app = app;
        this.execution = execution;
    }

    void write(GradingRun run) throws IOException {
        List<List<String>> caseRows = new ArrayList<>();
        caseRows.add(List.of("Archive", "Username", "Case", "Status", "Score", "Maximum",
                "DurationMillis", "Stdout", "Stderr"));
        List<List<String>> submissionRows = new ArrayList<>();
        submissionRows.add(List.of("Archive", "Username", "Export", "Reason", "ComputedScore",
                "PreparationWarnings"));
        Map<String, Double> scores = new LinkedHashMap<>();
        for (StudentGrade grade : run.batch().getGrades()) {
            SubmissionResult submission = grade.getSubmission();
            StudentRecord student = submission.getIdentity().getStudentRecord();
            String username = student == null ? "" : student.getUsername();
            String archive = submission.getArchivePath().getFileName().toString();
            String reason = run.withheld().get(submission);
            if (reason == null) {
                scores.put(student.getId(), grade.getTotalScore());
            }
            submissionRows.add(List.of(archive, username, reason == null ? "EXPORTED" : "WITHHELD",
                    reason == null ? "" : reason, Double.toString(grade.getTotalScore()),
                    String.join("\n", submission.getAnomalies())));
            for (TestCaseResult result : grade.getTestResults()) {
                caseRows.add(List.of(archive, username, result.getTestCase().getId(),
                        result.getStatus().name(), Double.toString(result.getScore()),
                        Double.toString(result.getTestCase().getMaximumScore()),
                        Long.toString(result.getDurationMillis()), result.getStandardOutput(),
                        result.getStandardError()));
            }
        }
        Files.writeString(run.directory().resolve("cases.csv"), CsvCodec.write(caseRows));
        Files.writeString(run.directory().resolve("submissions.csv"), CsvCodec.write(submissionRows));
        Files.writeString(run.directory().resolve(GradingPipeline.SIMILARITY_REPORT),
                CsvCodec.write(similarityRows(run)));
        ScoreSheet sheet = new ScoreSheetRepository(app.getScoreSheetPath(),
                app.getScoreSheetColumns()).load();
        new CsvScoreReportWriter().write(sheet, scores, run.directory().resolve(execution.reportFile()));
    }

    private List<List<String>> similarityRows(GradingRun run) {
        Map<Path, String> usernames = new LinkedHashMap<>();
        for (StudentGrade grade : run.batch().getGrades()) {
            SubmissionResult submission = grade.getSubmission();
            StudentRecord student = submission.getIdentity().getStudentRecord();
            usernames.put(submission.getArchivePath(), student == null ? "" : student.getUsername());
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Question", "Algorithm", "FirstArchive", "FirstUsername", "SecondArchive",
                "SecondUsername", "Similarity", "Evidence"));
        for (SimilarityFinding finding : run.batch().getSimilarities()) {
            rows.add(List.of(finding.getQuestion(), finding.getAlgorithm(),
                    finding.getFirstArchive().getFileName().toString(),
                    usernames.getOrDefault(finding.getFirstArchive(), ""),
                    finding.getSecondArchive().getFileName().toString(),
                    usernames.getOrDefault(finding.getSecondArchive(), ""),
                    String.format(Locale.ROOT, "%.3f", finding.getSimilarity()),
                    String.join("\n", finding.getEvidence())));
        }
        return rows;
    }
}
