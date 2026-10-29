package autograder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunSettingsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void withoutOverridesMatchesTheIndividualLoaders() throws Exception {
        Path config = writeConfig();
        RunSettings settings = RunSettings.load(config, null);
        AppConfig app = AppConfig.load(config);

        assertEquals(config.toAbsolutePath().normalize(),
                settings.configFile());
        assertEquals(app.getInputDirectory(),
                settings.app().getInputDirectory());
        assertEquals(app.getScoreSheetPath(),
                settings.app().getScoreSheetPath());
        assertEquals(app.getExtractionDirectory(),
                settings.app().getExtractionDirectory());
        assertEquals(ExecutionConfig.load(config, app),
                settings.execution());
        assertEquals(PlagiarismConfig.load(config), settings.plagiarism());
        assertEquals(HtmlReportConfig.load(config), settings.html());
        assertEquals(FeedbackConfig.load(config), settings.feedback());
        assertEquals(BoilerplateConfig.load(config), settings.boilerplate());
    }

    @Test
    void overridesReplaceOnlyTheThreePaths() throws Exception {
        Path config = writeConfig();
        Path submissions = temporaryDirectory.resolve("other/../subs");
        Path scores = temporaryDirectory.resolve("sheet.csv");
        Path grading = temporaryDirectory.resolve("out/grading");
        Files.createDirectories(submissions);
        RuntimePathOverrides overrides =
                new RuntimePathOverrides(submissions, scores, grading);

        RunSettings settings = RunSettings.load(config, overrides);
        AppConfig plain = AppConfig.load(config);

        assertTrue(overrides.submissionsDirectory().isAbsolute());
        assertEquals(temporaryDirectory.resolve("subs").toAbsolutePath(),
                overrides.submissionsDirectory());
        assertEquals(temporaryDirectory.toRealPath().resolve("subs"),
                settings.app().getInputDirectory());
        assertEquals(temporaryDirectory.toRealPath().resolve("sheet.csv"),
                settings.app().getScoreSheetPath());
        assertEquals(temporaryDirectory.toRealPath().resolve("out/grading"),
                settings.execution().directory());
        assertEquals(plain.getExtractionDirectory(),
                settings.app().getExtractionDirectory());
    }

    @Test
    void rejectsGradingDirectoryInsideSubmissionsOverride() throws Exception {
        Path config = writeConfig();
        Path submissions = temporaryDirectory.resolve("subs");
        RuntimePathOverrides overrides = new RuntimePathOverrides(
                submissions, null, submissions.resolve("grading"));

        assertThrows(IllegalArgumentException.class,
                () -> RunSettings.load(config, overrides));
    }

    @Test
    void leavesThePropertiesFileUnchanged() throws Exception {
        Path config = writeConfig();
        byte[] before = Files.readAllBytes(config);

        RunSettings.load(config, new RuntimePathOverrides(
                temporaryDirectory.resolve("subs"), null,
                temporaryDirectory.resolve("out")));

        assertArrayEquals(before, Files.readAllBytes(config));
    }

    private Path writeConfig() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(
                Path.of("config.properties"))) {
            properties.load(input);
        }
        // the plagiarism loader requires its starter directory to exist
        Files.createDirectories(temporaryDirectory.resolve(
                properties.getProperty("plagiarism.starter.directory")));
        Path configPath = temporaryDirectory.resolve("config.properties");
        try (var output = Files.newOutputStream(configPath)) {
            properties.store(output, null);
        }
        return configPath;
    }
}
