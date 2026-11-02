package autograder.gui;

import autograder.RunOutcome;
import autograder.RunOutcome.Stage;
import autograder.RunOutcome.Status;
import org.junit.jupiter.api.Test;

import java.nio.channels.ClosedByInterruptException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutcomeTextTest {
    private static RunOutcome outcome(Status status, Stage stage, Exception failure) {
        return new RunOutcome(status, stage, failure, List.of(), null, 0);
    }

    @Test
    void headlinesForNonFailureStatuses() {
        assertEquals("Grading complete", OutcomeText.headline(outcome(Status.COMPLETED, null, null)));
        assertEquals("Grading complete. Some reports could not be written.",
                OutcomeText.headline(outcome(Status.REPORT_FAILURES, null, null)));
        assertEquals("No submissions matched",
                OutcomeText.headline(outcome(Status.NO_SUBMISSIONS, null, null)));
        assertEquals("Grading stopped. No scores were written.",
                OutcomeText.headline(outcome(Status.INTERRUPTED, null, null)));
    }

    @Test
    void failureNamesStageAndMessage() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.PREPARATION,
                new IllegalStateException("bad zip")));
        assertEquals("Grading stopped while preparing submissions: bad zip", text);
    }

    @Test
    void failureWithoutMessageUsesExceptionName() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.REPORTS,
                new IllegalStateException()));
        assertEquals("Grading stopped while writing reports: IllegalStateException", text);
    }

    @Test
    void environmentFailureAddsHint() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.ENVIRONMENT,
                new IllegalStateException("no compiler")));
        assertTrue(text.startsWith("Grading stopped while checking the setup: no compiler"));
        assertTrue(text.contains("JDK"));
    }

    @Test
    void everyStageUsesItsFriendlyLabel() {
        assertStage(Stage.ENVIRONMENT, "checking the setup");
        assertStage(Stage.PREPARATION, "preparing submissions");
        assertStage(Stage.GRADING, "grading");
        assertStage(Stage.REPORTS, "writing reports");
    }

    private static void assertStage(Stage stage, String label) {
        String text = OutcomeText.headline(outcome(Status.FAILED, stage, new IllegalStateException("x")));
        assertTrue(text.startsWith("Grading stopped while " + label + ": x"));
        assertFalse(text.contains(stage.name()));
    }

    @Test
    void closedByInterruptNeverShowsItsClassName() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.GRADING,
                new IllegalStateException("wrapped", new ClosedByInterruptException())));
        assertFalse(text.contains("ClosedByInterruptException"));
        assertTrue(text.contains("the run was stopped"));
        assertFalse(OutcomeText.headline(outcome(Status.FAILED, Stage.GRADING,
                new ClosedByInterruptException())).contains("ClosedByInterruptException"));
    }

    @Test
    void stoppedHeadlineIsAccurateForLateStages() {
        assertEquals("Grading stopped. No scores were written.",
                OutcomeText.headline(outcome(Status.INTERRUPTED, Stage.PREPARATION, null)));
        String late = OutcomeText.headline(outcome(Status.INTERRUPTED, Stage.REPORTS, null));
        assertTrue(late.contains("writing reports"));
        assertTrue(late.contains("Scores may have been written"));
        assertFalse(late.contains("No scores were written"));
        assertFalse(OutcomeText.headline(outcome(Status.INTERRUPTED, Stage.GRADING, null))
                .contains("No scores were written"));
    }

    @Test
    void reportProblemsListsOneLinePerMessageAndIsEmptyWhenNone() {
        assertEquals("", OutcomeText.reportProblems(List.of()));
        assertEquals("a failed\nb failed",
                OutcomeText.reportProblems(java.util.Arrays.asList("a failed ", null, " ", "b failed")));
    }

    @Test
    void progressStrings() {
        assertEquals("Preparing 2 of 6: chee.zip", OutcomeText.intake("chee.zip", 2, 6));
        assertEquals("Grading 3 of 6: Chee", OutcomeText.grading("Chee", 3, 6));
        assertEquals("Grading 3 of 6", OutcomeText.grading("", 3, 6));
        assertEquals("Checking setup…", OutcomeText.CHECKING);
        assertEquals("Writing reports…", OutcomeText.WRITING_REPORTS);
    }
}
