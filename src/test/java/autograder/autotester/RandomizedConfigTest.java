package autograder.autotester;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RandomizedConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsDefaultsAndResolvesDirectoriesFromConfigurationLocation() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.properties");
        Files.writeString(configFile, "randomized.runs.per.case=7\nrandomized.timeout.seconds=12\n"
                + "randomized.java.command=tools/java\nrandomized.javac.command=tools/javac\n");

        RandomizedConfig config = RandomizedConfig.load(configFile);

        assertEquals(7, config.runsPerCase());
        assertEquals(12_000, config.timeoutMillis());
        assertEquals(temporaryDirectory.resolve("students/resources"), config.resourcesDirectory());
        assertEquals(temporaryDirectory.resolve("grading/results"), config.resultsDirectory());
        assertEquals(temporaryDirectory.resolve("tools/java").toString(), config.javaCommand());
        assertEquals(temporaryDirectory.resolve("tools/javac").toString(), config.javacCommand());
    }

    @Test
    void rejectsInvalidRunCountsAndTimeouts() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.properties");
        Files.writeString(configFile, "randomized.runs.per.case=0\n");
        assertThrows(IllegalArgumentException.class, () -> RandomizedConfig.load(configFile));
        Files.writeString(configFile, "randomized.timeout.seconds=0\n");
        assertThrows(IllegalArgumentException.class, () -> RandomizedConfig.load(configFile));
    }
}
