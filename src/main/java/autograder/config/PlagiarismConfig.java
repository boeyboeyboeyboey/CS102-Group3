package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public record PlagiarismConfig(boolean enabled, List<String> algorithms, int ngramSize, double threshold,
                               int minimumNgrams, double jplagThreshold, int jplagMinimumTokens,
                               Path starterDirectory) {
    public static final String NGRAM = "ngram";
    public static final String JPLAG = "jplag";
    private static final List<String> KNOWN_ALGORITHMS = List.of(NGRAM, JPLAG);

    public PlagiarismConfig {
        algorithms = List.copyOf(algorithms);
        if (algorithms.isEmpty()) {
            throw new IllegalArgumentException("plagiarism.algorithms must name at least one algorithm");
        }
        for (String algorithm : algorithms) {
            if (!KNOWN_ALGORITHMS.contains(algorithm)) {
                throw new IllegalArgumentException("plagiarism.algorithms contains an unknown algorithm: "
                        + algorithm + " (use " + String.join(" or ", KNOWN_ALGORITHMS) + ")");
            }
        }
        if (ngramSize < 1) {
            throw new IllegalArgumentException("plagiarism.ngram.size must be at least 1");
        }
        requireFraction(threshold, "plagiarism.threshold");
        if (minimumNgrams < 1) {
            throw new IllegalArgumentException("plagiarism.min.ngrams must be at least 1");
        }
        requireFraction(jplagThreshold, "plagiarism.jplag.threshold");
        if (jplagMinimumTokens < 1) {
            throw new IllegalArgumentException("plagiarism.jplag.min.tokens must be at least 1");
        }
    }

    public boolean uses(String algorithm) {
        return algorithms.contains(algorithm);
    }

    public static PlagiarismConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        Path baseDirectory = file.toAbsolutePath().normalize().getParent();
        return load(properties, baseDirectory);
    }

    static PlagiarismConfig load(Properties properties, Path baseDirectory)
            throws IOException {
        String enabled = required(properties, "plagiarism.enabled");
        if (!enabled.equals("true") && !enabled.equals("false")) {
            throw new IllegalArgumentException("plagiarism.enabled must be true or false");
        }
        Path starterDirectory = AppConfig.canonicalize(
                baseDirectory.resolve(required(properties, "plagiarism.starter.directory")));
        if (!Files.isDirectory(starterDirectory)) {
            throw new IllegalArgumentException("plagiarism.starter.directory is not a directory: "
                    + starterDirectory);
        }
        return new PlagiarismConfig(Boolean.parseBoolean(enabled),
                algorithmList(required(properties, "plagiarism.algorithms")),
                integer(properties, "plagiarism.ngram.size"),
                decimal(properties, "plagiarism.threshold"),
                integer(properties, "plagiarism.min.ngrams"),
                decimal(properties, "plagiarism.jplag.threshold"),
                integer(properties, "plagiarism.jplag.min.tokens"), starterDirectory);
    }

    private static List<String> algorithmList(String value) {
        List<String> algorithms = new ArrayList<>();
        for (String part : value.split(",")) {
            String algorithm = part.trim().toLowerCase(Locale.ROOT);
            if (algorithm.isEmpty()) {
                continue;
            }
            if (algorithms.contains(algorithm)) {
                throw new IllegalArgumentException("plagiarism.algorithms lists " + algorithm + " twice");
            }
            algorithms.add(algorithm);
        }
        return algorithms;
    }

    private static void requireFraction(double value, String key) {
        if (!Double.isFinite(value) || value <= 0.0 || value > 1.0) {
            throw new IllegalArgumentException(key + " must be above 0 and at most 1");
        }
    }

    private static int integer(Properties properties, String key) {
        try {
            return Integer.parseInt(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid whole number: " + key, exception);
        }
    }

    private static double decimal(Properties properties, String key) {
        try {
            return Double.parseDouble(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid number: " + key, exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required configuration value: " + key);
        }
        return value.trim();
    }
}
