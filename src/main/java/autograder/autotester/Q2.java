package autograder.autotester;

import java.util.List;

public final class Q2 extends Question {
    public Q2() {
        signature("Q2", "sum", int.class);
        describe("Return the sum of all integers in the array.");
        param("values", int[].class);
        addTestCase("mixed-values", new RandomIntArray(1, 10, -50, 50));
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
