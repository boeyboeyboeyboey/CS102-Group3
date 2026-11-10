package autograder.execution;

/** Outcome of one child process: exit code, timeout flag, duration and captured output. */
public record ProcessResult(int exitCode, boolean timedOut, long durationMillis,
                           String stdout, String stderr) {
}
