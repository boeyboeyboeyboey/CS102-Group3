package autograder.gui;

import autograder.RunOutcome;
import autograder.config.RunSettings;
import autograder.grading.GradingPipeline;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lists the outputs of a run that exist on disk, in display order. */
public final class RunArtifacts {
    static final String CASES_FILE = "cases.csv";
    static final String SUBMISSIONS_FILE = "submissions.csv";
    static final String STARTER_FILE = "starter-analysis.csv";
    static final String SANITY_FILE = "sanity-checks.csv";

    private RunArtifacts() {
    }

    public static List<Artifact> from(RunOutcome outcome, RunSettings settings) {
        List<Artifact> artifacts = new ArrayList<>();
        if (outcome == null || outcome.run() == null) {
            return artifacts;
        }
        Path directory = outcome.run().directory();
        add(artifacts, "Dashboard", directory.resolve(settings.html().file()));
        add(artifacts, "Scores", directory.resolve(settings.execution().reportFile()));
        add(artifacts, "Case results", directory.resolve(CASES_FILE));
        add(artifacts, "Submission decisions", directory.resolve(SUBMISSIONS_FILE));
        add(artifacts, "Similarity", directory.resolve(GradingPipeline.SIMILARITY_REPORT));
        add(artifacts, "Starter code", directory.resolve(STARTER_FILE));
        add(artifacts, "Sanity checks", directory.resolve(SANITY_FILE));
        add(artifacts, "Feedback", directory.resolve(settings.feedback().directory()));
        add(artifacts, "Run folder", directory);
        return artifacts;
    }

    private static void add(List<Artifact> artifacts, String label, Path path) {
        if (Files.exists(path)) {
            artifacts.add(new Artifact(label, path));
        }
    }
}
