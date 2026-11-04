package autograder.config;

import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

final class AppConfigLoader {
    private static final List<String> SCORE_COLUMN_KEYS = List.of(
            "id", "username", "lastName", "firstName", "email",
            "numerator", "denominator", "end");

    private final Properties properties;
    private final Path baseDirectory;
    private Pattern questionDirectoryPattern;
    private List<String> requiredQuestionDirectories;
    private Path scoreSheetPath;
    private Map<String, String> scoreSheetColumns;
    private Pattern javaSourcePattern;
    private AssessmentDefinition assessment;
    private Path inputDirectory;
    private Path extractionDirectory;
    private Path workingDirectory;
    private Path anomalyLog;
    private Path correctionLog;

    private AppConfigLoader(Properties properties, Path baseDirectory) {
        this.properties = properties;
        this.baseDirectory = baseDirectory;
    }

    static AppConfig load(Path configPath) throws IOException {
        Path absoluteConfigPath = configPath.toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(absoluteConfigPath)) {
            properties.load(input);
        }
        Path baseDirectory = absoluteConfigPath.getParent();
        if (baseDirectory == null) {
            throw new IllegalArgumentException("configuration path must have a parent directory");
        }
        return load(properties, baseDirectory);
    }

    static AppConfig load(Properties properties, Path baseDirectory)
            throws IOException {
        return new AppConfigLoader(properties, baseDirectory).build();
    }

    private AppConfig build() throws IOException {
        loadQuestionRules();
        loadScoreSheet();
        javaSourcePattern = PropertyParsers.compile(properties, "java.source.regex", false);
        loadAssessment();
        validateAssessmentFolders();
        loadAndValidateOutputPaths();
        return createConfig();
    }

    private void loadQuestionRules() {
        questionDirectoryPattern = PropertyParsers.compile(
                properties, "question.directory.regex", false);
        requiredQuestionDirectories = PropertyParsers.parseNames(
                PropertyParsers.required(properties, "question.directories.required"),
                "question.directories.required");
        for (String directory : requiredQuestionDirectories) {
            if (!questionDirectoryPattern.matcher(directory).matches()) {
                throw new IllegalArgumentException("required question directory does not match its pattern: "
                        + directory);
            }
        }
    }

    private void loadScoreSheet() throws IOException {
        scoreSheetPath = ConfigPathPolicy.canonicalize(ConfigPathPolicy.resolvePath(
                baseDirectory, PropertyParsers.required(properties, "score.sheet.path")));
        scoreSheetColumns = loadScoreSheetColumns();
    }

    private void loadAssessment() throws IOException {
        assessment = AssessmentConfigLoader.load(properties, baseDirectory,
                questionDirectoryPattern, javaSourcePattern);
    }

    private void validateAssessmentFolders() {
        for (QuestionDefinition question : assessment.getQuestions()) {
            if (!requiredQuestionDirectories.contains(question.getFolder())) {
                throw new IllegalArgumentException("assessment question uses an undeclared question folder: "
                        + question.getFolder());
            }
        }
    }

    private void loadAndValidateOutputPaths() throws IOException {
        inputDirectory = loadPath("input.directory");
        extractionDirectory = loadPath("extraction.directory");
        workingDirectory = loadPath("working.directory");
        anomalyLog = loadPath("anomaly.log");
        correctionLog = loadPath("correction.log");
        ConfigPathPolicy.validateOutputPaths(inputDirectory, extractionDirectory, workingDirectory,
                anomalyLog, correctionLog, scoreSheetPath, assessment);
    }

    private Path loadPath(String key) throws IOException {
        return ConfigPathPolicy.canonicalize(ConfigPathPolicy.resolvePath(baseDirectory,
                PropertyParsers.required(properties, key)));
    }

    private AppConfig createConfig() {
        return new AppConfig(
                inputDirectory,
                extractionDirectory,
                workingDirectory,
                PropertyParsers.pathPrefix(properties, "run.directory.prefix"),
                PropertyParsers.pathPrefix(properties, "working.run.directory.prefix"),
                PropertyParsers.pathPrefix(properties, "working.submission.directory.prefix"),
                anomalyLog,
                correctionLog,
                PropertyParsers.compile(properties, "archive.file.regex", false),
                javaSourcePattern,
                questionDirectoryPattern,
                requiredQuestionDirectories,
                PropertyParsers.compile(properties, "metadata.directory.regex", false),
                PropertyParsers.compile(properties, "identity.name.regex", true),
                PropertyParsers.compile(properties, "identity.email.regex", true),
                PropertyParsers.compile(properties, "identity.username.regex", true),
                PropertyParsers.required(properties, "identity.name.label"),
                PropertyParsers.required(properties, "identity.email.label"),
                PropertyParsers.required(properties, "identity.username.label"),
                PropertyParsers.compile(properties, "identity.archive.username.regex", true),
                PropertyParsers.compile(properties, "identity.wrapper.username.regex", false),
                PropertyParsers.compile(properties, "identity.wrapper.student.id.regex", false),
                PropertyParsers.compile(properties, "identity.wrapper.placeholder.regex", false),
                PropertyParsers.required(properties, "identity.roster.prefix"),
                PropertyParsers.required(properties, "identity.roster.last-name.placeholder"),
                PropertyParsers.positiveInt(properties, "identity.header.max.lines"),
                PropertyParsers.positiveInt(properties, "io.buffer.bytes"),
                PropertyParsers.positiveInt(properties, "limits.archive.entries"),
                PropertyParsers.positiveLong(properties, "limits.entry.bytes"),
                PropertyParsers.positiveLong(properties, "limits.archive.bytes"),
                scoreSheetPath, scoreSheetColumns, assessment);
    }

    private Map<String, String> loadScoreSheetColumns() {
        Map<String, String> columns = new LinkedHashMap<>();
        for (String key : SCORE_COLUMN_KEYS) {
            columns.put(key, PropertyParsers.required(properties, "score.sheet.columns." + key));
        }
        return columns;
    }
}
