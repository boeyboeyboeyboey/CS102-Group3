package autograder.submission;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Case-sensitive lookups of a directory or file by exact name, whatever the filesystem's case rules. */
final class ExactEntries {
    private ExactEntries() {
    }

    static boolean hasDirectory(Path parent, String expectedName) throws IOException {
        return has(parent, expectedName, true);
    }

    static boolean hasFile(Path directory, String expectedName) throws IOException {
        return has(directory, expectedName, false);
    }

    private static boolean has(Path parent, String expectedName, boolean wantDirectory) throws IOException {
        if (!Files.isDirectory(parent)) {
            return false;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(parent)) {
            for (Path entry : entries) {
                if (entry.getFileName().toString().equals(expectedName)
                        && (wantDirectory ? Files.isDirectory(entry) : Files.isRegularFile(entry))) {
                    return true;
                }
            }
        }
        return false;
    }
}
