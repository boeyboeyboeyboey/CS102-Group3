package autograder.autotester.question;

/** A question method parameter: its name, runtime type and source-code type. */
public record Parameter(String name, Class<?> type, String sourceType) { }
