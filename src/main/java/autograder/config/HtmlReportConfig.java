package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record HtmlReportConfig(boolean enabled, String file) {
    public HtmlReportConfig {
        if (file == null || !file.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.html")) {
            throw new IllegalArgumentException("grading.html.file must be a simple .html filename "
                    + "using letters, digits, dots, underscores or hyphens");
        }
    }

    public static HtmlReportConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        Path baseDirectory = file.toAbsolutePath().normalize().getParent();
        return load(properties, baseDirectory);
    }

    static HtmlReportConfig load(Properties properties, Path baseDirectory)
            throws IOException {
        String enabled = properties.getProperty("grading.html.enabled", "false").trim();
        if (!enabled.equalsIgnoreCase("true") && !enabled.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("grading.html.enabled must be true or false");
        }
        return new HtmlReportConfig(Boolean.parseBoolean(enabled),
                properties.getProperty("grading.html.file", "dashboard.html").trim());
    }
}
