package autograder.autotester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Q2a extends Question {
    private static final String FIXTURE_NAME = "personstester.txt";
    private static final Path FIXTURE_PATH = Path.of("Project/Tester-Files/personstester.txt");
    private static final List<String> SURNAME_CANDIDATES = List.of("TAN", "CHAN", "LIM", "TEO", "JUN");

    public Q2a() {
        staticMethod();
        signature("Q2a", "getAverageAge", double.class);
        describe("Return the average age for people whose first or last name token matches the surname, ignoring case; return zero when there are no matches and -1 when the file is unavailable.");
        param("filename", String.class);
        param("surname", String.class);
        includeFixture(FIXTURE_PATH.toString());
        addTestCase("random-surname", random -> FIXTURE_NAME, Q2a::randomSurname);
        addTestCase("missing-file", random -> "missing-persons.txt", random -> "LEE");
        addTestCase("no-matching-name", random -> FIXTURE_NAME, random -> "NO_MATCH");
    }

    @Override
    public Object answerKey(List<Object> arguments) throws IOException {
        String filename = (String) arguments.get(0);
        String query = (String) arguments.get(1);
        if (!filename.equals(FIXTURE_NAME)) return -1.0;
        List<String> lines = Files.readAllLines(FIXTURE_PATH);
        double total = 0;
        int count = 0;
        for (String line : lines) {
            int separator = line.lastIndexOf('-');
            if (separator < 0) continue;
            String[] nameTokens = line.substring(0, separator).trim().split("\\s+");
            int age;
            try {
                age = Integer.parseInt(line.substring(separator + 1).trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (nameTokens[0].equalsIgnoreCase(query)
                    || nameTokens[nameTokens.length - 1].equalsIgnoreCase(query)) {
                total += age;
                count++;
            }
        }
        return count == 0 ? 0.0 : total / count;
    }

    private static String randomSurname(Random random) {
        String surname = SURNAME_CANDIDATES.get(random.nextInt(SURNAME_CANDIDATES.size()));
        if (random.nextBoolean()) surname = surname.toLowerCase(Locale.ROOT);
        return surname;
    }
}
