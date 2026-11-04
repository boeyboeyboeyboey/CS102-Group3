package autograder.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Copies the repository config and Project folder into a temporary directory. */
final class TestSetup {
    final Path config;
    final Path submissions;
    final Path scoreSheet;
    final Path output;

    TestSetup(Path root) throws IOException {
        Path source = Path.of("").toAbsolutePath();
        Path project = Files.createDirectories(root.resolve("project"));
        copyTree(source.resolve("Project"), project.resolve("Project"));
        config = project.resolve("config.properties");
        Files.copy(source.resolve("config.properties"), config);
        submissions = project.resolve("Project/student-submission");
        scoreSheet = project.resolve("Project/IS442-ScoreSheet.csv");
        output = root.resolve("out/grading");
    }

    GuiInputs inputs() {
        return new GuiInputs(config, submissions, scoreSheet, output);
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination);
                }
            }
        }
    }
}
