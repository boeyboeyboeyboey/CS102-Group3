package autograder.util;

/** Turns an exception into text for people, falling back to the class name when it carries no message. */
public final class ExceptionMessages {
    private ExceptionMessages() {
    }

    /** The message, or the simple class name when the message is null or blank. */
    public static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
