package autograder.gui;

import autograder.config.WorkerLimits;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerChoiceTest {
    @Test
    void optionsRunFromOneToProcessorCount() {
        assertEquals(List.of(1, 2, 3, 4), new WorkerChoice(new WorkerLimits(() -> 4)).options());
        assertEquals(List.of(1), new WorkerChoice(new WorkerLimits(() -> 0)).options());
    }

    @Test
    void firstLoadSelectsConfiguredValueWithoutWarning() {
        WorkerChoice choice = new WorkerChoice(new WorkerLimits(() -> 8));
        WorkerChoice.Update update = choice.settingsLoaded(2);
        assertEquals(2, update.selection().getAsInt());
        assertTrue(update.warning().isEmpty());
    }

    @Test
    void cappedValueIsSelectedWithWarning() {
        WorkerChoice choice = new WorkerChoice(new WorkerLimits(() -> 4));
        WorkerChoice.Update update = choice.settingsLoaded(8);
        assertEquals(4, update.selection().getAsInt());
        assertEquals("execution.workers=8 exceeds 4 available processors; using 4",
                update.warning().orElseThrow());
    }

    @Test
    void unchangedConfigValueKeepsUserSelection() {
        WorkerChoice choice = new WorkerChoice(new WorkerLimits(() -> 8));
        choice.settingsLoaded(2);
        assertTrue(choice.settingsLoaded(2).selection().isEmpty());
        assertEquals(6, choice.settingsLoaded(6).selection().getAsInt());
        assertTrue(choice.settingsLoaded(6).selection().isEmpty());
    }

    @Test
    void warningRepeatsWhileSelectionIsKept() {
        WorkerChoice choice = new WorkerChoice(new WorkerLimits(() -> 2));
        choice.settingsLoaded(5);
        WorkerChoice.Update again = choice.settingsLoaded(5);
        assertTrue(again.selection().isEmpty());
        assertTrue(again.warning().isPresent());
    }
}
