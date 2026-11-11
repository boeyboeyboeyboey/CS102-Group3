package autograder.submission;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lookups of a directory or file by exact name, whatever the filesystem's case rules, with a unique case-variant fallback. */
final class ExactEntries {
    private ExactEntries() {
    }

    static boolean hasDirectory(Path parent, String expectedName) throws IOException {
        return has(parent, expectedName, true);
    }

    static boolean hasFile(Path directory, String expectedName) throws IOException {
        return has(directory, expectedName, false);
    }

    /** The on-disk name to use for a required directory: the exact name, else its only case variant, else null. */
    static String resolveDirectory(Path parent, String expectedName) throws IOException {
        return resolve(parent, expectedName, true);
    }

    /** The on-disk name to use for a required file: the exact name, else its only case variant, else null. */
    static String resolveFile(Path directory, String expectedName) throws IOException {
        return resolve(directory, expectedName, false);
    }

    private static String resolve(Path parent, String expectedName, boolean wantDirectory)
            throws IOException {
        if (!Files.isDirectory(parent)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(parent)) {
            for (Path entry : entries) {
                if (wantDirectory ? Files.isDirectory(entry) : Files.isRegularFile(entry)) {
                    names.add(entry.getFileName().toString());
                }
            }
        }
        return chooseName(names, expectedName);
    }

    /** The expected name if listed, else the only name differing in case, else null when none or several. */
    static String chooseName(List<String> names, String expectedName) {
        if (names.contains(expectedName)) {
            return expectedName;
        }
        String variant = null;
        int variantCount = 0;
        for (String name : names) {
            if (name.equalsIgnoreCase(expectedName)) {
                variant = name;
                variantCount++;
            }
        }
        return variantCount == 1 ? variant : null;
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
