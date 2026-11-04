package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * One effective, validated configuration built from a single read of the
 * properties file plus optional runtime path overrides.
 */
public record RunSettings(Path configFile, AppConfig app,
                          ExecutionConfig execution,
                          PlagiarismConfig plagiarism,
                          HtmlReportConfig html,
                          FeedbackConfig feedback,
                          BoilerplateConfig boilerplate,
                          SanityConfig sanity) {
    public RunSettings {
        configFile = configFile.toAbsolutePath().normalize();
    }

    // keeps the constructor from before sanity checks existed
    public RunSettings(Path configFile, AppConfig app, ExecutionConfig execution,
                       PlagiarismConfig plagiarism, HtmlReportConfig html,
                       FeedbackConfig feedback, BoilerplateConfig boilerplate) {
        this(configFile, app, execution, plagiarism, html, feedback, boilerplate,
                SanityConfig.defaults());
    }

    public RunSettings withWorkers(int workers) {
        return new RunSettings(configFile, app, execution.withWorkers(workers),
                plagiarism, html, feedback, boilerplate, sanity);
    }

    public static RunSettings load(Path configFile,
                                   RuntimePathOverrides overrides)
            throws IOException {
        // null overrides are treated as no overrides
        RuntimePathOverrides effective =
                overrides == null ? RuntimePathOverrides.none() : overrides;
        Path absoluteConfig = configFile.toAbsolutePath().normalize();
        Properties properties = read(absoluteConfig);
        apply(properties, effective);
        Path base = absoluteConfig.getParent();
        if (base == null) {
            throw new IllegalArgumentException(
                    "configuration path must have a parent directory");
        }
        AppConfig app = AppConfig.load(properties, base);
        ExecutionConfig execution =
                ExecutionConfig.load(properties, base, app);
        return new RunSettings(absoluteConfig, app, execution,
                PlagiarismConfig.load(properties, base),
                HtmlReportConfig.load(properties, base),
                FeedbackConfig.load(properties, base),
                BoilerplateConfig.load(properties, base),
                SanityConfig.load(properties, base));
    }

    private static Properties read(Path file) throws IOException {
        Properties loaded = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            loaded.load(input);
        }
        Properties copy = new Properties();
        copy.putAll(loaded);
        return copy;
    }

    private static void apply(Properties properties,
                              RuntimePathOverrides overrides) {
        set(properties, "input.directory", overrides.submissionsDirectory());
        set(properties, "score.sheet.path", overrides.scoreSheet());
        set(properties, "grading.directory", overrides.gradingDirectory());
    }

    private static void set(Properties properties, String key, Path value) {
        if (value != null) {
            properties.setProperty(key, value.toString());
        }
    }
}
