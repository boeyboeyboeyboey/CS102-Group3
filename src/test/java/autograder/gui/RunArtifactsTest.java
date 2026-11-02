package autograder.gui;

import autograder.RunOutcome;
import autograder.config.RunSettings;
import autograder.grading.GradingRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunArtifactsTest {
    @TempDir
    Path root;

    @Test
    void nullRunGivesNoArtifacts() throws Exception {
        RunSettings settings = new ReadinessChecker().check(new TestSetup(root).inputs()).settings();
        RunOutcome outcome = new RunOutcome(RunOutcome.Status.NO_SUBMISSIONS, null, null,
                List.of(), null, 1);

        assertTrue(RunArtifacts.from(outcome, settings).isEmpty());
    }

    @Test
    void listsOnlyExistingFilesInDisplayOrder() throws Exception {
        RunSettings settings = new ReadinessChecker().check(new TestSetup(root).inputs()).settings();
        Path run = Files.createDirectories(root.resolve("run"));
        Files.writeString(run.resolve("cases.csv"), "x");
        Files.writeString(run.resolve(settings.html().file()), "x");
        Files.writeString(run.resolve(settings.execution().reportFile()), "x");
        Files.createDirectories(run.resolve(settings.feedback().directory()));
        GradingRun gradingRun = new GradingRun(null, run, Map.of());
        RunOutcome outcome = new RunOutcome(RunOutcome.Status.COMPLETED, null, null,
                List.of(), gradingRun, 0);

        List<String> labels = RunArtifacts.from(outcome, settings).stream().map(Artifact::label).toList();

        assertEquals(List.of("Dashboard", "Scores", "Case results", "Feedback", "Run folder"), labels);
    }
}
