package autograder.autotester.question;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Randomized question: sum of the even Integer elements in a mixed-type list. */
public final class Q1b extends Question {
    private static final List<Object> NO_INTEGER_VALUES = List.of(true, 4.5, "two");
    private static final int MAX_MIXED_VALUE_COUNT = 10;
    private static final int VALUE_KIND_COUNT = 4;
    private static final int INTEGER_BOUND = 101;
    private static final int INTEGER_OFFSET = 50;
    private static final int DOUBLE_SCALE = 100;
    private static final int TEXT_SUFFIX_BOUND = 100;

    public Q1b() {
        staticMethod();
        signature("Q1b", "getSumOfEvenIntegers", int.class);
        describe("Return the sum of even Integer elements and ignore all other list values.");
        addParameter("inputs", ArrayList.class, "ArrayList<Object>");
        addTestCase("mixed-values", Q1b::randomMixedValues);
        addTestCase("no-integers", random -> new ArrayList<>(NO_INTEGER_VALUES));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        int total = 0;
        for (Object value : (ArrayList<?>) arguments.get(0)) {
            if (value instanceof Integer number && number % 2 == 0) {
                total += number;
            }
        }
        return total;
    }

    private static ArrayList<Object> randomMixedValues(Random random) {
        ArrayList<Object> values = new ArrayList<>();
        int count = 1 + random.nextInt(MAX_MIXED_VALUE_COUNT);
        for (int index = 0; index < count; index++) {
            switch (random.nextInt(VALUE_KIND_COUNT)) {
                case 0 -> values.add(random.nextInt(INTEGER_BOUND) - INTEGER_OFFSET);
                case 1 -> values.add(random.nextBoolean());
                case 2 -> values.add(random.nextDouble() * DOUBLE_SCALE);
                default -> values.add("text" + random.nextInt(TEXT_SUFFIX_BOUND));
            }
        }
        return values;
    }
}
