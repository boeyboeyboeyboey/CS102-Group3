package autograder.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public record DashboardSettings(Mode mode) {
    public enum Mode {
        AUTO, ANSI, PLAIN
    }

    public static DashboardSettings load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        String value = properties.getProperty("dashboard.mode", "auto").trim();
        try {
            return new DashboardSettings(Mode.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "dashboard.mode must be auto, ansi, or plain", exception);
        }
    }
}
