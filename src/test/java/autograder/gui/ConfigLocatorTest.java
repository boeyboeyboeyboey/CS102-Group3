package autograder.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLocatorTest {
    @TempDir
    Path root;

    private Path config(String directory, String name) throws Exception {
        Path folder = Files.createDirectories(root.resolve(directory));
        return Files.writeString(folder.resolve(name), "x");
    }

    @Test
    void argumentWinsThenRememberedThenWorkingDirThenCodeLocation() throws Exception {
        Path arg = config("a", "custom.properties");
        Path remembered = config("r", "mine.properties");
        Path working = config("w", "config.properties");
        Path jar = Files.createDirectories(root.resolve("repo/target")).resolve("app.jar");
        Path near = config("repo", "config.properties");
        String[] args = {arg.toString()};

        assertEquals(arg.toAbsolutePath(),
                ConfigLocator.locate(args, remembered, root.resolve("w"), jar).orElseThrow());
        assertEquals(remembered.toAbsolutePath(),
                ConfigLocator.locate(new String[0], remembered, root.resolve("w"), jar).orElseThrow());
        assertEquals(working.toAbsolutePath(),
                ConfigLocator.locate(new String[0], null, root.resolve("w"), jar).orElseThrow());
        assertEquals(near.toAbsolutePath(),
                ConfigLocator.locate(new String[0], null, root.resolve("none"), jar).orElseThrow());
    }

    @Test
    void missingCandidatesAreSkippedAndNothingFoundIsEmpty() throws Exception {
        Path stale = root.resolve("gone.properties");
        Optional<Path> found = ConfigLocator.locate(new String[] {stale.toString()}, stale,
                root.resolve("none"), root.resolve("x/y/app.jar"));

        assertTrue(found.isEmpty());
    }
}
