package autograder.autotester.question;

import autograder.autotester.generator.RandomInt;
import java.util.List;

/** Randomized question: whether an integer is even. */
public final class Q1 extends Question {
    private static final int SMALL_BOUND = 100;
    private static final int WIDE_BOUND = 1_000_000;
    private static final int ZERO_BOUNDARY_BOUND = 3;

    public Q1() {
        signature("Q1", "isEven", boolean.class);
        describe("Return true when the integer is even.");
        addParameter("n", int.class);
        addTestCase("small", new RandomInt(-SMALL_BOUND, SMALL_BOUND));
        addTestCase("wide", new RandomInt(-WIDE_BOUND, WIDE_BOUND));
        addTestCase("zero-boundary", new RandomInt(-ZERO_BOUNDARY_BOUND, ZERO_BOUNDARY_BOUND));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        return ((Integer) arguments.get(0)) % 2 == 0;
    }
}
