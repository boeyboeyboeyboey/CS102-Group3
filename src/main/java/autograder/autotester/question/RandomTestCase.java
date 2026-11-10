package autograder.autotester.question;

import autograder.autotester.generator.Generator;
import java.util.List;

/** A named test case with one generator per method argument. */
public record RandomTestCase(String id, List<Generator<?>> generators) {
    public RandomTestCase {
        generators = List.copyOf(generators);
    }
}
