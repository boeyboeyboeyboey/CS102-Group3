package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Settings for the starter-code comparison. */
public record BoilerplateConfig(boolean enabled, Path starterDirectory,
                                double similarityThreshold, int maxSourceBytes, int maxTokens) {
    private static final int MAX_SOURCE_BYTES_LIMIT = 10485760;
    private static final int MAX_TOKENS_LIMIT = 10000;
    private static final String DEFAULT_STARTER_DIRECTORY = "Project/RenameToYourUsername";
    private static final String DEFAULT_SIMILARITY_THRESHOLD = "0.98";
    private static final String DEFAULT_MAX_SOURCE_BYTES = "1048576";
    private static final String DEFAULT_MAX_TOKENS = "4000";

    public BoilerplateConfig {
        if (starterDirectory == null || !Double.isFinite(similarityThreshold)
                || similarityThreshold <= 0 || similarityThreshold > 1
                || maxSourceBytes <= 0 || maxSourceBytes > MAX_SOURCE_BYTES_LIMIT
                || maxTokens <= 0 || maxTokens > MAX_TOKENS_LIMIT) {
            throw new IllegalArgumentException("invalid boilerplate settings: threshold must be (0,1], "
                    + "source bytes 1.." + MAX_SOURCE_BYTES_LIMIT + " and tokens 1.." + MAX_TOKENS_LIMIT);
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
                        DEFAULT_STARTER_DIRECTORY).trim()),
                Double.parseDouble(properties.getProperty("grading.boilerplate.similarity.threshold",
                        DEFAULT_SIMILARITY_THRESHOLD)),
                Integer.parseInt(properties.getProperty("grading.boilerplate.max.source.bytes",
                        DEFAULT_MAX_SOURCE_BYTES)),
                Integer.parseInt(properties.getProperty("grading.boilerplate.max.tokens", DEFAULT_MAX_TOKENS)));
    }
}
