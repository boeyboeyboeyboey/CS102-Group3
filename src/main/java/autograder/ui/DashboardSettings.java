package autograder.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** The dashboard display mode read from the config file. */
public record DashboardSettings(Mode mode) {
    private static final String MODE_KEY = "dashboard.mode";
    private static final String DEFAULT_MODE = "auto";

    public enum Mode {
        AUTO, ANSI, PLAIN
    }

    public static DashboardSettings load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        String value = properties.getProperty(MODE_KEY, DEFAULT_MODE).trim();
        try {
            return new DashboardSettings(Mode.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "dashboard.mode must be auto, ansi, or plain", exception);
        }
    }
}
