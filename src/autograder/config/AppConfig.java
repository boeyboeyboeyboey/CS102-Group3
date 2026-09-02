package autograder.config;

import autograder.model.QuestionSpec;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Loads and validates all application configuration from a properties file. */
public final class AppConfig {
    private final Path projectRoot;
    private final Path configFile;
    private final Properties properties;
    private final List<QuestionSpec> questions;

    private AppConfig(Path projectRoot, Path configFile, Properties properties,
            List<QuestionSpec> questions) {
        this.projectRoot = projectRoot;
        this.configFile = configFile;
        this.properties = properties;
        this.questions = Collections.unmodifiableList(questions);
    }

    public static AppConfig load(Path configFile) throws IOException {
        Path absoluteConfig = configFile.toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(absoluteConfig, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        Path root = absoluteConfig.getParent();
        List<QuestionSpec> questions = loadQuestions(properties);
        return new AppConfig(root, absoluteConfig, properties, questions);
    }

    private static List<QuestionSpec> loadQuestions(Properties properties) {
        String[] ids = require(properties, "questions").split(",");
        List<QuestionSpec> questions = new ArrayList<>();
        for (String rawId : ids) {
            String id = rawId.trim();
            String prefix = "question." + id + ".";
            List<String> fixtures = splitList(properties.getProperty(prefix + "fixtures", ""));
            List<String> companionSources = splitList(
                    properties.getProperty(prefix + "companion.sources", ""));
            List<String> supportFiles = splitList(
                    properties.getProperty(prefix + "support.files", ""));
            questions.add(new QuestionSpec(
                    id,
                    require(properties, prefix + "folder"),
                    require(properties, prefix + "source"),
                    properties.getProperty(prefix + "fallback.source", "").trim(),
                    properties.getProperty(prefix + "fallback.parent", "").trim(),
                    require(properties, prefix + "tester"),
                    Double.parseDouble(require(properties, prefix + "max.score")),
                    fixtures,
                    companionSources,
                    supportFiles));
        }
        return questions;
    }

    private static List<String> splitList(String property) {
        List<String> values = new ArrayList<>();
        if (property == null || property.trim().isEmpty()) {
            return values;
        }
        for (String value : property.split(",")) {
            values.add(value.trim());
        }
        return values;
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration: " + key);
        }
        return value.trim();
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public Path getPath(String key) {
        Path value = Path.of(require(properties, key));
        return value.isAbsolute() ? value.normalize() : projectRoot.resolve(value).normalize();
    }

    public String getRequired(String key) {
        return require(properties, key);
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(require(properties, key));
    }

    public int getPositiveInt(String key) {
        int value = Integer.parseInt(require(properties, key));
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }

    public long getPositiveLong(String key) {
        long value = Long.parseLong(require(properties, key));
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }

    public List<QuestionSpec> getQuestions() {
        return questions;
    }
}
