package autograder.plagiarism;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds token n-grams and records where each window first appears, for evidence. */
public final class NGramBuilder {
    private final int size;

    public NGramBuilder(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("n-gram size must be at least 1");
        }
        this.size = size;
    }

    public Map<String, LineRange> build(String file, List<Token> tokens) {
        Map<String, LineRange> grams = new LinkedHashMap<>();
        for (int start = 0; start + size <= tokens.size(); start++) {
            StringBuilder gram = new StringBuilder();
            for (int offset = 0; offset < size; offset++) {
                if (offset > 0) {
                    gram.append(' ');
                }
                gram.append(tokens.get(start + offset).text());
            }
            LineRange location = new LineRange(file, tokens.get(start).line(),
                    tokens.get(start + size - 1).line());
            grams.putIfAbsent(gram.toString(), location);
        }
        return grams;
    }
}
