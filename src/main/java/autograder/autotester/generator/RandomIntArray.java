package autograder.autotester.generator;

import java.util.Random;

/** Generates int arrays with a random length and random values within inclusive bounds. */
public final class RandomIntArray implements Generator<int[]> {
    private static final int MAX_ARRAY_LENGTH = 100_000;
    private final int minimumLength;
    private final int maximumLength;
    private final int minimumValue;
    private final int maximumValue;

    public RandomIntArray(int minimumLength, int maximumLength, int minimumValue, int maximumValue) {
        if (minimumLength < 0 || minimumLength > maximumLength || maximumLength > MAX_ARRAY_LENGTH
                || minimumValue > maximumValue) {
            throw new IllegalArgumentException("invalid array generator bounds");
        }
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }

    @Override
    public int[] next(Random random) {
        long valueWidth = (long) maximumValue - minimumValue + 1;
        int length = minimumLength + random.nextInt(maximumLength - minimumLength + 1);
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = (int) (minimumValue + random.nextLong(valueWidth));
        }
        return values;
    }
}
