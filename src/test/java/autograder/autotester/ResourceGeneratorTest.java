package autograder.autotester;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceGeneratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesCompilableStubAndTesterWithoutAnswerKeyImplementation() throws Exception {
        new ResourceGenerator().generate(new Q1(), temporaryDirectory, 2, 41L);
        Path stub = temporaryDirectory.resolve("Q1.java");
        Path tester = temporaryDirectory.resolve("Q1Tester.java");
        String stubText = Files.readString(stub);
        String testerText = Files.readString(tester);

        assertTrue(stubText.contains("public boolean isEven(int n)"));
        assertTrue(stubText.contains("Return true when the integer is even."));
        assertTrue(testerText.contains("Objects.deepEquals(expected, actual)"));
        assertFalse(stubText.contains("answerKey"));
        assertFalse(testerText.contains("answerKey"));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "17", "-d", temporaryDirectory.toString(),
                stub.toString(), tester.toString()));
    }

    @Test
    void writesArrayInputsAsValidTesterLiterals() throws Exception {
        new ResourceGenerator().generate(new Q2(), temporaryDirectory, 2, 23L);
        String tester = Files.readString(temporaryDirectory.resolve("Q2Tester.java"));
        assertTrue(tester.contains("new int[] {"));
        assertTrue(tester.contains("new int[] {}"));
    }

    @Test
    void rejectsNonPositiveExampleCount() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResourceGenerator().generate(new Q1(), temporaryDirectory, 0, 1L));
    }
}
