package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record FeedbackConfig(boolean enabled, String directory, int maxDiagnosticLines) {
    private static final String DEFAULT_DIRECTORY = "feedback";
    private static final String DEFAULT_MAX_DIAGNOSTIC_LINES = "40";
    private static final String RESERVED_DIRECTORY_PREFIX = "submission-";

    public FeedbackConfig {
        if (directory == null || !directory.matches("[A-Za-z0-9][A-Za-z0-9._-]*")
                || directory.startsWith(RESERVED_DIRECTORY_PREFIX)) {
            throw new IllegalArgumentException("grading.feedback.directory must be a simple folder name "
                    + "using letters, digits, dots, underscores or hyphens and must not start with "
                    + RESERVED_DIRECTORY_PREFIX);
        }
        if (maxDiagnosticLines <= 0) {
            throw new IllegalArgumentException("grading.feedback.max.diagnostic.lines must be positive");
        }
    }

    public static FeedbackConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        Path baseDirectory = file.toAbsolutePath().normalize().getParent();
        return load(properties, baseDirectory);
    }

    static FeedbackConfig load(Properties properties, Path baseDirectory)
            throws IOException {
        String enabled = properties.getProperty("grading.feedback.enabled", "false").trim();
        if (!enabled.equalsIgnoreCase("true") && !enabled.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("grading.feedback.enabled must be true or false");
        }
        String lines = properties.getProperty("grading.feedback.max.diagnostic.lines",
                DEFAULT_MAX_DIAGNOSTIC_LINES).trim();
        int maxLines;
        try {
            maxLines = Integer.parseInt(lines);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "grading.feedback.max.diagnostic.lines must be a positive whole number", exception);
        }
        return new FeedbackConfig(Boolean.parseBoolean(enabled),
                properties.getProperty("grading.feedback.directory", DEFAULT_DIRECTORY).trim(), maxLines);
    }
}
