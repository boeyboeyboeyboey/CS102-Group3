package autograder.autotester;

import java.io.Serializable;
import java.util.Random;

@FunctionalInterface
public interface Generator<T extends Serializable> {
    T next(Random random);
}
