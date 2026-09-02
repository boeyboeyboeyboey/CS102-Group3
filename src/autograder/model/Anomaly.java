package autograder.model;

import java.util.Objects;

/** A structured diagnostic discovered while processing a submission. */
public final class Anomaly {
    private final String code;
    private final Severity severity;
    private final String message;

    public Anomaly(String code, Severity severity, String message) {
        this.code = Objects.requireNonNull(code);
        this.severity = Objects.requireNonNull(severity);
        this.message = Objects.requireNonNull(message);
    }

    public String getCode() {
        return code;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + code + ": " + message;
    }
}
