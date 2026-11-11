package autograder.util;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Turns an exception into text for people, falling back to the class name when it carries no message. */
public final class ExceptionMessages {
    private ExceptionMessages() {
    }

    /** The message, or the simple class name when the message is null or blank. */
    public static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    /** An exception that names the file and says whether it is missing or could not be read. */
    public static IOException unreadableFile(Path file, IOException cause) {
        String prefix = cause instanceof NoSuchFileException ? "file not found: " : "cannot read file: ";
        return new IOException(prefix + file, cause);
    }
}
