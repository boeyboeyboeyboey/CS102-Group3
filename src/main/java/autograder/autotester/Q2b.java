package autograder.autotester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Q2b extends Question {
    private static final String FIXTURE_NAME = "studentstester.txt";
    private static final Path FIXTURE_PATH = Path.of("Project/Tester-Files/studentstester.txt");
    private static final List<String> COURSES = List.of("CS101", "CS102", "CS103");

    public Q2b() {
        staticMethod();
        signature("Q2b", "getTopStudent", String.class);
        describe("Return the name and GPA of the highest-GPA student enrolled in the course; throw DataException if the file or course has no matching records.");
        param("filename", String.class);
        param("courseName", String.class);
        includeFixture(FIXTURE_PATH.toString());
        includeSupportSource("src/main/resources/autograder/autotester/labtest/DataException.java");
        addTestCase("available-course", random -> FIXTURE_NAME, Q2b::randomCourse);
        addTestCase("unknown-course", random -> FIXTURE_NAME, random -> "SMU101");
        addTestCase("missing-file", random -> "missing-students.txt", random -> "CS101");
    }

    @Override
    public Object answerKey(List<Object> arguments) throws IOException {
        String filename = (String) arguments.get(0);
        String course = (String) arguments.get(1);
        if (!filename.equals(FIXTURE_NAME)) return new ExpectedException("DataException");
        String topName = null;
        double topGpa = Double.NEGATIVE_INFINITY;
        for (String line : Files.readAllLines(FIXTURE_PATH)) {
            int comma = line.indexOf(',');
            if (comma < 0) continue;
            String name = line.substring(0, comma).trim();
            for (String pair : line.substring(comma + 1).trim().split("-")) {
                int separator = pair.indexOf('#');
                if (separator < 0 || !pair.substring(0, separator).trim()
                        .equalsIgnoreCase(course)) continue;
                double gpa;
                try {
                    gpa = Double.parseDouble(pair.substring(separator + 1).trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (gpa > topGpa) {
                    topName = name;
                    topGpa = gpa;
                }
            }
        }
        return topName == null ? new ExpectedException("DataException") : topName + "-" + topGpa;
    }

    private static String randomCourse(Random random) {
        String course = COURSES.get(random.nextInt(COURSES.size()));
        if (random.nextBoolean()) course = course.toLowerCase(Locale.ROOT);
        return course;
    }
}
