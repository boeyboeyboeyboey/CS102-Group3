package autograder.autotester.generator;

import java.util.Random;

/** Generates an Integer uniformly between inclusive bounds. */
public final class RandomInt implements Generator<Integer> {
    private final int minimum;
    private final int maximum;

    public RandomInt(int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public Integer next(Random random) {
        long width = (long) maximum - minimum + 1;
        return (int) (minimum + random.nextLong(width));
    }
}
