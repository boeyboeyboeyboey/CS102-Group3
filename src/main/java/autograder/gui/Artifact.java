package autograder.gui;

import java.nio.file.Path;

/** One output of a finished run that the instructor can open. */
public record Artifact(String label, Path path, View view) {
    /** Where an output is shown: in the system's own application, or inside the GUI. */
    public enum View {
        EXTERNAL,
        TABLE,
        FEEDBACK
    }
}
