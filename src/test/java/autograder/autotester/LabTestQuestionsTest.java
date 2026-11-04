package autograder.autotester;

import autograder.execution.ProcessResult;
import autograder.execution.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabTestQuestionsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void registryContainsEveryQuestionFromTheCurrentLabTest() {
        assertEquals(List.of("Q1", "Q2", "Q1a", "Q1b", "Q2a", "Q2b", "Q3"),
                QuestionRegistry.questionIds());
    }

    @Test
    void generatedResourcesForAllLabQuestionsCompile() throws Exception {
        Path resources = temporaryDirectory.resolve("lab-resources");
        ResourceGenerator generator = new ResourceGenerator();
        List<Path> sourceFiles = new ArrayList<>();
        for (String questionId : List.of("Q1a", "Q1b", "Q2a", "Q2b", "Q3")) {
            Question question = QuestionRegistry.find(questionId);
            generator.generate(question, resources, 1, 781L);
            String generatedStub = Files.readString(resources.resolve(questionId + ".java"));
            assertTrue(generatedStub.contains("public static "));
            sourceFiles.add(resources.resolve(questionId + ".java"));
            sourceFiles.add(resources.resolve(questionId + "Tester.java"));
        }
        sourceFiles.add(resources.resolve("ShapeComparator.java"));
        sourceFiles.add(resources.resolve("DataException.java"));
        sourceFiles.add(resources.resolve("Shape.java"));
        sourceFiles.add(resources.resolve("Circle.java"));
        sourceFiles.add(resources.resolve("Rectangle.java"));
        Files.writeString(resources.resolve("Q1a.java"), isogramSource());
        Files.writeString(resources.resolve("Q1b.java"), evenSumSource());
        Files.writeString(resources.resolve("Q2a.java"), averageAgeSource());
        Files.writeString(resources.resolve("Q2b.java"), topStudentSource());
        Files.writeString(resources.resolve("Q3.java"), sortShapesSource());
        Files.writeString(resources.resolve("ShapeComparator.java"), shapeComparatorSource());
        assertTrue(Files.isRegularFile(resources.resolve("personstester.txt")));
        assertTrue(Files.isRegularFile(resources.resolve("studentstester.txt")));
        assertTrue(Files.isRegularFile(resources.resolve("DataException.java")));
        assertTrue(Files.isRegularFile(resources.resolve("Circle.java")));

        Path classes = Files.createDirectory(temporaryDirectory.resolve("generated-classes"));
        List<String> command = new ArrayList<>(List.of(javacCommand(), "--release", "17", "-classpath",
                resources.toString(), "-d", classes.toString()));
        sourceFiles.forEach(source -> command.add(source.toString()));
        ProcessResult result = ProcessRunner.run(command, temporaryDirectory, 60_000, 65_536);
        if (result.exitCode() != 0) throw new AssertionError(result.stderr() + result.stdout());
        assertEquals(0, result.exitCode());
        String classPath = classes + System.getProperty("path.separator") + resources;
        var exampleCounts = java.util.Map.of("Q1a", 2, "Q1b", 2, "Q2a", 3, "Q2b", 3, "Q3", 2);
        for (String questionId : List.of("Q1a", "Q1b", "Q2a", "Q2b", "Q3")) {
            ProcessResult tester = ProcessRunner.run(List.of(javaCommand(), "-classpath", classPath,
                    questionId + "Tester"), resources, 30_000, 65_536);
            int expectedCount = exampleCounts.get(questionId);
            assertTrue(tester.exitCode() == 0
                    && tester.stdout().contains("passed " + expectedCount + " of " + expectedCount + " examples"));
        }
    }

    @Test
    void gradesAllFiveLabQuestionsIncludingFileErrorsAndShapeCompanion() throws Exception {
        RandomizedConfig config = config();
        RandomizedGrader grader = new RandomizedGrader();

        RandomizedResult q1a = grade(grader, new Q1a(), "Q1a.java", isogramSource());
        RandomizedResult q1b = grade(grader, new Q1b(), "Q1b.java", evenSumSource());
        RandomizedResult q2a = grade(grader, new Q2a(), "Q2a.java", averageAgeSource());
        RandomizedResult q2b = grade(grader, new Q2b(), "Q2b.java", topStudentSource());
        RandomizedResult q3 = grade(grader, new Q3(), "Q3.java", sortShapesSource(),
                shapeComparatorSource());

        assertPassed(q1a);
        assertPassed(q1b);
        assertPassed(q2a);
        assertPassed(q2b);
        assertPassed(q3);
    }

    private void assertPassed(RandomizedResult result) {
        if (!result.passed()) throw new AssertionError(result);
    }

    private RandomizedResult grade(RandomizedGrader grader, Question question, String filename,
                                   String source, String... companions) throws Exception {
        Path submission = Files.createDirectory(temporaryDirectory.resolve(question.getClassName()));
        Files.writeString(submission.resolve(filename), source);
        for (int index = 0; index < companions.length; index++) {
            Files.writeString(submission.resolve(question.getCompanionSourceNames().get(index)), companions[index]);
        }
        return grader.grade(question, submission, config(), 456L);
    }

    private RandomizedConfig config() {
        return new RandomizedConfig(temporaryDirectory.resolve("resources"),
                temporaryDirectory.resolve("results"), toolPath("java"), toolPath("javac"),
                4, 30_000, 8, 2, 65_536);
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

    private String averageAgeSource() {
        return "import java.nio.file.*; import java.util.*; public class Q2a { public static double "
                + "getAverageAge(String filename, String surname) { try { double total = 0; int count = 0; "
                + "for (String line : Files.readAllLines(Path.of(filename))) { int split = line.lastIndexOf('-'); "
                + "if (split < 0) continue; String[] names = line.substring(0, split).trim().split(\"\\\\s+\"); "
                + "int age = Integer.parseInt(line.substring(split + 1).trim()); if (names[0].equalsIgnoreCase(surname) "
                + "|| names[names.length - 1].equalsIgnoreCase(surname)) { total += age; count++; } } "
                + "return count == 0 ? 0.0 : total / count; } catch (Exception ex) { return -1.0; } } }";
    }

    private String isogramSource() {
        return "import java.util.ArrayList; public class Q1a { public static ArrayList<String> "
                + "getIsogramWords(ArrayList<String> words) { ArrayList<String> result = new ArrayList<>(); "
                + "for (String word : words) { boolean[] seen = new boolean[26]; boolean unique = true; "
                + "for (char c : word.toLowerCase().toCharArray()) { if (c >= 'a' && c <= 'z') "
                + "{ int i = c - 'a'; if (seen[i]) unique = false; seen[i] = true; } } "
                + "if (unique) result.add(word); } return result; } }";
    }

    private String evenSumSource() {
        return "import java.util.ArrayList; public class Q1b { public static int getSumOfEvenIntegers("
                + "ArrayList<Object> values) { int sum = 0; for (Object value : values) "
                + "if (value instanceof Integer number && number % 2 == 0) sum += number; return sum; } }";
    }

    private String topStudentSource() {
        return "import java.nio.file.*; public class Q2b { public static String getTopStudent(String filename, "
                + "String course) { try { String best = null; double top = Double.NEGATIVE_INFINITY; "
                + "for (String line : Files.readAllLines(Path.of(filename))) { int comma = line.indexOf(','); "
                + "if (comma < 0) continue; String name = line.substring(0, comma).trim(); "
                + "for (String pair : line.substring(comma + 1).split(\"-\")) { int hash = pair.indexOf('#'); "
                + "if (hash < 0 || !pair.substring(0, hash).trim().equalsIgnoreCase(course)) continue; "
                + "double gpa = Double.parseDouble(pair.substring(hash + 1).trim()); if (gpa > top) "
                + "{ best = name; top = gpa; } } } if (best == null) throw new DataException(); "
                + "return best + \"-\" + top; } catch (DataException ex) { throw ex; } "
                + "catch (Exception ex) { throw new DataException(); } } }";
    }

    private String sortShapesSource() {
        return "import java.util.*; public class Q3 { public static List<Shape> sortShapes(List<Shape> shapes) { "
                + "List<Shape> result = new ArrayList<>(); for (Shape shape : shapes) "
                + "if (shape.getArea() <= 1000) result.add(shape); result.sort(new ShapeComparator()); "
                + "return result; } }";
    }

    private String shapeComparatorSource() {
        return "import java.util.Comparator; public class ShapeComparator implements Comparator<Shape> { "
                + "public int compare(Shape a, Shape b) { int area = Double.compare(b.getArea(), a.getArea()); "
                + "return area != 0 ? area : Double.compare(b.getPerimeter(), a.getPerimeter()); } }";
    }
}
