package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record SanityConfig(boolean enabled, int minGraded, int zeroPercent, int sameFailurePercent,
                           int timeoutPercent, int fixedDynamicGapPoints) {
    public SanityConfig {
        if (minGraded <= 0 || !percent(zeroPercent) || !percent(sameFailurePercent)
                || !percent(timeoutPercent) || !percent(fixedDynamicGapPoints)) {
            throw new IllegalArgumentException("invalid sanity settings: min graded must be positive "
                    + "and percentages and gap points must be 1..100");
        }
    }

    public static SanityConfig defaults() {
        return new SanityConfig(true, 5, 90, 80, 50, 40);
    }

    public static SanityConfig disabled() {
        return new SanityConfig(false, 5, 90, 80, 50, 40);
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
                integer(properties, "min.graded", 5, Integer.MAX_VALUE),
                integer(properties, "zero.percent", 90, 100),
                integer(properties, "same.failure.percent", 80, 100),
                integer(properties, "timeout.percent", 50, 100),
                integer(properties, "fixed.dynamic.gap.points", 40, 100));
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
        return value >= 1 && value <= 100;
    }
}
