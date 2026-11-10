package autograder.autotester.execution;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Validated settings for the randomized grader, loaded from a properties file. */
public record RandomizedConfig(Path resourcesDirectory, Path resultsDirectory,
                               String javaCommand, String javacCommand,
                               int runsPerCase, long timeoutMillis,
                               int maxFailuresShown, int examplesPerCase,
                               int maxOutputBytes) {
    private static final int MIN_RUNS_PER_CASE = 1;
    private static final int MAX_RUNS_PER_CASE = 100_000;
    private static final int DEFAULT_RUNS_PER_CASE = 50;
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long MIN_TIMEOUT_SECONDS = 1;
    private static final long MAX_TIMEOUT_SECONDS = 3_600;
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int MIN_MAX_FAILURES_SHOWN = 0;
    private static final int MAX_MAX_FAILURES_SHOWN = 10_000;
    private static final int DEFAULT_MAX_FAILURES_SHOWN = 10;
    private static final int MIN_EXAMPLES_PER_CASE = 1;
    private static final int MAX_EXAMPLES_PER_CASE = 100;
    private static final int DEFAULT_EXAMPLES_PER_CASE = 2;
    private static final int MIN_OUTPUT_BYTES = 1_024;
    private static final int MAX_OUTPUT_BYTES = 64 * 1_024 * 1_024;
    private static final int DEFAULT_OUTPUT_BYTES = 1_048_576;

    public RandomizedConfig {
        if (resourcesDirectory == null || resultsDirectory == null
                || javaCommand == null || javaCommand.isBlank()
                || javacCommand == null || javacCommand.isBlank()
                || runsPerCase < MIN_RUNS_PER_CASE || runsPerCase > MAX_RUNS_PER_CASE
                || timeoutMillis < MIN_TIMEOUT_SECONDS * MILLIS_PER_SECOND
                || timeoutMillis > MAX_TIMEOUT_SECONDS * MILLIS_PER_SECOND
                || maxFailuresShown < MIN_MAX_FAILURES_SHOWN || maxFailuresShown > MAX_MAX_FAILURES_SHOWN
                || examplesPerCase < MIN_EXAMPLES_PER_CASE || examplesPerCase > MAX_EXAMPLES_PER_CASE
                || maxOutputBytes < MIN_OUTPUT_BYTES || maxOutputBytes > MAX_OUTPUT_BYTES) {
            throw new IllegalArgumentException("invalid randomized autograder configuration");
        }
    }

    public static RandomizedConfig load(Path file) throws IOException {
        Path absoluteFile = file.toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(absoluteFile)) {
            properties.load(input);
        }
        Path base = absoluteFile.getParent();
        return new RandomizedConfig(
                resolve(base, properties.getProperty("randomized.resources.directory", "students/resources")),
                resolve(base, properties.getProperty("randomized.results.directory", "grading/results")),
                resolveCommand(base, nonBlankProperty(properties, "randomized.java.command", "java")),
                resolveCommand(base, nonBlankProperty(properties, "randomized.javac.command", "javac")),
                integer(properties, "randomized.runs.per.case",
                        DEFAULT_RUNS_PER_CASE, MIN_RUNS_PER_CASE, MAX_RUNS_PER_CASE),
                seconds(properties, "randomized.timeout.seconds",
                        DEFAULT_TIMEOUT_SECONDS, MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS),
                integer(properties, "randomized.max.failures.shown",
                        DEFAULT_MAX_FAILURES_SHOWN, MIN_MAX_FAILURES_SHOWN, MAX_MAX_FAILURES_SHOWN),
                integer(properties, "randomized.examples.per.case",
                        DEFAULT_EXAMPLES_PER_CASE, MIN_EXAMPLES_PER_CASE, MAX_EXAMPLES_PER_CASE),
                integer(properties, "randomized.max.output.bytes",
                        DEFAULT_OUTPUT_BYTES, MIN_OUTPUT_BYTES, MAX_OUTPUT_BYTES));
    }

    private static Path resolve(Path base, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("randomized directory paths must not be blank");
        }
        Path path = Path.of(value);
        return (path.isAbsolute() ? path : base.resolve(path)).normalize();
    }

    private static String resolveCommand(Path base, String command) {
        Path path = Path.of(command);
        if (path.isAbsolute() || (!command.contains("/") && !command.contains("\\"))) {
            return command;
        }
        return base.resolve(path).normalize().toString();
    }

    private static String nonBlankProperty(Properties properties, String name, String fallback) {
        String value = properties.getProperty(name, fallback).strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static int integer(Properties properties, String name, int fallback, int minimum, int maximum) {
        String value = properties.getProperty(name, Integer.toString(fallback)).strip();
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private static long seconds(Properties properties, String name, long fallback, long minimum, long maximum) {
        String value = properties.getProperty(name, Long.toString(fallback)).strip();
        try {
            long parsed = Long.parseLong(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
            }
            return Math.multiplyExact(parsed, MILLIS_PER_SECOND);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be a valid number of seconds", exception);
        }
    }
}
