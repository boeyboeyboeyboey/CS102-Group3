package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionServicesTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void processRunnerBoundsBothStreamsAndDrainsThem() throws Exception {
        Path cwd = Files.createDirectory(temporaryDirectory.resolve("streams"));
        ProcessResult result = ProcessRunner.run(javaSourceCommand(cwd, "Flood",
                "System.out.print(\"x\".repeat(200000)); System.err.print(\"y\".repeat(200000));"),
                cwd, 5000, 128);

        assertEquals(0, result.exitCode());
        assertFalse(result.timedOut());
        assertEquals(128, result.stdout().length());
        assertEquals(128, result.stderr().length());
    }

    @Test
    void processRunnerTimesOutLoopAndCanRunFollowingProcess() throws Exception {
        Path cwd = Files.createDirectory(temporaryDirectory.resolve("timeout"));
        ProcessResult timedOut = ProcessRunner.run(javaSourceCommand(cwd, "Loop", "while (true) {}"), cwd, 150, 64);
        ProcessResult later = ProcessRunner.run(javaSourceCommand(cwd, "Ok", "System.out.print(\"ok\");"), cwd, 5000, 64);

        assertTrue(timedOut.timedOut());
        assertFalse(later.timedOut());
        assertEquals("ok", later.stdout());
    }

    @Test
    void interruptingProcessRunnerTerminatesTheChildAndPropagatesInterruption() throws Exception {
        Path cwd = Files.createDirectory(temporaryDirectory.resolve("interrupt"));
        Path pidFile = cwd.resolve("pid.txt");
        Path source = cwd.resolve("InterruptLoop.java");
        Files.writeString(source, "class InterruptLoop { public static void main(String[] args) throws Exception { "
                + "java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), "
                + "Long.toString(ProcessHandle.current().pid())); while (true) {} } }");
        List<String> command = List.of(java(), source.toString(), pidFile.toString());
        java.util.concurrent.atomic.AtomicReference<Throwable> outcome =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread runner = new Thread(() -> {
            try {
                ProcessRunner.run(command, cwd, 10000, 64);
            } catch (Throwable throwable) {
                outcome.set(throwable);
            }
        });
        runner.start();
        long childPid = awaitPid(pidFile, System.nanoTime() + TimeUnit.SECONDS.toNanos(5));

        runner.interrupt();
        runner.join(5000);

        assertFalse(runner.isAlive(), "interrupted process runner must finish cleanup");
        assertTrue(outcome.get() instanceof InterruptedException);
        var childHandle = ProcessHandle.of(childPid);
        boolean aliveAfterRunnerCleanup = childHandle.map(ProcessHandle::isAlive).orElse(false);
        if (aliveAfterRunnerCleanup) {
            ProcessHandle child = childHandle.get();
            child.destroyForcibly();
            child.onExit().get(2, TimeUnit.SECONDS);
        }
        assertFalse(aliveAfterRunnerCleanup,
                "interrupted process must be terminated");
    }

    private static long awaitPid(Path pidFile, long deadline) throws Exception {
        while (System.nanoTime() < deadline) {
            if (Files.exists(pidFile)) {
                String text = Files.readString(pidFile).trim();
                if (!text.isEmpty()) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException partiallyWritten) {
                        // the child creates the file before writing the pid; keep polling
                    }
                }
            }
            Thread.sleep(10);
        }
        throw new AssertionError("child process did not start");
    }

    @Test
    void compilerReturnsStudentSyntaxErrorsAndKeepsQuestionWorkspaceIsolated() throws Exception {
        Path studentRoot = Files.createDirectory(temporaryDirectory.resolve("student"));
        Path qx = Files.createDirectory(studentRoot.resolve("QX"));
        Files.writeString(qx.resolve("QX.java"), "public class QX { broken }");
        Path part = Files.createDirectory(temporaryDirectory.resolve("part"));
        QuestionDefinition question = question(List.of());

        ProcessResult result = new StudentCompiler().compile(question, studentRoot, part,
                javac(), 10000, 4096);

        assertFalse(result.timedOut());
        assertTrue(result.exitCode() != 0);
        assertTrue(result.stderr().contains("error"));
        assertTrue(Files.isRegularFile(part.resolve("src/QXAdapter.java")));
        assertFalse(Files.exists(part.resolve("classes/QX.class")));
    }

    @Test
    void executorUsesResultFileAndTreatsSystemExitAsRuntimeError() throws Exception {
        Path part = Files.createDirectory(temporaryDirectory.resolve("run"));
        Path classes = Files.createDirectory(part.resolve("classes"));
        compileRunner(classes, "ExitRunner", "System.exit(0);");
        TestCaseDefinition test = test("exit", "ExitRunner");

        var result = new CaseExecutor().execute(test, question(List.of()), part,
                java(), 5000, 512);

        assertEquals(TestStatus.RUNTIME_ERROR, result.getStatus());
        assertTrue(result.getStandardError().contains("did not write a result file"));
    }

    @Test
    void executorAcceptsOnlyExplicitResultProtocol() throws Exception {
        Path part = Files.createDirectory(temporaryDirectory.resolve("normal"));
        Path classes = Files.createDirectory(part.resolve("classes"));
        compileRunner(classes, "ResultRunner",
                "try { java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), \"PASSED\\n\"); } catch (Exception e) { throw new RuntimeException(e); }");
        TestCaseDefinition test = test("pass", "ResultRunner");

        var result = new CaseExecutor().execute(test, question(List.of()), part,
                java(), 5000, 1);

        assertEquals(TestStatus.PASSED, result.getStatus());
    }

    @Test
    void missingAdapterIsInfrastructureError() throws Exception {
        Path studentRoot = Files.createDirectory(temporaryDirectory.resolve("student-no-adapter"));
        Files.createDirectory(studentRoot.resolve("QMissing"));
        Path part = Files.createDirectory(temporaryDirectory.resolve("missing"));
        QuestionDefinition missing = new QuestionDefinition("missing", "QMissing", "QMissing.java",
                Path.of("tester"), 1, 1, List.of(), List.of(), List.of());

        IOException error = org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> new StudentCompiler().compile(missing, studentRoot, part, javac(), 5000, 512));
        assertTrue(error.getMessage().contains("adapter resource is missing"));
    }

    private void compileRunner(Path classes, String className, String body) throws IOException {
        Path source = temporaryDirectory.resolve(className + ".java");
        Files.writeString(source, "public class " + className + " { public static void main(String[] args) { "
                + body + " } }");
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", classes.toString(), source.toString());
        assertEquals(0, exit);
    }

    private static QuestionDefinition question(List<Path> fixtures) {
        return new QuestionDefinition("QX", "QX", "QX.java", Path.of("tester"), 1, 1,
                List.of(), fixtures, List.of());
    }

    private static TestCaseDefinition test(String id, String runner) {
        return new TestCaseDefinition(id, "QX", runner, List.of(), 1);
    }

    private static List<String> javaSourceCommand(Path cwd, String className, String body) throws IOException {
        Path source = cwd.resolve(className + ".java");
        Files.writeString(source, "class " + className + " { public static void main(String[] args) { "
                + body + " } }");
        return List.of(java(), source.toString());
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static String javac() {
        return Path.of(System.getProperty("java.home"), "bin", "javac").toString();
    }
}
