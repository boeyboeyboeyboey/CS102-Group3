package autograder.autotester.question;

import autograder.autotester.generator.Generator;
import java.util.List;

public record RandomTestCase(String id, List<Generator<?>> generators) {
    public RandomTestCase {
        generators = List.copyOf(generators);
    }
}
