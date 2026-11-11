package autograder.config;

import autograder.model.QuestionDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsRelativePathsAssessmentOrderAndImmutableCollections() throws Exception {
        Properties properties = defaultProperties();
        properties.setProperty("input.directory", "source");
        properties.setProperty("extraction.directory", "output/extracted");
        properties.setProperty("working.directory", "output/working");
        properties.setProperty("anomaly.log", "logs/anomalies.log");
        properties.setProperty("correction.log", "logs/corrections.log");
        properties.setProperty("score.sheet.path", "resources/scores.csv");
        properties.setProperty("assessment.question.ids", "Q2b,Q1a");
        properties.setProperty("assessment.question.Q2b.tester.file", "resources/testers/q2b.java");
        properties.setProperty("assessment.question.Q2b.support.files", "resources/support/Q2b.class");
        properties.setProperty("assessment.question.Q2b.fixtures", "resources/fixtures/input.txt");
        properties.setProperty("assessment.question.Q2b.points.per.case", "2.5");

        AppConfig config = AppConfig.load(writeConfig(properties));
        List<QuestionDefinition> questions = config.getAssessment().getQuestions();

        assertEquals(temporaryDirectory.toRealPath().resolve("source"), config.getInputDirectory());
        assertEquals(List.of("Q2b", "Q1a"), questions.stream().map(QuestionDefinition::getId).toList());
        assertEquals(2.5, questions.get(0).getPointsPerCase(), 0.0001);
        assertEquals(temporaryDirectory.toRealPath().resolve("resources/testers/q2b.java"),
                questions.get(0).getTesterFile());
        assertEquals(List.of(temporaryDirectory.toRealPath().resolve("resources/support/Q2b.class")),
                questions.get(0).getSupportFiles());
        assertEquals(List.of(temporaryDirectory.toRealPath().resolve("resources/fixtures/input.txt")),
                questions.get(0).getFixtures());
        assertTrue(questions.get(1).getSupportFiles().isEmpty());
        assertTrue(questions.get(1).getFixtures().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> config.getRequiredQuestionDirectories().add("Q4"));
        assertThrows(UnsupportedOperationException.class,
                () -> config.getScoreSheetColumns().put("extra", "column"));
    }

    @Test
    void rejectsDuplicateRequiredFoldersBeforeLoadingAssessment() throws Exception {
        Properties properties = defaultProperties();
        properties.setProperty("question.directories.required", "Q1,Q1");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(writeConfig(properties)));

        assertEquals("invalid or duplicate value in configuration: question.directories.required",
                exception.getMessage());
    }

    @Test
    void rejectsIdentityPatternsWithoutCaptureGroups() throws Exception {
        Properties properties = defaultProperties();
        properties.setProperty("identity.name.regex", "^Name:");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(writeConfig(properties)));

        assertEquals("configuration pattern needs a capture group: identity.name.regex",
                exception.getMessage());
    }

    @Test
    void rejectsOutputDirectoriesNestedUnderSubmissionInput() throws Exception {
        Properties properties = defaultProperties();
        properties.setProperty("input.directory", "source");
        properties.setProperty("extraction.directory", "source/extracted");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(writeConfig(properties)));

        assertTrue(exception.getMessage().startsWith("extraction directory cannot overlap submission input:"));
    }

    @Test
    void rejectsNonNumericArchiveLimits() throws Exception {
        Properties properties = defaultProperties();
        properties.setProperty("limits.entry.bytes", "large");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(writeConfig(properties)));

        assertEquals("configuration value must be an integer: limits.entry.bytes = large", exception.getMessage());
    }

    private Properties defaultProperties() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("config.properties"))) {
            properties.load(input);
        }
        return properties;
    }

    private Path writeConfig(Properties properties) throws Exception {
        Path configPath = temporaryDirectory.resolve("config.properties");
        try (var output = Files.newOutputStream(configPath)) {
            properties.store(output, null);
        }
        return configPath;
    }
}
