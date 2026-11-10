package autograder.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Working directory shared by the cases of one host; holds only fresh copies of the fixtures. */
final class FixtureDirectory {
    private final Path directory;
    private final List<Path> fixtures;
    private Map<Path, byte[]> expected;

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
        Map<Path, byte[]> copies = new HashMap<>();
        for (Path fixture : fixtures) {
            if (!Files.isRegularFile(fixture)) {
                throw new IOException("configured fixture is missing: " + fixture);
            }
            Path copy = directory.resolve(fixture.getFileName());
            Files.copy(fixture, copy, StandardCopyOption.REPLACE_EXISTING);
            copies.put(copy.getFileName(), Files.readAllBytes(copy));
        }
        expected = copies;
    }

    // true when no case has added, removed or changed anything since the last reset; contents
    // are compared byte for byte because timestamps are too coarse on some file systems
    boolean isUnchanged() throws IOException {
        if (expected == null || !Files.isDirectory(directory)) {
            return false;
        }
        int entryCount = 0;
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                entryCount++;
                if (!matchesFixture(entry)) {
                    return false;
                }
            }
        }
        return entryCount == expected.size();
    }

    private boolean matchesFixture(Path entry) throws IOException {
        byte[] original = expected.get(entry.getFileName());
        if (original == null || !Files.isRegularFile(entry)) {
            return false;
        }
        // check the size first so a case that wrote a huge file is not read into memory
        return Files.size(entry) == original.length && Arrays.equals(Files.readAllBytes(entry), original);
    }
}
