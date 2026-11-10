package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Settings and thresholds for the score sanity checks. */
public record SanityConfig(boolean enabled, int minGraded, int zeroPercent, int sameFailurePercent,
                           int timeoutPercent, int fixedDynamicGapPoints) {
    private static final int DEFAULT_MIN_GRADED = 5;
    private static final int DEFAULT_ZERO_PERCENT = 90;
    private static final int DEFAULT_SAME_FAILURE_PERCENT = 80;
    private static final int DEFAULT_TIMEOUT_PERCENT = 50;
    private static final int DEFAULT_FIXED_DYNAMIC_GAP_POINTS = 40;
    private static final int MAX_PERCENT = 100;

    public SanityConfig {
        if (minGraded <= 0 || !percent(zeroPercent) || !percent(sameFailurePercent)
                || !percent(timeoutPercent) || !percent(fixedDynamicGapPoints)) {
            throw new IllegalArgumentException("invalid sanity settings: min graded must be positive "
                    + "and percentages and gap points must be 1..100");
        }
    }

    public static SanityConfig defaults() {
        return withEnabled(true);
    }

    public static SanityConfig disabled() {
        return withEnabled(false);
    }

    private static SanityConfig withEnabled(boolean enabled) {
        return new SanityConfig(enabled, DEFAULT_MIN_GRADED, DEFAULT_ZERO_PERCENT,
                DEFAULT_SAME_FAILURE_PERCENT, DEFAULT_TIMEOUT_PERCENT, DEFAULT_FIXED_DYNAMIC_GAP_POINTS);
    }

    public static SanityConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return load(properties, file.toAbsolutePath().normalize().getParent());
    }

    static SanityConfig load(Properties properties, Path baseDirectory) {
        String enabled = properties.getProperty("grading.sanity.enabled", "true").trim();
        if (!enabled.equalsIgnoreCase("true") && !enabled.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("grading.sanity.enabled must be true or false");
        }
        return new SanityConfig(Boolean.parseBoolean(enabled),
                integer(properties, "min.graded", DEFAULT_MIN_GRADED, Integer.MAX_VALUE),
                integer(properties, "zero.percent", DEFAULT_ZERO_PERCENT, MAX_PERCENT),
                integer(properties, "same.failure.percent", DEFAULT_SAME_FAILURE_PERCENT, MAX_PERCENT),
                integer(properties, "timeout.percent", DEFAULT_TIMEOUT_PERCENT, MAX_PERCENT),
                integer(properties, "fixed.dynamic.gap.points",
                        DEFAULT_FIXED_DYNAMIC_GAP_POINTS, MAX_PERCENT));
    }

    private static int integer(Properties properties, String name, int fallback, int maximum) {
        String key = "grading.sanity." + name;
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a whole number");
        }
        if (parsed < 1 || parsed > maximum) {
            throw new IllegalArgumentException(maximum == Integer.MAX_VALUE
                    ? key + " must be at least 1" : key + " must be between 1 and " + maximum);
        }
        return parsed;
    }

    private static boolean percent(int value) {
        return value >= 1 && value <= MAX_PERCENT;
    }
}
