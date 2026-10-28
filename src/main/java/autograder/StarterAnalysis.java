package autograder;

import autograder.api.BoilerplateDetector;
import autograder.analysis.boilerplate.StarterComparisonReport;
import autograder.config.BoilerplateConfig;
import autograder.csv.CsvCodec;
import autograder.grading.GradingRun;
import autograder.model.AnalysisFinding;
import autograder.model.BatchResult;
import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

final class StarterAnalysis {
    private StarterAnalysis() {
    }

    record Result(GradingRun run, int exitCode) {
    }

    static Result analyze(GradingRun run, BoilerplateConfig config, BoilerplateDetector detector) {
        if (!config.enabled()) {
            return new Result(run, 0);
        }
        List<AnalysisFinding> findings = new ArrayList<>(run.batch().getFindings());
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Archive", "SourceFile", "Code", "Message"));
        for (StudentGrade grade : run.batch().getGrades()) {
            SubmissionResult submission = grade.getSubmission();
            List<AnalysisFinding> detected = compare(submission, config, detector);
            findings.addAll(detected);
            for (AnalysisFinding finding : detected) {
                rows.add(List.of(submission.getArchivePath().getFileName().toString(),
                        finding.getSourceFile().toString(), finding.getCode(), finding.getMessage()));
                System.out.println("Starter review: " + finding.getCode() + " | " + finding.getMessage());
            }
        }
        BatchResult batch = new BatchResult(run.batch().getAssessment(), run.batch().getGrades(),
                findings, run.batch().getSimilarities());
        GradingRun analyzed = new GradingRun(batch, run.directory(), run.withheld());
        int csvExit = writeReport(run.directory(), rows);
        int comparisonExit = writeMethodReport(analyzed, config);
        return new Result(analyzed, Math.max(csvExit, comparisonExit));
    }

    private static int writeMethodReport(GradingRun run, BoilerplateConfig config) {
        try {
            Path report = StarterComparisonReport.generate(run.batch(), config, run.directory());
            System.out.println("Method comparison report: " + report);
            return 0;
        } catch (IOException | RuntimeException exception) {
            System.err.println("could not write method comparison report: " + exception.getMessage());
            System.err.println("Continuing with HTML and Markdown reports; marks unchanged");
            return 1;
        }
    }

    private static List<AnalysisFinding> compare(SubmissionResult submission, BoilerplateConfig config,
                                                BoilerplateDetector detector) {
        try {
            return detector.analyze(submission, config.starterDirectory());
        } catch (IOException | RuntimeException exception) {
            return List.of(new AnalysisFinding("STARTER_COMPARISON_UNAVAILABLE", submission.getArchivePath(),
                    "Comparison failed: " + exception.getMessage()));
        }
    }

    private static int writeReport(Path directory, List<List<String>> rows) {
        Path report = directory.resolve("starter-analysis.csv");
        try {
            Files.writeString(report, CsvCodec.write(rows),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            System.out.println("Starter comparison: " + (rows.size() - 1) + " findings; " + report);
            return 0;
        } catch (IOException | RuntimeException exception) {
            System.err.println("could not write starter analysis " + report + ": " + exception.getMessage());
            System.err.println("Grading and CSV exports are complete in " + directory
                    + "; continuing with HTML and Markdown reports");
            return 1;
        }
    }
}
