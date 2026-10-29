package autograder.autotester;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomizedGraderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void gradesCorrectAndHardCodedSolutionsWithARepeatableSeed() throws Exception {
        Path submission = submission("public class Q1 { public boolean isEven(int n) { return n % 2 == 0; } }");
        RandomizedGrader grader = new RandomizedGrader();

        RandomizedResult correct = grader.grade(new Q1(), submission, config(), 712L);
        RandomizedResult repeated = grader.grade(new Q1(), submission, config(), 712L);
        assertTrue(correct.passed());
        assertEquals(correct, repeated);
        assertEquals(15, correct.totalCount());

        Path hardCoded = submission("public class Q1 { public boolean isEven(int n) { return false; } }");
        RandomizedResult failed = grader.grade(new Q1(), hardCoded, config(), 712L);
        assertFalse(failed.passed());
        assertTrue(failed.passedCount() < failed.totalCount());
        assertTrue(failed.failures().size() <= config().maxFailuresShown());
    }

    @Test
    void answerKeyUsesOriginalArrayWhenStudentMutatesItsInput() throws Exception {
        Path submission = submission("public class Q2 { public int sum(int[] values) { "
                + "int total = 0; for (int value : values) total += value; "
                + "if (values.length > 0) values[0] = 0; return total; } }");
        RandomizedResult result = new RandomizedGrader().grade(new Q2(), submission, config(), 98L);
        assertTrue(result.passed());
    }

    @Test
    void reportsCompileFailureAndTimeoutAsStudentFailures() throws Exception {
        Path invalid = submission("public class Q1 { this is not java }");
        RandomizedGrader grader = new RandomizedGrader();
        RandomizedResult compileFailure = grader.grade(new Q1(), invalid, config(), 1L);
        assertFalse(compileFailure.passed());
        assertTrue(compileFailure.errorReason().startsWith("COMPILE ERROR"));

        Path looping = submission("public class Q1 { public boolean isEven(int n) { "
                + "while (true) {} } }");
        RandomizedResult timeout = grader.grade(new Q1(), looping, timeoutConfig(), 1L);
        assertFalse(timeout.passed());
        assertTrue(timeout.errorReason().startsWith("TIMEOUT:"));
    }

    @Test
    void recordsStudentExceptionsAsFailedTrials() throws Exception {
        Path throwing = submission("public class Q1 { public boolean isEven(int n) { "
                + "throw new IllegalStateException(\"bad input\"); } }");
        RandomizedResult result = new RandomizedGrader().grade(new Q1(), throwing, config(), 8L);

        assertFalse(result.passed());
        assertTrue(result.failures().get(0).actual().startsWith("EXCEPTION:"));
    }

    @Test
    void reportsMissingPublicMethodAsAStudentFailure() throws Exception {
        Path missingMethod = submission("public class Q1 { public boolean anotherName(int n) { return false; } }");
        RandomizedResult result = new RandomizedGrader().grade(new Q1(), missingMethod, config(), 9L);

        assertFalse(result.passed());
        assertTrue(result.failures().get(0).actual().contains("NoSuchMethodException"));
    }

    @Test
    void writesMachineReadableVerdictSeedAndCappedFailures() throws Exception {
        Path submission = submission("public class Q1 { public boolean isEven(int n) { return false; } }");
        RandomizedConfig config = config();
        RandomizedGrader grader = new RandomizedGrader();
        RandomizedResult result = grader.grade(new Q1(), submission, config, 51L);
        Path report = grader.writeResult(new Q1(), submission, config, result);
        String text = Files.readString(report);

        assertTrue(text.startsWith("FAIL\n"));
        assertTrue(text.contains("seed: 51"));
        assertTrue(text.contains("passed "));
    }

    private Path submission(String source) throws Exception {
        Path directory = Files.createTempDirectory(temporaryDirectory, "submission-");
        Files.writeString(directory.resolve("Q1.java"), source);
        if (source.contains("class Q2")) {
            Files.move(directory.resolve("Q1.java"), directory.resolve("Q2.java"));
        }
        return directory;
    }

    private RandomizedConfig config() {
        return new RandomizedConfig(temporaryDirectory.resolve("resources"),
                temporaryDirectory.resolve("results"), javaCommand(), javacCommand(),
                5, 15_000, 3, 2, 65_536);
    }

    private RandomizedConfig timeoutConfig() {
        RandomizedConfig base = config();
        return new RandomizedConfig(base.resourcesDirectory(), base.resultsDirectory(),
                base.javaCommand(), base.javacCommand(), base.runsPerCase(), 3_000,
                base.maxFailuresShown(), base.examplesPerCase(), base.maxOutputBytes());
    }

    private String javaCommand() {
        return toolPath("java");
    }

    private String javacCommand() {
        return toolPath("javac");
    }

    private String toolPath(String executable) {
        String suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
        return Path.of(System.getProperty("java.home"), "bin", executable + suffix).toString();
    }
}
