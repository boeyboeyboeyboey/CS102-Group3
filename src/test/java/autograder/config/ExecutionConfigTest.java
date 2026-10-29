package autograder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesToolAndOutputPathsAgainstConfiguration() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        properties.setProperty("execution.javac", "tools/custom-javac");
        ExecutionConfig config = load(properties, app);
        assertEquals(temporaryDirectory.toRealPath().resolve("grading"), config.directory());
        assertEquals(temporaryDirectory.resolve("tools/custom-javac").toString(), config.javacExecutable());
        assertTrue(Path.of(config.javaExecutable()).isAbsolute());
    }

    @Test
    void rejectsInputOverlapAndInvalidLimitsOrReportNames() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        properties.setProperty("grading.directory", app.getInputDirectory().resolve("output").toString());
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
        properties.setProperty("grading.directory", "grading");
        properties.setProperty("execution.case.timeout.ms", "0");
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
        properties.setProperty("execution.case.timeout.ms", "2000");
        properties.setProperty("grading.report.file", "../source.csv");
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
        properties.setProperty("grading.report.file", "cases.csv");
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
    }

    @Test
    void rejectsEveryReservedReportFilenameCaseInsensitively() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        for (String name : new String[] {"starter-analysis.csv", "Starter-Analysis.csv",
                "STARTER-ANALYSIS.csv", "sTaRtEr-AnAlYsIs.csv", "Cases.csv", "CASES.csv",
                "Submissions.csv", "SUBMISSIONS.csv", "Similarity.csv",
                "SIMILARITY.csv", "sanity-checks.csv", "Sanity-Checks.CSV"}) {
            properties.setProperty("grading.report.file", name);
            assertThrows(IllegalArgumentException.class, () -> load(properties, app), name);
        }
    }

    @Test
    void acceptsDistinctScoreFilenamesIncludingSimilarNames() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        for (String name : new String[] {"scorereport.csv", "scores.csv", "starter-analysis-results.csv"}) {
            properties.setProperty("grading.report.file", name);
            assertEquals(name, load(properties, app).reportFile());
        }
    }

    @Test
    void defaultsWorkersToOneAndParsesConfiguredValue() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        assertEquals(1, load(properties, app).workers());
        properties.setProperty("execution.workers", " ");
        assertEquals(1, load(properties, app).workers());
        properties.setProperty("execution.workers", "4");
        assertEquals(4, load(properties, app).workers());
        properties.setProperty("execution.workers", "32");
        assertEquals(32, load(properties, app).workers());
    }

    @Test
    void rejectsWorkerCountsOutsideOneToThirtyTwo() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        for (String value : new String[] {"0", "-1", "33", "abc"}) {
            properties.setProperty("execution.workers", value);
            assertThrows(IllegalArgumentException.class, () -> load(properties, app), value);
        }
    }

    @Test
    void resolvesAutoWorkersCaseInsensitivelyThroughLimits() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        for (String value : new String[] {"auto", " AUTO ", "Auto"}) {
            properties.setProperty("execution.workers", value);
            assertEquals(2, load(properties, app, new WorkerLimits(() -> 2)).workers(), value);
            assertEquals(4, load(properties, app, new WorkerLimits(() -> 16)).workers(), value);
        }
    }

    @Test
    void withWorkersCopiesConfigAndValidatesRange() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        ExecutionConfig config = load(settings(), app);
        ExecutionConfig changed = config.withWorkers(8);
        assertEquals(8, changed.workers());
        assertEquals(config.directory(), changed.directory());
        assertEquals(config.caseTimeoutMillis(), changed.caseTimeoutMillis());
        assertEquals(32, config.withWorkers(32).workers());
        assertThrows(IllegalArgumentException.class, () -> config.withWorkers(0));
        assertThrows(IllegalArgumentException.class, () -> config.withWorkers(-1));
        IllegalArgumentException tooMany = assertThrows(IllegalArgumentException.class,
                () -> config.withWorkers(33));
        assertEquals("execution.workers must be at most 32", tooMany.getMessage());
    }

    private ExecutionConfig load(Properties properties, AppConfig app,
                                 WorkerLimits limits) throws Exception {
        Path path = temporaryDirectory.resolve("execution.properties");
        try (var output = Files.newOutputStream(path)) {
            properties.store(output, null);
        }
        Properties loaded = new Properties();
        try (var input = Files.newInputStream(path)) {
            loaded.load(input);
        }
        return ExecutionConfig.load(loaded, temporaryDirectory, app, limits);
    }

    private ExecutionConfig load(Properties properties, AppConfig app) throws Exception {
        Path path = temporaryDirectory.resolve("execution.properties");
        try (var output = Files.newOutputStream(path)) {
            properties.store(output, null);
        }
        return ExecutionConfig.load(path, app);
    }

    private Properties settings() {
        Properties properties = new Properties();
        properties.setProperty("grading.directory", "grading");
        properties.setProperty("grading.report.file", "scores.csv");
        properties.setProperty("execution.java", "JDK");
        properties.setProperty("execution.javac", "JDK");
        properties.setProperty("execution.compile.timeout.ms", "15000");
        properties.setProperty("execution.case.timeout.ms", "2000");
        properties.setProperty("execution.output.max.bytes", "65536");
        return properties;
    }
}
