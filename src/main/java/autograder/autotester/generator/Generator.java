package autograder.autotester.generator;

import java.io.Serializable;
import java.util.Random;

/** Produces one random argument value per call for a question test case. */
@FunctionalInterface
public interface Generator<T extends Serializable> {
    T next(Random random);
}
