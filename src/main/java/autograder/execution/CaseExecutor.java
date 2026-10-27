package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class CaseExecutor {
    private static final int MAX_RESULT_BYTES = 16;

    public TestCaseResult execute(TestCaseDefinition test, QuestionDefinition question,
                                  Path partWorkspace, String javaExecutable,
                                  long timeoutMillis, int maxOutputBytes)
            throws IOException, InterruptedException {
        Path workspace = partWorkspace.toAbsolutePath().normalize();
        Path caseDirectory = workspace.resolve("cases").resolve(test.getId());
        recreateDirectory(caseDirectory);
        for (Path fixture : question.getFixtures()) {
            if (!Files.isRegularFile(fixture)) {
                throw new IOException("configured fixture is missing: " + fixture);
            }
            Files.copy(fixture, caseDirectory.resolve(fixture.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Path classesDirectory = workspace.resolve("classes").toAbsolutePath().normalize();
        if (!Files.isDirectory(classesDirectory)) {
            throw new IOException("compiled classes directory is missing: " + classesDirectory);
        }
        Path resultFile = caseDirectory.resolve("case-result.txt").toAbsolutePath();
        List<String> command = buildCommand(javaExecutable, classesDirectory, test, resultFile);

        ProcessResult process = ProcessRunner.run(command, caseDirectory, timeoutMillis, maxOutputBytes);
        TestStatus status;
        String diagnostic = process.stderr();
        if (process.timedOut()) {
            status = TestStatus.TIMED_OUT;
            diagnostic = "case execution timed out\n" + diagnostic;
        } else if (process.exitCode() != 0) {
            status = TestStatus.RUNTIME_ERROR;
            diagnostic = "runner exited with status " + process.exitCode() + "\n" + diagnostic;
        } else if (!Files.isRegularFile(resultFile)) {
            status = TestStatus.RUNTIME_ERROR;
            diagnostic = "runner did not write a result file\n" + diagnostic;
        } else {
            long resultSize = Files.size(resultFile);
            if (resultSize > MAX_RESULT_BYTES) {
                status = TestStatus.RUNTIME_ERROR;
                diagnostic = "runner result file exceeded " + MAX_RESULT_BYTES + " bytes\n" + diagnostic;
            } else {
                String result = Files.readString(resultFile, StandardCharsets.UTF_8).trim();
                status = switch (result) {
                    case "PASSED" -> TestStatus.PASSED;
                    case "FAILED" -> TestStatus.FAILED;
                    case "ERROR" -> TestStatus.RUNTIME_ERROR;
                    default -> TestStatus.RUNTIME_ERROR;
                };
                if (result.equals("ERROR")) {
                    diagnostic = "runner reported an exception\n" + diagnostic;
                } else if (status == TestStatus.RUNTIME_ERROR) {
                    diagnostic = "runner wrote an invalid result token\n" + diagnostic;
                }
            }
        }
        return new TestCaseResult(test, status, process.durationMillis(),
                process.stdout(), diagnostic);
    }

    static List<String> buildCommand(String javaExecutable, Path classesDirectory,
                                     TestCaseDefinition test, Path resultFile) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        // child output is decoded as utf-8 by the parent whatever the platform default is
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-classpath");
        command.add(classesDirectory.toString());
        command.add(test.getRunnerClass());
        command.addAll(test.getArguments());
        command.add(resultFile.toString());
        return command;
    }

    private static void recreateDirectory(Path directory) throws IOException {
        new DirectoryCleaner().deleteRecursively(directory);
        Files.createDirectories(directory);
    }
}
