package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseStartupTimeoutTest {
    private static final long GENEROUS_ALLOWANCE = 20000;
    private static final String WRITE_MARKER =
            "try { java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), \"\"); }"
                    + " catch (java.io.IOException e) { throw new RuntimeException(e); }";

    @TempDir
    Path temporaryDirectory;

    @Test
    void slowStartBeforeTheMarkerDoesNotUseTheCaseTimeLimit() throws Exception {
        Path classes = classes("slow");
        compile(classes, "SlowStart", "try { Thread.sleep(3000); } catch (InterruptedException e) { }"
                + WRITE_MARKER + " System.out.print(\"ok\");");
        Path marker = temporaryDirectory.resolve("slow.started");

        ProcessResult result = ProcessRunner.run(javaCommand(classes, "SlowStart", marker),
                temporaryDirectory, marker, GENEROUS_ALLOWANCE, 1000, 64);

        assertFalse(result.timedOut());
        assertEquals(0, result.exitCode());
        assertEquals("ok", result.stdout());
    }

    @Test
    void loopAfterTheMarkerTimesOutAtTheCaseLimit() throws Exception {
        Path classes = classes("loop");
        compile(classes, "LoopAfterMarker", WRITE_MARKER + " while (true) { }");
        Path marker = temporaryDirectory.resolve("loop.started");

        ProcessResult result = ProcessRunner.run(javaCommand(classes, "LoopAfterMarker", marker),
                temporaryDirectory, marker, GENEROUS_ALLOWANCE, 500, 64);

        assertTrue(result.timedOut());
        assertTrue(result.durationMillis() < GENEROUS_ALLOWANCE / 2,
                "timed out by the case limit, not the start-up allowance: " + result.durationMillis());
    }

    @Test
    void childThatNeverSignalsTimesOutAtTheAllowance() throws Exception {
        Path classes = classes("silent");
        compile(classes, "NeverSignals", "while (true) { }");
        Path marker = temporaryDirectory.resolve("silent.started");

        ProcessResult result = ProcessRunner.run(javaCommand(classes, "NeverSignals", marker),
                temporaryDirectory, marker, 1500, 200, 64);

        assertTrue(result.timedOut());
        assertTrue(result.durationMillis() >= 1500, "waited for the allowance: " + result.durationMillis());
    }

    @Test
    void launcherRunsRunnersWithTheSameStatusesAndKeepsTheMarkerOutOfTheCaseDirectory() throws Exception {
        Path part = part("statuses");
        Path classes = part.resolve("classes");
        compile(classes, "Pass", "write(args, \"PASSED\");");
        compile(classes, "ExitZero", "System.exit(0);");
        compile(classes, "Throws", "throw new IllegalStateException(\"boom\");");
        compile(classes, "Lists", "write(args, new java.io.File(\".\").list().length == 0 ? \"PASSED\" : \"FAILED\");");

        assertEquals(TestStatus.PASSED, execute(part, "Pass", 2000).getStatus());
        TestCaseResult exit = execute(part, "ExitZero", 2000);
        assertEquals(TestStatus.RUNTIME_ERROR, exit.getStatus());
        assertTrue(exit.getStandardError().contains("did not write a result file"));
        TestCaseResult thrown = execute(part, "Throws", 2000);
        assertEquals(TestStatus.RUNTIME_ERROR, thrown.getStatus());
        assertTrue(thrown.getStandardError().contains("runner exited with status 1"));
        assertTrue(thrown.getStandardError().contains("boom"));
        assertEquals(TestStatus.PASSED, execute(part, "Lists", 2000).getStatus(),
                "the working directory holds no marker");
        assertTrue(Files.exists(part.resolve("cases").resolve("Lists.started")));
    }

    @Test
    void studentStaticCodeStillCountsTowardTheCaseLimit() throws Exception {
        Path part = part("statics");
        compile(part.resolve("classes"), "SlowStatic",
                "write(args, \"PASSED\"); }\n static { try { Thread.sleep(3000); } catch (InterruptedException e) { } ");

        TestCaseResult result = execute(part, "SlowStatic", 1000);

        assertEquals(TestStatus.TIMED_OUT, result.getStatus());
    }

    @Test
    void withoutACompiledLauncherTheCaseUsesTheSingleTimeLimit() throws Exception {
        Path part = Files.createDirectory(temporaryDirectory.resolve("fallback"));
        Path classes = Files.createDirectory(part.resolve("classes"));
        compile(classes, "Pass", "write(args, \"PASSED\");");

        TestCaseResult result = execute(part, "Pass", 5000);

        assertEquals(TestStatus.PASSED, result.getStatus());
        assertFalse(Files.exists(part.resolve("cases").resolve("Pass.started")));
    }

    private TestCaseResult execute(Path part, String runner, long timeoutMillis) throws Exception {
        TestCaseDefinition test = new TestCaseDefinition(runner, "QX", runner, List.of(), 1);
        QuestionDefinition question = new QuestionDefinition("QX", "QX", "QX.java", Path.of("tester"),
                1, 1, List.of(), List.of(), List.of());
        return new CaseExecutor().execute(test, question, part, java(), GENEROUS_ALLOWANCE,
                timeoutMillis, 4096);
    }

    private Path part(String name) throws IOException {
        Path part = Files.createDirectory(temporaryDirectory.resolve(name));
        Path classes = Files.createDirectory(part.resolve("classes"));
        Path launcher = temporaryDirectory.resolve(name + "-launcher").resolve("GraderCaseLauncher.java");
        Files.createDirectories(launcher.getParent());
        try (InputStream source = getClass().getResourceAsStream("/autograder/adapters/GraderCaseLauncher.java")) {
            assertNotNull(source);
            Files.copy(source, launcher);
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", classes.toString(), launcher.toString()));
        return part;
    }

    private Path classes(String name) throws IOException {
        return Files.createDirectories(temporaryDirectory.resolve(name).resolve("classes"));
    }

    private void compile(Path classes, String className, String body) throws IOException {
        Path source = classes.getParent().resolve(className + ".java");
        Files.writeString(source, "public class " + className + " {\n"
                + "    public static void main(String[] args) { " + body + " }\n"
                + "    static void write(String[] args, String status) {\n"
                + "        try { java.nio.file.Files.writeString(java.nio.file.Path.of(args[args.length - 1]), status); }\n"
                + "        catch (java.io.IOException e) { throw new RuntimeException(e); }\n"
                + "    }\n"
                + "}\n");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", classes.toString(), source.toString()));
    }

    private static List<String> javaCommand(Path classes, String className, Path marker) {
        return List.of(java(), "-classpath", classes.toString(), className, marker.toString());
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
