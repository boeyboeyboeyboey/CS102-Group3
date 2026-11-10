package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Runs one test case in its own child jvm, used when case isolation is per case. */
public final class CaseExecutor {
    static final String CLASSES_DIRECTORY = "classes";
    static final String CASES_DIRECTORY = "cases";
    static final String SOURCE_DIRECTORY = "src";
    static final String RESULT_FILE = "case-result.txt";

    public TestCaseResult execute(TestCaseDefinition test, QuestionDefinition question,
                                  Path partWorkspace, String javaExecutable,
                                  long timeoutMillis, int maxOutputBytes)
            throws IOException, InterruptedException {
        Path workspace = partWorkspace.toAbsolutePath().normalize();
        Path caseDirectory = workspace.resolve(CASES_DIRECTORY).resolve(test.getId());
        recreateDirectory(caseDirectory);
        for (Path fixture : question.getFixtures()) {
            if (!Files.isRegularFile(fixture)) {
                throw new IOException("configured fixture is missing: " + fixture);
            }
            Files.copy(fixture, caseDirectory.resolve(fixture.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Path classesDirectory = workspace.resolve(CLASSES_DIRECTORY).toAbsolutePath().normalize();
        if (!Files.isDirectory(classesDirectory)) {
            throw new IOException("compiled classes directory is missing: " + classesDirectory);
        }
        Path resultFile = caseDirectory.resolve(RESULT_FILE).toAbsolutePath();
        List<String> command = buildCommand(javaExecutable, classesDirectory, test, resultFile);

        ProcessResult process = ProcessRunner.run(command, caseDirectory, timeoutMillis, maxOutputBytes);
        return CaseResultReader.read(test, process, resultFile);
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
