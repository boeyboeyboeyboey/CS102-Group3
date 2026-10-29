package autograder.gui;

import autograder.config.RuntimePathOverrides;

import java.nio.file.Path;

/** The four paths chosen in the GUI, normalized to absolute form. */
public record GuiInputs(Path config, Path submissions, Path scoreSheet, Path output) {
    public GuiInputs {
        config = normalize(config);
        submissions = normalize(submissions);
        scoreSheet = normalize(scoreSheet);
        output = normalize(output);
    }

    public RuntimePathOverrides toOverrides() {
        return new RuntimePathOverrides(submissions, scoreSheet, output);
    }

    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }
}
