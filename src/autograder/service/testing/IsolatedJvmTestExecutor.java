package autograder.service.testing;

import autograder.model.ProcessResult;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.service.api.ScoreParser;
import autograder.service.api.TestExecutionStrategy;
import autograder.service.process.ExternalProcessRunner;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;

/** Executes each generated case in a fresh JVM so failures cannot poison later cases. */
public final class IsolatedJvmTestExecutor implements TestExecutionStrategy {
    private final String javaCommand;
    private final ExternalProcessRunner processRunner;
    private final ScoreParser scoreParser;

    public IsolatedJvmTestExecutor(String javaCommand, ExternalProcessRunner processRunner,
            ScoreParser scoreParser) {
        this.javaCommand = javaCommand;
        this.processRunner = processRunner;
        this.scoreParser = scoreParser;
    }

    @Override
    public TestCaseResult execute(TestCaseDefinition testCase, Path classesDirectory,
            Path workingDirectory, Duration timeout) throws IOException, InterruptedException {
        ProcessResult process = processRunner.run(List.of(javaCommand, "-cp",
                classesDirectory.toAbsolutePath().toString(), testCase.getClassName()),
                workingDirectory, timeout);
        if (process.isTimedOut()) {
            return new TestCaseResult(testCase.getNumber(), 0.0,
                    TestCaseResult.Status.TIMED_OUT, "Exceeded " + timeout.toSeconds() + "s");
        }
        OptionalDouble parsed = scoreParser.parse(process.getStandardOutput());
        if (process.getExitCode() != 0) {
            return new TestCaseResult(testCase.getNumber(), 0.0,
                    TestCaseResult.Status.EXECUTION_ERROR,
                    summarize(process.getStandardError(), "JVM exit " + process.getExitCode()));
        }
        if (parsed.isEmpty() || !Double.isFinite(parsed.getAsDouble())
                || parsed.getAsDouble() < 0.0 || parsed.getAsDouble() > 1.0) {
            return new TestCaseResult(testCase.getNumber(), 0.0,
                    TestCaseResult.Status.INVALID_OUTPUT, "Missing or invalid score sentinel");
        }
        double score = parsed.getAsDouble();
        return new TestCaseResult(testCase.getNumber(), score,
                score > 0.0 ? TestCaseResult.Status.PASSED : TestCaseResult.Status.FAILED,
                process.isOutputTruncated() ? "Output was truncated" : "");
    }

    private String summarize(String output, String fallback) {
        String trimmed = output == null ? "" : output.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        int newline = trimmed.indexOf('\n');
        return newline < 0 ? trimmed : trimmed.substring(0, newline);
    }
}
