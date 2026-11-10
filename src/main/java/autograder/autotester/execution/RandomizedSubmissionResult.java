package autograder.autotester.execution;

/** Result and report file name for one ZIP submission in a batch. */
public record RandomizedSubmissionResult(String archive, String report,
                                         RandomizedResult result) { }
