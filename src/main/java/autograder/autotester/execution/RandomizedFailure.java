package autograder.autotester.execution;

/** One failed trial: the case, run number, input, expected and actual value as text. */
public record RandomizedFailure(String testCaseId, int runNumber, String input,
                                String expected, String actual) { }
