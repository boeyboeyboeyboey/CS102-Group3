package autograder.autotester;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record RandomizedConfig(Path resourcesDirectory, Path resultsDirectory,
                               String javaCommand, String javacCommand,
                               int runsPerCase, long timeoutMillis,
                               int maxFailuresShown, int examplesPerCase,
                               int maxOutputBytes) {
    public RandomizedConfig {
        if (resourcesDirectory == null || resultsDirectory == null
                || javaCommand == null || javaCommand.isBlank()
                || javacCommand == null || javacCommand.isBlank()
                || runsPerCase < 1 || runsPerCase > 100_000
                || timeoutMillis < 1_000 || timeoutMillis > 3_600_000
                || maxFailuresShown < 0 || maxFailuresShown > 10_000
                || examplesPerCase < 1 || examplesPerCase > 100
                || maxOutputBytes < 1_024 || maxOutputBytes > 64 * 1_024 * 1_024) {
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
                resolveCommand(base, required(properties, "randomized.java.command", "java")),
                resolveCommand(base, required(properties, "randomized.javac.command", "javac")),
                integer(properties, "randomized.runs.per.case", 50, 1, 100_000),
                seconds(properties, "randomized.timeout.seconds", 10, 1, 3_600),
                integer(properties, "randomized.max.failures.shown", 10, 0, 10_000),
                integer(properties, "randomized.examples.per.case", 2, 1, 100),
                integer(properties, "randomized.max.output.bytes", 1_048_576, 1_024, 64 * 1_024 * 1_024));
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
        if (path.isAbsolute() || (!command.contains("/") && !command.contains("\\"))) return command;
        return base.resolve(path).normalize().toString();
    }

    private static String required(Properties properties, String name, String fallback) {
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
            return Math.multiplyExact(parsed, 1_000L);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be a valid number of seconds", exception);
        }
    }
}
