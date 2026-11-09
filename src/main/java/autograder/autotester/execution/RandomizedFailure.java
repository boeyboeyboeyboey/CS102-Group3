package autograder.autotester.execution;

public record RandomizedFailure(String testCaseId, int runNumber, String input,
                                String expected, String actual) { }
