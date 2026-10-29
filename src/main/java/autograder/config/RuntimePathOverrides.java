package autograder.config;

import java.nio.file.Path;

/**
 * Optional runtime replacements for configured paths. A null component
 * means the configured value is used.
 */
public record RuntimePathOverrides(Path submissionsDirectory,
                                   Path scoreSheet,
                                   Path gradingDirectory) {
    public RuntimePathOverrides {
        submissionsDirectory = normalize(submissionsDirectory);
        scoreSheet = normalize(scoreSheet);
        gradingDirectory = normalize(gradingDirectory);
    }

    public static RuntimePathOverrides none() {
        return new RuntimePathOverrides(null, null, null);
    }

    private static Path normalize(Path path) {
        if (path == null) {
            return null;
        }
        return path.toAbsolutePath().normalize();
    }
}
