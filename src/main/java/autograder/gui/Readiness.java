package autograder.gui;

import autograder.config.RunSettings;

import java.util.List;
import java.util.Optional;

/** Outcome of checking all inputs; settings is non-null only when ready. */
public record Readiness(List<FieldStatus> statuses, RunSettings settings) {
    public Readiness {
        statuses = List.copyOf(statuses);
    }

    public boolean ready() {
        return settings != null;
    }

    public Optional<FieldStatus> status(InputField field) {
        return statuses.stream().filter(status -> status.field() == field).findFirst();
    }
}
