package autograder.autotester.question;

import autograder.autotester.generator.RandomWordListGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Randomized question: isogram words in a list. The answer logic must stay in step with
 * src/main/resources/autograder/adapters/DynamicRunner.java.
 */
public final class Q1a extends Question {
    private static final int ALPHABET_SIZE = 26;

    public Q1a() {
        staticMethod();
        signature("Q1a", "getIsogramWords", ArrayList.class, "ArrayList<String>");
        describe("Return the input words with no repeated letters, ignoring case and non-letters.");
        addParameter("inputs", ArrayList.class, "ArrayList<String>");
        addTestCase("word-lists", new RandomWordListGenerator());
        addTestCase("empty-list", random -> new ArrayList<String>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object answerKey(List<Object> arguments) {
        ArrayList<String> words = (ArrayList<String>) arguments.get(0);
        ArrayList<String> result = new ArrayList<>();
        for (String word : words) {
            if (isIsogram(word)) {
                result.add(word);
            }
        }
        return result;
    }

    private static boolean isIsogram(String word) {
        boolean[] seen = new boolean[ALPHABET_SIZE];
        for (char character : word.toLowerCase(Locale.ROOT).toCharArray()) {
            if (character < 'a' || character > 'z') {
                continue;
            }
            int index = character - 'a';
            if (seen[index]) {
                return false;
            }
            seen[index] = true;
        }
        return true;
    }
}
