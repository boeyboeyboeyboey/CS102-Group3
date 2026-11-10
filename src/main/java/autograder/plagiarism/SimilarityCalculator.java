package autograder.plagiarism;

import java.util.HashSet;
import java.util.Set;

/** Jaccard similarity: shared n-grams divided by all distinct n-grams. */
public final class SimilarityCalculator {
    public double jaccard(Set<String> first, Set<String> second) {
        Set<String> union = new HashSet<>(first);
        union.addAll(second);
        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) sharedCount(first, second) / union.size();
    }

    public int sharedCount(Set<String> first, Set<String> second) {
        int shared = 0;
        for (String gram : first) {
            if (second.contains(gram)) {
                shared++;
            }
        }
        return shared;
    }
}
