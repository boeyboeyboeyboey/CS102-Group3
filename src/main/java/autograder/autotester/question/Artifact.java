package autograder.autotester.question;

import java.nio.file.Path;

public record Artifact(Path source, ArtifactKind kind) { }
