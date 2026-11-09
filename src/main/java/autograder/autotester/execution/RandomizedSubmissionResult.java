package autograder.autotester.execution;

public record RandomizedSubmissionResult(String archive, String report,
                                         RandomizedResult result) { }
