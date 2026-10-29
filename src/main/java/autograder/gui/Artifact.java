package autograder.gui;

import java.nio.file.Path;

/** One output of a finished run that the instructor can open. */
public record Artifact(String label, Path path) {
}
