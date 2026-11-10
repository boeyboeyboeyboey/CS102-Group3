package autograder.gui;

import autograder.RunOutcome;
import autograder.config.RunSettings;
import autograder.report.ReportFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lists the outputs of a run that exist on disk, in display order. */
public final class RunArtifacts {
    private RunArtifacts() {
    }

    public static List<Artifact> from(RunOutcome outcome, RunSettings settings) {
        List<Artifact> artifacts = new ArrayList<>();
        if (outcome == null || outcome.run() == null) {
            return artifacts;
        }
        Path directory = outcome.run().directory();
        add(artifacts, "Dashboard", directory.resolve(settings.html().file()), Artifact.View.EXTERNAL);
        add(artifacts, "Scores", directory.resolve(settings.execution().reportFile()), Artifact.View.TABLE);
        add(artifacts, "Case results", directory.resolve(ReportFiles.CASES), Artifact.View.TABLE);
        add(artifacts, "Submission decisions", directory.resolve(ReportFiles.SUBMISSIONS), Artifact.View.TABLE);
        add(artifacts, "Similarity", directory.resolve(ReportFiles.SIMILARITY), Artifact.View.TABLE);
        add(artifacts, "Starter code", directory.resolve(ReportFiles.STARTER_ANALYSIS), Artifact.View.TABLE);
        add(artifacts, "Sanity checks", directory.resolve(ReportFiles.SANITY_CHECKS), Artifact.View.TABLE);
        add(artifacts, "Feedback", directory.resolve(settings.feedback().directory()), Artifact.View.FEEDBACK);
        add(artifacts, "Run folder", directory, Artifact.View.EXTERNAL);
        return artifacts;
    }

    private static void add(List<Artifact> artifacts, String label, Path path, Artifact.View view) {
        if (Files.exists(path)) {
            artifacts.add(new Artifact(label, path, view));
        }
    }
}
