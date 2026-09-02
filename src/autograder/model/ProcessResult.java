package autograder.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Captured outcome of an external compiler or Java process. */
public final class ProcessResult {
    private final List<String> command;
    private final int exitCode;
    private final boolean timedOut;
    private final boolean outputTruncated;
    private final String standardOutput;
    private final String standardError;
    private final Duration duration;

    public ProcessResult(List<String> command, int exitCode, boolean timedOut,
            boolean outputTruncated, String standardOutput, String standardError,
            Duration duration) {
        this.command = Collections.unmodifiableList(new ArrayList<>(command));
        this.exitCode = exitCode;
        this.timedOut = timedOut;
        this.outputTruncated = outputTruncated;
        this.standardOutput = standardOutput;
        this.standardError = standardError;
        this.duration = duration;
    }

    public List<String> getCommand() {
        return command;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public boolean isOutputTruncated() {
        return outputTruncated;
    }

    public String getStandardOutput() {
        return standardOutput;
    }

    public String getStandardError() {
        return standardError;
    }

    public Duration getDuration() {
        return duration;
    }
}
