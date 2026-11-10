package autograder.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Shared parsing and validation helpers for configuration values. */
final class PropertyParsers {
    private PropertyParsers() {
    }

    static List<String> parseNames(String value, String key) {
        List<String> names = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            String name = item.trim();
            if (name.isEmpty() || names.contains(name)) {
                throw new IllegalArgumentException("invalid or duplicate value in configuration: " + key);
            }
            names.add(name);
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("configuration list cannot be empty: " + key);
        }
        return names;
    }

    static List<String> parseOptionalNames(String value) {
        List<String> names = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return names;
        }
        for (String item : value.split(",", -1)) {
            String name = item.trim();
            if (name.isEmpty() || names.contains(name)) {
                throw new IllegalArgumentException("invalid or duplicate companion source file");
            }
            names.add(name);
        }
        return names;
    }

    static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required configuration value: " + key);
        }
        return value.trim();
    }

    static boolean optionalBoolean(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key, String.valueOf(defaultValue)).trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    static String pathPrefix(Properties properties, String key) {
        String value = required(properties, key);
        Path path;
        try {
            path = Path.of(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("invalid output directory prefix: " + key, exception);
        }
        if (value.contains("/") || value.contains("\\") || path.isAbsolute()
                || path.getNameCount() != 1) {
            throw new IllegalArgumentException("output directory prefix must be a single path segment: "
                    + key);
        }
        return value;
    }

    static Pattern compile(Properties properties, String key, boolean needsCaptureGroup) {
        try {
            Pattern pattern = Pattern.compile(required(properties, key));
            if (needsCaptureGroup && pattern.matcher("").groupCount() == 0) {
                throw new IllegalArgumentException("configuration pattern needs a capture group: " + key);
            }
            return pattern;
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("invalid regular expression in configuration: " + key,
                    exception);
        }
    }

    static int positiveInt(Properties properties, String key) {
        try {
            int value = Integer.parseInt(required(properties, key));
            if (value <= 0) {
                throw new IllegalArgumentException("configuration value must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("configuration value must be an integer: " + key,
                    exception);
        }
    }

    static long positiveLong(Properties properties, String key) {
        try {
            long value = Long.parseLong(required(properties, key));
            if (value <= 0) {
                throw new IllegalArgumentException("configuration value must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("configuration value must be an integer: " + key,
                    exception);
        }
    }

    static double positiveDouble(Properties properties, String key) {
        try {
            double value = Double.parseDouble(required(properties, key));
            if (!Double.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException("configuration value must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("configuration value must be numeric: " + key,
                    exception);
        }
    }
}
