package autograder.execution;

import autograder.model.TestCaseDefinition;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseExecutorCommandTest {
    @Test
    void addsUtf8FlagsBeforeClasspathAndKeepsRemainingOrder() {
        TestCaseDefinition test = new TestCaseDefinition("c1", "Q1", "Runner",
                List.of("a", "b"), 1);

        List<String> command = CaseExecutor.buildCommand("java", Path.of("classes"), test,
                Path.of("result.txt"));

        assertEquals(List.of("java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8", "-classpath", "classes", "Runner", "a", "b",
                "result.txt"), command);
    }
}
