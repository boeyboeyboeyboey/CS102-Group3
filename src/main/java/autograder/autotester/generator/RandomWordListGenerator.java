package autograder.autotester.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Generates short word lists, some words with their first letter doubled, for isogram questions. */
public final class RandomWordListGenerator implements Generator<ArrayList<String>> {
    private static final List<String> WORDS = List.of(
            "cat", "dog", "tiger", "alan", "ben", "cindy", "danny", "evan",
            "Evelyne", "book", "moon", "loop", "abc", "a", "Anna", "xYz");
    private static final int MAX_WORD_COUNT = 7;

    @Override
    public ArrayList<String> next(Random random) {
        ArrayList<String> words = new ArrayList<>();
        int count = 1 + random.nextInt(MAX_WORD_COUNT);

        for (int index = 0; index < count; index++) {
            String word = WORDS.get(random.nextInt(WORDS.size()));
            if (random.nextBoolean() && !word.isEmpty()) {
                word = word + word.charAt(0);
            }
            words.add(word);
        }

        return words;
    }
}
