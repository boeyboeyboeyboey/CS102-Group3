package autograder.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BoilerplateConfigTest {
    @TempDir
    Path directory;

    @Test
    void defaultsToDisabledAndResolvesStarterRelativeToConfiguration() throws Exception {
        Path file = directory.resolve("config.properties");
        Files.writeString(file, "");
        assertFalse(BoilerplateConfig.load(file).enabled());
        Files.writeString(file, "grading.boilerplate.enabled=true\ngrading.boilerplate.starter.directory=starter\n");
        assertEquals(directory.resolve("starter"), BoilerplateConfig.load(file).starterDirectory());
        assertTrue(BoilerplateConfig.load(file).enabled());
    }

    @Test
    void rejectsInvalidBooleanThresholdAndResourceLimits() throws Exception {
        Path file = directory.resolve("config.properties");
        for (String setting : new String[] {"enabled=yes", "similarity.threshold=NaN",
                "similarity.threshold=0", "similarity.threshold=1.1", "max.source.bytes=0",
                "max.tokens=10001", "max.tokens=abc"}) {
            Files.writeString(file, "grading.boilerplate." + setting);
            assertThrows(IllegalArgumentException.class, () -> BoilerplateConfig.load(file), setting);
        }
    }

    @Test
    void numberErrorsNameTheOffendingKey() throws Exception {
        Path file = directory.resolve("config.properties");
        for (String key : new String[] {"similarity.threshold", "max.source.bytes", "max.tokens"}) {
            Files.writeString(file, "grading.boilerplate." + key + "=abc");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> BoilerplateConfig.load(file), key);
            assertTrue(error.getMessage().contains("grading.boilerplate." + key), key);
        }
    }
}
