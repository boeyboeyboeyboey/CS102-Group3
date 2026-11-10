package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs the cases of one compiled part in a reused jvm instead of one jvm per case. Results use the
 * same statuses and diagnostics as {@link CaseExecutor}, and a timeout, crash, non-zero exit or
 * leftover thread replaces the jvm before the next case.
 */
public final class PartCaseRunner {
    private static final String HOST_DIRECTORY = "_host";

    private final String javaExecutable;
    private final long caseTimeoutMillis;
    private final long startupTimeoutMillis;
    private final int maxOutputBytes;

    public PartCaseRunner(String javaExecutable, long caseTimeoutMillis,
                          long startupTimeoutMillis, int maxOutputBytes) {
        if (caseTimeoutMillis <= 0 || startupTimeoutMillis <= 0 || maxOutputBytes < 0) {
            throw new IllegalArgumentException("positive timeouts and a non-negative output limit are required");
        }
        this.javaExecutable = javaExecutable;
        this.caseTimeoutMillis = caseTimeoutMillis;
        this.startupTimeoutMillis = startupTimeoutMillis;
        this.maxOutputBytes = maxOutputBytes;
    }

    public void run(QuestionDefinition question, List<TestCaseDefinition> tests,
                    Path partWorkspace, Consumer<TestCaseResult> results)
            throws IOException, InterruptedException {
        Path workspace = partWorkspace.toAbsolutePath().normalize();
        Path classes = workspace.resolve(CaseExecutor.CLASSES_DIRECTORY);
        if (!Files.isDirectory(classes)) {
            throw new IOException("compiled classes directory is missing: " + classes);
        }
        FixtureDirectory hostDirectory = new FixtureDirectory(
                workspace.resolve(CaseExecutor.CASES_DIRECTORY).resolve(HOST_DIRECTORY), question.getFixtures());
        CaseHostProcess host = null;
        try {
            for (TestCaseDefinition test : tests) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("grading cancelled");
                }
                host = runWithRetry(host, hostDirectory, classes, test, workspace, results);
            }
        } finally {
            stop(host);
        }
    }

    // a host that dies or never starts costs at most this case: it is retried once on a fresh
    // host, then recorded as a runtime error, so the submission is never withheld for it
    private CaseHostProcess runWithRetry(CaseHostProcess host, FixtureDirectory hostDirectory,
                                         Path classes, TestCaseDefinition test, Path workspace,
                                         Consumer<TestCaseResult> results)
            throws IOException, InterruptedException {
        CaseHostProcess.HostFailureException firstFailure;
        try {
            return attempt(host, hostDirectory, classes, test, workspace, results);
        } catch (CaseHostProcess.HostFailureException failure) {
            firstFailure = failure;
        }
        try {
            return attempt(null, hostDirectory, classes, test, workspace, results);
        } catch (CaseHostProcess.HostFailureException failure) {
            results.accept(new TestCaseResult(test, TestStatus.RUNTIME_ERROR, 0, "",
                    "case host failed twice, so the case could not run\nfirst attempt: "
                            + firstFailure.getMessage() + "\nretry: " + failure.getMessage() + "\n"));
            return null;
        }
    }

    // runs one case, starting a host when needed; the result is only reported once the case has
    // finished, and the case directory is recreated first, so a retry cannot report it twice
    private CaseHostProcess attempt(CaseHostProcess current, FixtureDirectory hostDirectory,
                                    Path classes, TestCaseDefinition test, Path workspace,
                                    Consumer<TestCaseResult> results)
            throws IOException, InterruptedException {
        CaseHostProcess host = current;
        try {
            if (!hostDirectory.isUnchanged()) {
                // stop first so files the last case left open cannot block the reset
                host = stop(host);
                hostDirectory.reset();
            }
            if (host == null) {
                host = CaseHostProcess.start(javaExecutable, classes, hostDirectory.path(),
                        maxOutputBytes, startupTimeoutMillis);
            }
            CaseHostProcess.HostCaseOutcome outcome = runCase(host, test, workspace, results);
            return outcome.needsNewHost() ? stop(host) : host;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            stop(host);
            throw exception;
        }
    }

    private CaseHostProcess.HostCaseOutcome runCase(CaseHostProcess host, TestCaseDefinition test,
                                                    Path workspace, Consumer<TestCaseResult> results)
            throws IOException, InterruptedException {
        Path caseDirectory = workspace.resolve(CaseExecutor.CASES_DIRECTORY).resolve(test.getId());
        new DirectoryCleaner().deleteRecursively(caseDirectory);
        Files.createDirectories(caseDirectory);
        Path resultFile = caseDirectory.resolve(CaseExecutor.RESULT_FILE);
        List<String> arguments = new ArrayList<>(test.getArguments());
        arguments.add(resultFile.toString());

        CaseHostProcess.HostCaseOutcome outcome = host.run(test.getRunnerClass(), caseDirectory,
                arguments, caseTimeoutMillis);
        String stdout = readCapture(caseDirectory.resolve(CaseHost.STDOUT_FILE));
        String stderr = readCapture(caseDirectory.resolve(CaseHost.STDERR_FILE)) + outcome.hostErrors();
        ProcessResult process = new ProcessResult(outcome.exitCode(), outcome.timedOut(),
                outcome.durationMillis(), stdout, stderr);
        results.accept(CaseResultReader.read(test, process, resultFile));
        return outcome;
    }

    private String readCapture(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return "";
        }
        byte[] bytes = Files.readAllBytes(file);
        int length = Math.min(bytes.length, maxOutputBytes);
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private static CaseHostProcess stop(CaseHostProcess host) {
        if (host != null) {
            host.close();
        }
        return null;
    }
}
