package autograder.gui;

/** Result of checking one input field; message is empty when nothing needs saying. */
public record FieldStatus(InputField field, boolean ok, String message) {
    public FieldStatus {
        message = message == null ? "" : message;
    }

    public static FieldStatus ok(InputField field, String message) {
        return new FieldStatus(field, true, message);
    }

    public static FieldStatus error(InputField field, String message) {
        return new FieldStatus(field, false, message);
    }
}
