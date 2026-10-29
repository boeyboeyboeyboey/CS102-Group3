package autograder.autotester;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomizedGraderCommandTest {
    @Test
    void childJvmWritesUtf8BeforeTheUnchangedRunnerArguments() {
        Question question = QuestionRegistry.find("Q1");
        Path classes = Path.of("classes");
        Path inputs = Path.of("inputs.bin");
        Path outputs = Path.of("outputs.bin");

        List<String> command = RandomizedGrader.childCommand("java", classes, question, inputs, outputs);

        assertEquals(List.of("java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8", "-classpath", classes.toString(), "RandomizedStudentRunner",
                question.getClassName(), question.getMethodName(), inputs.toString(), outputs.toString()),
                command.subList(0, 11));
        assertEquals(11 + question.getParameters().size(), command.size());
    }
}
