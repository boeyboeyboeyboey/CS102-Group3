package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BoilerplateConfig(boolean enabled, Path starterDirectory,
                                double similarityThreshold, int maxSourceBytes, int maxTokens) {
    public BoilerplateConfig {
        if (starterDirectory == null || !Double.isFinite(similarityThreshold)
                || similarityThreshold <= 0 || similarityThreshold > 1
                || maxSourceBytes <= 0 || maxSourceBytes > 10485760
                || maxTokens <= 0 || maxTokens > 10000) {
            throw new IllegalArgumentException("invalid boilerplate settings: threshold must be (0,1], "
                    + "source bytes 1..10485760 and tokens 1..10000");
        }
        starterDirectory = starterDirectory.toAbsolutePath().normalize();
    }

    public static BoilerplateConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        Path baseDirectory = file.toAbsolutePath().normalize().getParent();
        return load(properties, baseDirectory);
    }

    static BoilerplateConfig load(Properties properties, Path baseDirectory)
            throws IOException {
        String enabled = properties.getProperty("grading.boilerplate.enabled", "false").trim();
        if (!enabled.equalsIgnoreCase("true") && !enabled.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("grading.boilerplate.enabled must be true or false");
        }
        return new BoilerplateConfig(Boolean.parseBoolean(enabled), baseDirectory.resolve(
                properties.getProperty("grading.boilerplate.starter.directory",
                        "Project/RenameToYourUsername").trim()),
                Double.parseDouble(properties.getProperty("grading.boilerplate.similarity.threshold", "0.98")),
                Integer.parseInt(properties.getProperty("grading.boilerplate.max.source.bytes", "1048576")),
                Integer.parseInt(properties.getProperty("grading.boilerplate.max.tokens", "4000")));
    }
}
