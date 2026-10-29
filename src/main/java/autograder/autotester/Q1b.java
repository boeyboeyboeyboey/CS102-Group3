package autograder.autotester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Q1b extends Question {
    public Q1b() {
        staticMethod();
        signature("Q1b", "getSumOfEvenIntegers", int.class);
        describe("Return the sum of even Integer elements and ignore all other list values.");
        param("inputs", ArrayList.class, "ArrayList<Object>");
        addTestCase("mixed-values", Q1b::randomMixedValues);
        addTestCase("no-integers", random -> new ArrayList<>(List.of(true, 4.5, "two")));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        int total = 0;
        for (Object value : (ArrayList<?>) arguments.get(0)) {
            if (value instanceof Integer number && number % 2 == 0) total += number;
        }
        return total;
    }

    private static ArrayList<Object> randomMixedValues(Random random) {
        ArrayList<Object> values = new ArrayList<>();
        int count = 1 + random.nextInt(10);
        for (int index = 0; index < count; index++) {
            switch (random.nextInt(4)) {
                case 0 -> values.add(random.nextInt(101) - 50);
                case 1 -> values.add(random.nextBoolean());
                case 2 -> values.add(random.nextDouble() * 100);
                default -> values.add("text" + random.nextInt(100));
            }
        }
        return values;
    }
}
