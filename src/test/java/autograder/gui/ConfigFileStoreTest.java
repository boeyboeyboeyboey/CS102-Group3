package autograder.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileStoreTest {
    private static final String TIMEOUT = "execution.case.timeout.ms=2000";

    @TempDir
    Path root;

    private void assertNoCheckFilesLeft(Path config) throws IOException {
        try (Stream<Path> files = Files.list(config.getParent())) {
            assertTrue(files.noneMatch(file -> file.getFileName().toString().startsWith(".config-check-")));
        }
    }

    @Test
    void validEditIsSavedAndKeepsComments() throws Exception {
        Path config = new TestSetup(root).config;
        String original = "# keep me\n" + ConfigFileStore.read(config);
        Files.writeString(config, original, StandardCharsets.ISO_8859_1);

        ConfigFileStore.Saved saved = ConfigFileStore.save(config, original,
                original.replace(TIMEOUT, "execution.case.timeout.ms=3000"));

        assertEquals(2000, saved.before().execution().caseTimeoutMillis());
        assertEquals(3000, saved.after().execution().caseTimeoutMillis());
        assertTrue(ConfigFileStore.read(config).startsWith("# keep me\n"));
        assertNoCheckFilesLeft(config);
    }

    @Test
    void editThatWouldNotLoadLeavesTheFileAlone() throws Exception {
        Path config = new TestSetup(root).config;
        String original = ConfigFileStore.read(config);

        assertThrows(RuntimeException.class, () -> ConfigFileStore.save(config, original,
                original.replace(TIMEOUT, "execution.case.timeout.ms=soon")));

        assertEquals(original, ConfigFileStore.read(config));
        assertNoCheckFilesLeft(config);
    }

    @Test
    void refusesToOverwriteAFileChangedSinceOpening() throws Exception {
        Path config = new TestSetup(root).config;
        String original = ConfigFileStore.read(config);
        Files.writeString(config, original + "\n# edited elsewhere\n", StandardCharsets.ISO_8859_1);

        IOException failure = assertThrows(IOException.class,
                () -> ConfigFileStore.save(config, original, original));

        assertEquals(ConfigFileStore.CHANGED_ON_DISK, failure.getMessage());
    }

    @Test
    void refusesCharactersTheFileCannotHold() throws Exception {
        Path config = new TestSetup(root).config;
        String original = ConfigFileStore.read(config);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ConfigFileStore.save(config, original, original + "\n# 日本\n"));

        assertEquals(ConfigFileStore.UNSUPPORTED_CHARACTERS, failure.getMessage());
        assertEquals(original, ConfigFileStore.read(config));
    }
}
