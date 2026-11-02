package autograder.gui;

import autograder.config.WorkerLimits;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/** Decides the worker options and when the selection follows the config. */
final class WorkerChoice {
    private final WorkerLimits limits;
    private Integer lastConfigured;

    WorkerChoice(WorkerLimits limits) {
        this.limits = limits;
    }

    /** What the window should do after settings load. */
    record Update(OptionalInt selection, Optional<String> warning) {
    }

    List<Integer> options() {
        return IntStream.rangeClosed(1, limits.processors()).boxed().toList();
    }

    // selection is only reset when the configured value differs from the previous load
    Update settingsLoaded(int configured) {
        WorkerLimits.Capped capped = limits.cap(configured);
        boolean changed = lastConfigured == null || lastConfigured != configured;
        lastConfigured = configured;
        OptionalInt selection = changed ? OptionalInt.of(capped.workers()) : OptionalInt.empty();
        return new Update(selection, capped.warning());
    }
}
