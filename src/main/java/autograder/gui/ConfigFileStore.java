package autograder.gui;

import autograder.config.RunSettings;
import autograder.config.RuntimePathOverrides;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads a config file as text and saves edits only when the result would load. */
final class ConfigFileStore {
    // java.util.Properties reads files as latin-1, and this keeps every byte of the file intact
    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;
    static final String CHANGED_ON_DISK =
            "This file changed since it was opened. Close this window and open it again.";
    static final String UNSUPPORTED_CHARACTERS =
            "Config files can only hold Latin-1 characters. Write any others as \\uXXXX.";

    /** The settings around a save; before is null when the old file didn't load. */
    record Saved(RunSettings before, RunSettings after) {
    }

    private ConfigFileStore() {
    }

    static String read(Path file) throws IOException {
        return Files.readString(file, CHARSET);
    }

    /** Replaces the file with edited, or throws with the reason and leaves it untouched. */
    static Saved save(Path file, String original, String edited) throws IOException {
        if (!read(file).equals(original)) {
            throw new IOException(CHANGED_ON_DISK);
        }
        if (!CHARSET.newEncoder().canEncode(edited)) {
            throw new IllegalArgumentException(UNSUPPORTED_CHARACTERS);
        }
        RunSettings before = loadOrNull(file);
        RunSettings after = validate(file.toAbsolutePath().getParent(), edited);
        Files.writeString(file, edited, CHARSET);
        return new Saved(before, after);
    }

    // the check copy sits beside the real file so relative paths resolve the same way
    private static RunSettings validate(Path directory, String text) throws IOException {
        Path check = Files.createTempFile(directory, ".config-check-", ".properties");
        try {
            Files.writeString(check, text, CHARSET);
            return RunSettings.load(check, RuntimePathOverrides.none());
        } finally {
            Files.deleteIfExists(check);
        }
    }

    private static RunSettings loadOrNull(Path file) {
        try {
            return RunSettings.load(file, RuntimePathOverrides.none());
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }
}
