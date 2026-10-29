package autograder;

import autograder.config.AppConfig;
import autograder.config.RunSettings;
import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;
import autograder.ui.TerminalDashboard;

import java.util.List;

/** routes service events to the console and terminal dashboard */
final class DashboardRunListener implements RunListener {
    private final RunListener console;
    private final TerminalDashboard dashboard;

    DashboardRunListener(RunListener console, TerminalDashboard dashboard) {
        this.console = console;
        this.dashboard = dashboard;
    }

    @Override
    public void onProgress(String stage, String item, int completed, int total) {
        dashboard.onProgress(stage, item, completed, total);
    }

    @Override
    public void preparing(String assessmentName) {
        console.preparing(assessmentName);
    }

    @Override
    public void prepared(List<SubmissionResult> results, AppConfig app) {
        console.prepared(results, app);
    }

    @Override
    public void gradingProgress(String item, int completed, int total) {
        console.gradingProgress(item, completed, total);
        dashboard.onProgress("grading", item, completed, total);
    }

    @Override
    public void reportingProgress(String item, int completed, int total) {
        console.reportingProgress(item, completed, total);
        dashboard.onProgress("reporting", item, completed, total);
    }

    @Override
    public void graded(GradingRun run, RunSettings settings) {
        console.graded(run, settings);
    }

    @Override
    public void reportFailed(String message) {
        console.reportFailed(message);
    }
}
