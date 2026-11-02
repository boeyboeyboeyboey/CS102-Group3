package autograder;

import autograder.config.AppConfig;
import autograder.config.PlagiarismConfig;
import autograder.config.RunSettings;
import autograder.grading.GradingPipeline;
import autograder.grading.GradingRun;
import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.ui.ConsoleProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConsoleRunListener implements RunListener {
    private final ConsoleProgressListener intake = new ConsoleProgressListener();

    @Override
    public void onProgress(String stage, String item, int completed, int total) {
        intake.onProgress(stage, item, completed, total);
    }

    @Override
    public void preparing(String assessmentName) {
        System.out.println("Preparing submissions for " + assessmentName);
    }

    @Override
    public void prepared(List<SubmissionResult> results, AppConfig app) {
        printResults(results);
        System.out.println("Diagnostics: " + app.getAnomalyLog());
        System.out.println("Corrections: " + app.getCorrectionLog());
    }

    @Override
    public void graded(GradingRun run, RunSettings settings) {
        for (StudentGrade grade : run.batch().getGrades()) {
            String reason = run.withheld().get(grade.getSubmission());
            System.out.println(grade.getSubmission().getIdentity().getLabel() + " | "
                    + (reason == null ? "Score: " + grade.getTotalScore()
                    : "WITHHELD: " + reason));
        }
        System.out.println("Score report: " + run.directory().resolve(settings.execution().reportFile()));
        System.out.println("Per-case diagnostics: " + run.directory().resolve("cases.csv"));
        System.out.println("Export decisions and review warnings: "
                + run.directory().resolve("submissions.csv"));
        System.out.println("Withheld or absent students retain their original score-sheet rows");
        printSimilarity(run, settings.plagiarism());
    }

    private static void printResults(List<SubmissionResult> results) {
        int ready = 0;
        int reviewRequired = 0;
        int failed = 0;
        System.out.println("Prepared " + results.size() + " submission archives");
        for (SubmissionResult result : results) {
            if (result.getStatus() == SubmissionStatus.READY) {
                ready++;
            } else if (result.getStatus() == SubmissionStatus.REVIEW_REQUIRED) {
                reviewRequired++;
            } else {
                failed++;
            }
            String identityLabel = result.getIdentity().getLabel();
            if (result.getIdentity().getStudentRecord() != null) {
                identityLabel = result.getIdentity().getStudentRecord().getUsername();
            }
            System.out.println(result.getArchivePath().getFileName() + " | "
                    + identityLabel + " | " + result.getStatus()
                    + " | issues=" + result.getAnomalies().size()
                    + " | corrections=" + result.getCorrections().size());
            if (result.getWorkingRoot() != null) {
                System.out.println("  Working copy: " + result.getWorkingRoot());
            }
        }
        System.out.println("Ready: " + ready + "; needs review: " + reviewRequired
                + "; failed: " + failed);
        System.out.println("READY means ready for the next stage, not a passing grade");
    }

    private static void printSimilarity(GradingRun run, PlagiarismConfig plagiarism) {
        if (!plagiarism.enabled()) {
            System.out.println("Similarity check: disabled in configuration");
            return;
        }
        List<String> thresholds = new ArrayList<>();
        if (plagiarism.uses(PlagiarismConfig.NGRAM)) {
            thresholds.add(String.format(Locale.ROOT, "n-gram >= %.2f", plagiarism.threshold()));
        }
        if (plagiarism.uses(PlagiarismConfig.JPLAG)) {
            thresholds.add(String.format(Locale.ROOT, "JPlag >= %.2f", plagiarism.jplagThreshold()));
        }
        int pairs = run.batch().getSimilarities().size();
        System.out.println("Similarity review: " + pairs + " finding(s) (" + String.join(", ", thresholds)
                + "), starter code excluded: " + run.directory().resolve(GradingPipeline.SIMILARITY_REPORT));
        if (pairs > 0) {
            System.out.println("Similarity needs instructor judgement and is not proof of copying");
        }
    }
}
