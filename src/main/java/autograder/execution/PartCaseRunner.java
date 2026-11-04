package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// runs the cases of one compiled part in a reused jvm instead of one jvm per case; results use
// the same statuses and diagnostics as CaseExecutor, and a timeout, crash, non-zero exit or
// leftover thread replaces the jvm before the next case
public final class PartCaseRunner {
    private static final String HOST_DIRECTORY = "_host";
    private static final String RESULT_FILE = "case-result.txt";

    private final String javaExecutable;
    private final long caseTimeoutMillis;
    private final long startupTimeoutMillis;
    private final int maxOutputBytes;

    public PartCaseRunner(String javaExecutable, long caseTimeoutMillis,
                          long startupTimeoutMillis, int maxOutputBytes) {
        if (caseTimeoutMillis <= 0 || startupTimeoutMillis <= 0 || maxOutputBytes < 0) {
            throw new IllegalArgumentException("positive timeouts and an output limit are required");
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
        Path classes = workspace.resolve("classes");
        if (!Files.isDirectory(classes)) {
            throw new IOException("compiled classes directory is missing: " + classes);
        }
        FixtureDirectory hostDirectory = new FixtureDirectory(
                workspace.resolve("cases").resolve(HOST_DIRECTORY), question.getFixtures());
        CaseHostProcess host = null;
        try {
            for (TestCaseDefinition test : tests) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("grading cancelled");
                }
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
                if (outcome.needsNewHost()) {
                    host = stop(host);
                }
            }
        } finally {
            stop(host);
        }
    }

    private CaseHostProcess.HostCaseOutcome runCase(CaseHostProcess host, TestCaseDefinition test,
                                                    Path workspace, Consumer<TestCaseResult> results)
            throws IOException, InterruptedException {
        Path caseDirectory = workspace.resolve("cases").resolve(test.getId());
        new DirectoryCleaner().deleteRecursively(caseDirectory);
        Files.createDirectories(caseDirectory);
        Path resultFile = caseDirectory.resolve(RESULT_FILE);
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
