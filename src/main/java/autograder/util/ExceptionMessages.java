package autograder.util;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Turns an exception into text for people, falling back to the class name when it carries no message. */
public final class ExceptionMessages {
    private static final int MAX_CAUSE_DEPTH = 20;

    private ExceptionMessages() {
    }

    /** The message, or the simple class name when the message is null or blank. */
    public static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    /** An exception that names the file and says whether it is missing or could not be read. */
    public static IOException unreadableFile(Path file, IOException cause) {
        String prefix = findCause(cause, NoSuchFileException.class) != null
                ? "file not found: " : "cannot read file: ";
        return new IOException(prefix + file, cause);
    }

    /** The first throwable of the given type in the cause chain, starting with the throwable itself, or null. */
    public static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        for (Throwable cause : causeChain(throwable)) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }

    /** The throwable and its causes, outermost first, stopping after a fixed depth so a cycle cannot loop forever. */
    public static List<Throwable> causeChain(Throwable throwable) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable cause = throwable; cause != null && chain.size() < MAX_CAUSE_DEPTH; cause = cause.getCause()) {
            chain.add(cause);
        }
        return chain;
    }
}
