package autograder.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// working directory shared by the cases of one host; holds only fresh copies of the fixtures
final class FixtureDirectory {
    private final Path directory;
    private final List<Path> fixtures;
    private Map<Path, FileState> expected;

    FixtureDirectory(Path directory, List<Path> fixtures) {
        this.directory = directory;
        this.fixtures = List.copyOf(fixtures);
    }

    Path path() {
        return directory;
    }

    void reset() throws IOException {
        expected = null;
        new DirectoryCleaner().deleteRecursively(directory);
        Files.createDirectories(directory);
        Map<Path, FileState> copies = new HashMap<>();
        for (Path fixture : fixtures) {
            if (!Files.isRegularFile(fixture)) {
                throw new IOException("configured fixture is missing: " + fixture);
            }
            Path copy = directory.resolve(fixture.getFileName());
            Files.copy(fixture, copy, StandardCopyOption.REPLACE_EXISTING);
            copies.put(copy.getFileName(), FileState.of(copy));
        }
        expected = copies;
    }

    // true when no case has added, removed or changed anything since the last reset
    boolean isUnchanged() throws IOException {
        if (expected == null || !Files.isDirectory(directory)) {
            return false;
        }
        Map<Path, FileState> actual = new HashMap<>();
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                if (!Files.isRegularFile(entry)) {
                    return false;
                }
                actual.put(entry.getFileName(), FileState.of(entry));
            }
        }
        return actual.equals(expected);
    }

    private record FileState(long size, FileTime modified) {
        static FileState of(Path file) throws IOException {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            return new FileState(attributes.size(), attributes.lastModifiedTime());
        }
    }
}
