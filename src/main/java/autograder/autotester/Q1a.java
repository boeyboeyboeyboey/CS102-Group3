package autograder.autotester;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Q1a extends Question {
    private static final List<String> WORDS = List.of(
            "cat", "dog", "tiger", "alan", "ben", "cindy", "danny", "evan", "Evelyne",
            "book", "moon", "loop", "abc", "a", "Anna", "xYz");

    public Q1a() {
        staticMethod();
        signature("Q1a", "getIsogramWords", ArrayList.class, "ArrayList<String>");
        describe("Return the input words with no repeated letters, ignoring case and non-letters.");
        param("inputs", ArrayList.class, "ArrayList<String>");
        addTestCase("word-lists", random -> randomWords(random));
        addTestCase("empty-list", random -> new ArrayList<String>());
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        ArrayList<String> words = (ArrayList<String>) arguments.get(0);
        ArrayList<String> result = new ArrayList<>();
        for (String word : words) {
            if (isIsogram(word)) result.add(word);
        }
        return result;
    }

    private static ArrayList<String> randomWords(Random random) {
        ArrayList<String> words = new ArrayList<>();
        int count = 1 + random.nextInt(7);
        for (int index = 0; index < count; index++) {
            String word = WORDS.get(random.nextInt(WORDS.size()));
            if (random.nextBoolean() && !word.isEmpty()) word = word + word.charAt(0);
            words.add(word);
        }
        return words;
    }

    private static boolean isIsogram(String word) {
        boolean[] seen = new boolean[26];
        for (char character : word.toLowerCase(Locale.ROOT).toCharArray()) {
            if (character < 'a' || character > 'z') continue;
            int index = character - 'a';
            if (seen[index]) return false;
            seen[index] = true;
        }
        return true;
    }
}
