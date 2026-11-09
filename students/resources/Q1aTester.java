import java.util.*;

public final class Q1aTester {
    public static void main(String[] args) {
        int passed = 0;
        int total = 3;
        try {
            Object actual = Q1a.getIsogramWords(new ArrayList<String>(java.util.Arrays.asList("Evelyne", "dog", "abca")));
            Object expected = new ArrayList<String>(java.util.Arrays.asList("dog"));
            if (Objects.deepEquals(expected, actual)) {
                passed++;
                System.out.println("PASS word-lists");
            } else {
                System.out.println("FAIL word-lists: expected " + expected + ", got " + actual);
            }
        } catch (Throwable failure) {
            System.out.println("FAIL word-lists: threw " + failure.getClass().getSimpleName());
        }
        try {
            Object actual = Q1a.getIsogramWords(new ArrayList<String>(java.util.Arrays.asList("loop", "benb", "bookb", "dogd", "alan")));
            Object expected = new ArrayList<String>(java.util.Arrays.asList());
            if (Objects.deepEquals(expected, actual)) {
                passed++;
                System.out.println("PASS word-lists");
            } else {
                System.out.println("FAIL word-lists: expected " + expected + ", got " + actual);
            }
        } catch (Throwable failure) {
            System.out.println("FAIL word-lists: threw " + failure.getClass().getSimpleName());
        }
        try {
            Object actual = Q1a.getIsogramWords(new ArrayList<String>(java.util.Arrays.asList()));
            Object expected = new ArrayList<String>(java.util.Arrays.asList());
            if (Objects.deepEquals(expected, actual)) {
                passed++;
                System.out.println("PASS empty-list");
            } else {
                System.out.println("FAIL empty-list: expected " + expected + ", got " + actual);
            }
        } catch (Throwable failure) {
            System.out.println("FAIL empty-list: threw " + failure.getClass().getSimpleName());
        }
        System.out.println("passed " + passed + " of " + total + " examples");
    }
}
