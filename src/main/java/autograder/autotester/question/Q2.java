package autograder.autotester.question;

import autograder.autotester.generator.RandomIntArray;
import java.util.List;

/** Randomized question: sum of an int array. */
public final class Q2 extends Question {
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 10;
    private static final int MIN_VALUE = -50;
    private static final int MAX_VALUE = 50;

    public Q2() {
        signature("Q2", "sum", int.class);
        describe("Return the sum of all integers in the array.");
        addParameter("values", int[].class);
        addTestCase("mixed-values", new RandomIntArray(MIN_LENGTH, MAX_LENGTH, MIN_VALUE, MAX_VALUE));
        addTestCase("empty-array", new RandomIntArray(0, 0, 0, 0));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        int total = 0;
        for (int value : (int[]) arguments.get(0)) {
            total += value;
        }
        return total;
    }
}
