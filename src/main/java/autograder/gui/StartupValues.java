package autograder.gui;

import autograder.config.RunSettings;
import autograder.config.RuntimePathOverrides;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

/** Field contents the window opens with: remembered paths first, then config defaults. */
record StartupValues(Path config, Path submissions, Path scoreSheet, Path output,
                     String configError) {
    static StartupValues resolve(String[] args, GuiPreferences.Remembered remembered,
                                 Path workingDir, Path codeLocation) {
        Optional<Path> config = ConfigLocator.locate(args, remembered.config(), workingDir, codeLocation);
        if (config.isEmpty()) {
            return new StartupValues(null, remembered.submissions(), remembered.scoreSheet(),
                    remembered.output(), "No config file found");
        }
        try {
            RunSettings defaults = RunSettings.load(config.get(), RuntimePathOverrides.none());
            return new StartupValues(config.get(),
                    firstNonNull(remembered.submissions(), defaults.app().getInputDirectory()),
                    firstNonNull(remembered.scoreSheet(), defaults.app().getScoreSheetPath()),
                    firstNonNull(remembered.output(), defaults.execution().directory()), null);
        } catch (IOException | RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            return new StartupValues(config.get(), remembered.submissions(),
                    remembered.scoreSheet(), remembered.output(), "Config problem: " + message);
        }
    }

    static Path codeLocation() {
        try {
            return Path.of(GuiLauncher.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
        } catch (URISyntaxException | RuntimeException exception) {
            return null;
        }
    }

    private static Path firstNonNull(Path preferred, Path fallback) {
        return preferred != null ? preferred : fallback;
    }
}
