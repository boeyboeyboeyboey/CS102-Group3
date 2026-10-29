package autograder.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SanityConfigTest {
    @TempDir
    Path directory;

    @Test
    void usesDefaultsWhenKeysAreAbsent() throws Exception {
        Path file = directory.resolve("config.properties");
        Files.writeString(file, "");
        assertEquals(SanityConfig.defaults(), SanityConfig.load(file));
        assertEquals(new SanityConfig(true, 5, 90, 80, 50, 40), SanityConfig.defaults());
    }

    @Test
    void readsOverrides() throws Exception {
        Path file = directory.resolve("config.properties");
        Files.writeString(file, "grading.sanity.enabled=false\ngrading.sanity.min.graded=3\n"
                + "grading.sanity.zero.percent=70\ngrading.sanity.same.failure.percent=60\n"
                + "grading.sanity.timeout.percent=30\ngrading.sanity.fixed.dynamic.gap.points=25\n");
        assertEquals(new SanityConfig(false, 3, 70, 60, 30, 25), SanityConfig.load(file));
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        Path file = directory.resolve("config.properties");
        for (String setting : new String[] {"enabled=yes", "min.graded=0", "min.graded=x",
                "zero.percent=0", "zero.percent=101", "same.failure.percent=-1", "timeout.percent=2.5",
                "fixed.dynamic.gap.points=0", "fixed.dynamic.gap.points=101"}) {
            Files.writeString(file, "grading.sanity." + setting);
            assertThrows(IllegalArgumentException.class, () -> SanityConfig.load(file), setting);
        }
    }
}
