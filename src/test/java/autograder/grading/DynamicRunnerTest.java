package autograder.grading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicRunnerTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path DYNAMIC_RUNNER = PROJECT_ROOT.resolve(
            "src/main/resources/autograder/adapters/DynamicRunner.java");
    private static final Path Q3_SUPPORT = PROJECT_ROOT.resolve("Project/RenameToYourUsername/Q3");
    private static final Path PERSONS_FIXTURE = PROJECT_ROOT.resolve(
            "Project/Tester-Files/personstester.txt");

    private static final String CORRECT_Q2A = """
            import java.nio.file.Files;
            import java.nio.file.Path;
            public class Q2a {
                public static double getAverageAge(String filename, String surname) {
                    try {
                        double sum = 0;
                        int count = 0;
                        for (String line : Files.readAllLines(Path.of(filename))) {
                            int separator = line.lastIndexOf('-');
                            if (separator < 0) continue;
                            String[] name = line.substring(0, separator).trim().split("\\\\s+");
                            String age = line.substring(separator + 1).trim();
                            if (name[0].equalsIgnoreCase(surname)
                                    || name[name.length - 1].equalsIgnoreCase(surname)) {
                                sum += Integer.parseInt(age);
                                count++;
                            }
                        }
                        return count == 0 ? 0.0 : sum / count;
                    } catch (Exception exception) {
                        return -1.0;
                    }
                }
            }
            """;

    private static final String CORRECT_Q3 = """
            import java.util.ArrayList;
            import java.util.List;
            public class Q3 {
                public static List<Shape> sortShapes(List<Shape> shapes) {
                    ArrayList<Shape> result = new ArrayList<>();
                    for (Shape shape : shapes) {
                        if (shape.getArea() <= 1000.0) result.add(shape);
                    }
                    result.sort((first, second) -> {
                        int byArea = Double.compare(second.getArea(), first.getArea());
                        return byArea != 0 ? byArea
                                : Double.compare(second.getPerimeter(), first.getPerimeter());
                    });
                    return result;
                }
            }
            """;

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsUppercaseSurnameAtEitherEndOfName() throws Exception {
        Path classes = compile("Q2a", CORRECT_Q2A, null);
        Files.copy(PERSONS_FIXTURE, classes.resolve("personstester.txt"));

        for (long seed : List.of(7L, 42L, 99L)) {
            assertRunnerPasses("Q2a", seed, classes, classes);
        }
    }

    @Test
    void filtersLargeShapesAndUsesPerimeterAsAreaTieBreaker() throws Exception {
        Path classes = compile("Q3", CORRECT_Q3, Q3_SUPPORT);

        for (long seed : List.of(7L, 42L, 99L)) {
            assertRunnerPasses("Q3", seed, classes, Q3_SUPPORT);
        }
    }

    private Path compile(String questionId, String source, Path support) throws IOException {
        Path classes = Files.createDirectories(temporaryDirectory.resolve(questionId));
        Path studentSource = classes.resolve(questionId + ".java");
        Files.writeString(studentSource, source, StandardCharsets.UTF_8);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try (PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            String[] compilerArguments = support == null
                    ? new String[] {"--release", "17", "-d", classes.toString(),
                        DYNAMIC_RUNNER.toString(), studentSource.toString()}
                    : new String[] {"--release", "17", "-cp", support.toString(), "-d",
                        classes.toString(), DYNAMIC_RUNNER.toString(), studentSource.toString()};
            int result = ToolProvider.getSystemJavaCompiler().run(
                    null, null, errorStream, compilerArguments);
            assertEquals(0, result, errors.toString(StandardCharsets.UTF_8));
        }
        return classes.toAbsolutePath().normalize();
    }

    private void assertRunnerPasses(String questionId, long seed, Path classes, Path support)
            throws Exception {
        Path resultFile = classes.resolve("result-" + seed + ".txt");
        String classpath = support.equals(classes)
                ? classes.toString()
                : classes + java.io.File.pathSeparator + support;
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Process process = new ProcessBuilder(java.toString(), "-cp", classpath,
                "DynamicRunner", questionId, Long.toString(seed), resultFile.toString())
                .directory(classes.toFile()).redirectErrorStream(true).start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertTrue(finished, questionId + " runner timed out");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertEquals("PASSED", Files.readString(resultFile, StandardCharsets.UTF_8).trim(), output);
    }
}
