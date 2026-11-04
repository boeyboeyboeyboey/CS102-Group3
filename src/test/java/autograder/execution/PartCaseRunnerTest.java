package autograder.execution;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartCaseRunnerTest {
    private static final String PASS = "write(args, \"PASSED\");";
    private static final long CASE_TIMEOUT = 3000;
    private static final long SHORT_TIMEOUT = 700;
    private static final long STARTUP_TIMEOUT = 30000;

    @TempDir
    Path temporaryDirectory;

    @Test
    void runsCasesInOrderWithSeparateOutputAndResults() throws Exception {
        Path part = part("order");
        runner(part, "Echo", "System.out.print(\"out-\" + args[0]); System.err.print(\"err-\" + args[0]);"
                + "write(args, args[0].equals(\"1\") ? \"PASSED\" : \"FAILED\");");

        List<TestCaseResult> results = run(part, List.of(), CASE_TIMEOUT,
                test("a", "Echo", "1"), test("b", "Echo", "2"));

        assertEquals(List.of(TestStatus.PASSED, TestStatus.FAILED), statuses(results));
        assertEquals("out-1", results.get(0).getStandardOutput());
        assertEquals("err-2", results.get(1).getStandardError());
    }

    @Test
    void eachCaseSeesFreshStaticState() throws Exception {
        Path part = part("statics");
        runner(part, "Counter", "count++; write(args, count == 1 ? \"PASSED\" : \"FAILED\");");

        List<TestCaseResult> results = run(part, List.of(), CASE_TIMEOUT,
                test("a", "Counter"), test("b", "Counter"), test("c", "Counter"));

        assertEquals(List.of(TestStatus.PASSED, TestStatus.PASSED, TestStatus.PASSED), statuses(results));
    }

    @Test
    void infiniteLoopTimesOutAndLaterCasesStillRun() throws Exception {
        Path part = part("loop");
        runner(part, "Loop", "while (true) { }");
        runner(part, "Pass", PASS);

        List<TestCaseResult> results = run(part, List.of(), SHORT_TIMEOUT,
                test("a", "Pass"), test("b", "Loop"), test("c", "Pass"));

        assertEquals(List.of(TestStatus.PASSED, TestStatus.TIMED_OUT, TestStatus.PASSED), statuses(results));
        assertTrue(results.get(1).getStandardError().contains("case execution timed out"));
    }

    @Test
    void lingeringNonDaemonThreadCountsTowardTheTimeLimit() throws Exception {
        Path part = part("threads");
        runner(part, "Linger", "write(args, \"PASSED\"); new Thread(() -> { while (true) { } }).start();");
        runner(part, "Pass", PASS);

        List<TestCaseResult> results = run(part, List.of(), SHORT_TIMEOUT,
                test("a", "Linger"), test("b", "Pass"));

        assertEquals(List.of(TestStatus.TIMED_OUT, TestStatus.PASSED), statuses(results));
    }

    @Test
    void systemExitAndUncaughtExceptionsMatchSeparateJvmDiagnostics() throws Exception {
        Path part = part("exit");
        runner(part, "ExitZero", "System.exit(0);");
        runner(part, "ExitThree", "System.exit(3);");
        runner(part, "Throws", "throw new IllegalStateException(\"boom\");");
        runner(part, "Pass", PASS);

        List<TestCaseResult> results = run(part, List.of(), CASE_TIMEOUT, test("a", "ExitZero"),
                test("b", "ExitThree"), test("c", "Throws"), test("d", "Pass"));

        assertEquals(List.of(TestStatus.RUNTIME_ERROR, TestStatus.RUNTIME_ERROR,
                TestStatus.RUNTIME_ERROR, TestStatus.PASSED), statuses(results));
        assertTrue(results.get(0).getStandardError().contains("did not write a result file"));
        assertTrue(results.get(1).getStandardError().contains("runner exited with status 3"));
        assertTrue(results.get(2).getStandardError().contains("runner exited with status 1"));
        assertTrue(results.get(2).getStandardError().contains("Exception in thread \"main\""));
        assertTrue(results.get(2).getStandardError().contains("boom"));
    }

    @Test
    void fixturesAreRestoredAfterACaseChangesThem() throws Exception {
        Path part = part("fixtures");
        Path fixture = temporaryDirectory.resolve("data.txt");
        Files.writeString(fixture, "original");
        runner(part, "Vandal", "try { java.nio.file.Files.writeString(java.nio.file.Path.of(\"data.txt\"), \"x\");"
                + " java.nio.file.Files.writeString(java.nio.file.Path.of(\"extra.txt\"), \"y\"); }"
                + " catch (Exception e) { throw new RuntimeException(e); } write(args, \"PASSED\");");
        runner(part, "Reader", "try { boolean same = java.nio.file.Files.readString(java.nio.file.Path.of(\"data.txt\"))"
                + ".equals(\"original\") && !java.nio.file.Files.exists(java.nio.file.Path.of(\"extra.txt\"));"
                + " write(args, same ? \"PASSED\" : \"FAILED\"); } catch (Exception e) { throw new RuntimeException(e); }");

        List<TestCaseResult> results = run(part, List.of(fixture), CASE_TIMEOUT,
                test("a", "Reader"), test("b", "Vandal"), test("c", "Reader"));

        assertEquals(List.of(TestStatus.PASSED, TestStatus.PASSED, TestStatus.PASSED), statuses(results));
        assertEquals("original", Files.readString(fixture));
    }

    @Test
    void standardInputIsEmptyAndSystemPropertiesAreRestored() throws Exception {
        Path part = part("globals");
        runner(part, "Stdin", "try { write(args, System.in.read() == -1 ? \"PASSED\" : \"FAILED\"); }"
                + " catch (java.io.IOException e) { throw new RuntimeException(e); }");
        runner(part, "SetProperty", "System.setProperty(\"grader.leak\", \"yes\"); write(args, \"PASSED\");");
        runner(part, "CheckProperty", "write(args, System.getProperty(\"grader.leak\") == null ? \"PASSED\" : \"FAILED\");");

        List<TestCaseResult> results = run(part, List.of(), CASE_TIMEOUT, test("a", "Stdin"),
                test("b", "SetProperty"), test("c", "CheckProperty"));

        assertEquals(List.of(TestStatus.PASSED, TestStatus.PASSED, TestStatus.PASSED), statuses(results));
    }

    @Test
    void producesTheSameStatusesAsOneJvmPerCase() throws Exception {
        Path part = part("parity");
        runner(part, "Pass", PASS);
        runner(part, "Fail", "write(args, \"FAILED\");");
        runner(part, "Error", "write(args, \"ERROR\");");
        runner(part, "Garbage", "write(args, \"MAYBE\");");
        runner(part, "Silent", "System.out.print(\"nothing written\");");
        runner(part, "ExitZero", "System.exit(0);");
        runner(part, "Loop", "while (true) { }");
        List<TestCaseDefinition> tests = List.of(test("a", "Pass"), test("b", "Fail"), test("c", "Error"),
                test("d", "Garbage"), test("e", "Silent"), test("f", "ExitZero"), test("g", "Loop"),
                test("h", "Missing"));

        List<TestCaseResult> reused = run(part, List.of(), 1500, tests.toArray(new TestCaseDefinition[0]));
        List<TestStatus> separate = new ArrayList<>();
        for (TestCaseDefinition test : tests) {
            separate.add(new CaseExecutor().execute(test, question(List.of()), part, java(), 5000, 4096)
                    .getStatus());
        }

        assertEquals(separate, statuses(reused));
    }

    @Test
    void interruptionStopsTheRunBeforeTheNextCase() throws Exception {
        Path part = part("cancel");
        runner(part, "Pass", PASS);
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class,
                    () -> run(part, List.of(), CASE_TIMEOUT, test("a", "Pass")));
        } finally {
            Thread.interrupted();
        }
    }

    private List<TestCaseResult> run(Path part, List<Path> fixtures, long timeoutMillis,
                                     TestCaseDefinition... tests) throws Exception {
        List<TestCaseResult> results = new ArrayList<>();
        new PartCaseRunner(java(), timeoutMillis, STARTUP_TIMEOUT, 4096)
                .run(question(fixtures), List.of(tests), part, results::add);
        return results;
    }

    private Path part(String name) throws IOException {
        Path part = Files.createDirectory(temporaryDirectory.resolve(name));
        Files.createDirectory(part.resolve("classes"));
        return part;
    }

    private void runner(Path part, String className, String body) throws IOException {
        Path source = temporaryDirectory.resolve(className + ".java");
        Files.writeString(source, "public class " + className + " {\n"
                + "    static int count;\n"
                + "    public static void main(String[] args) { " + body + " }\n"
                + "    static void write(String[] args, String status) {\n"
                + "        try { java.nio.file.Files.writeString(java.nio.file.Path.of(args[args.length - 1]), status); }\n"
                + "        catch (java.io.IOException e) { throw new RuntimeException(e); }\n"
                + "    }\n"
                + "}\n");
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", part.resolve("classes").toString(), source.toString());
        assertEquals(0, exit);
    }

    private static List<TestStatus> statuses(List<TestCaseResult> results) {
        List<TestStatus> statuses = new ArrayList<>();
        for (TestCaseResult result : results) {
            statuses.add(result.getStatus());
        }
        return statuses;
    }

    private static QuestionDefinition question(List<Path> fixtures) {
        return new QuestionDefinition("QX", "QX", "QX.java", Path.of("tester"), 1, 1,
                List.of(), fixtures, List.of());
    }

    private static TestCaseDefinition test(String id, String runner, String... arguments) {
        return new TestCaseDefinition(id, "QX", runner, List.of(arguments), 1);
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
