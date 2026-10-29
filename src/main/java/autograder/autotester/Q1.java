package autograder.autotester;

import java.util.List;

public final class Q1 extends Question {
    public Q1() {
        signature("Q1", "isEven", boolean.class);
        describe("Return true when the integer is even.");
        param("n", int.class);
        addTestCase("small", new RandomInt(-100, 100));
        addTestCase("wide", new RandomInt(-1_000_000, 1_000_000));
        addTestCase("zero-boundary", new RandomInt(-3, 3));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        return ((Integer) arguments.get(0)) % 2 == 0;
    }
}
