package autograder.service.api;

import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/** Executes one test case according to an isolation policy. */
public interface TestExecutionStrategy {
    TestCaseResult execute(TestCaseDefinition testCase, Path classesDirectory,
            Path workingDirectory, Duration timeout) throws IOException, InterruptedException;
}
