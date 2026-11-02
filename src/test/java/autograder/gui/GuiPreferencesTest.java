package autograder.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPreferencesTest {
    @TempDir
    Path root;

    private Preferences node;

    @BeforeEach
    void createNode() {
        node = Preferences.userRoot().node("autograder-test-" + UUID.randomUUID());
    }

    @AfterEach
    void removeNode() throws Exception {
        node.removeNode();
    }

    @Test
    void savedPathsRoundTrip() throws Exception {
        Path config = Files.writeString(root.resolve("config.properties"), "x");
        Path folder = Files.createDirectories(root.resolve("subs"));
        Path sheet = Files.writeString(root.resolve("sheet.csv"), "x");
        GuiPreferences preferences = new GuiPreferences(node);

        assertTrue(preferences.save(new GuiInputs(config, folder, sheet, root.resolve("out"))));
        GuiPreferences.Remembered loaded = preferences.load();

        assertEquals(config.toAbsolutePath(), loaded.config());
        assertEquals(folder.toAbsolutePath(), loaded.submissions());
        assertEquals(sheet.toAbsolutePath(), loaded.scoreSheet());
        assertEquals(root.resolve("out").toAbsolutePath(), loaded.output());
    }

    @Test
    void stalePathsLoadAsNull() {
        GuiPreferences preferences = new GuiPreferences(node);
        preferences.save(new GuiInputs(root.resolve("a.properties"), root.resolve("gone"),
                root.resolve("b.csv"), root.resolve("missing/parent/out")));

        GuiPreferences.Remembered loaded = preferences.load();

        assertNull(loaded.config());
        assertNull(loaded.submissions());
        assertNull(loaded.scoreSheet());
        assertNull(loaded.output());
    }
}
