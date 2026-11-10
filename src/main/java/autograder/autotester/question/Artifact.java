package autograder.autotester.question;

import java.nio.file.Path;

/** A file a question needs at grading time, with how it is used. */
public record Artifact(Path source, ArtifactKind kind) { }
