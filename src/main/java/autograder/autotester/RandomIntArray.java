package autograder.autotester;

import java.util.Random;

public final class RandomIntArray implements Generator<int[]> {
    private final int minimumLength;
    private final int maximumLength;
    private final int minimumValue;
    private final int maximumValue;

    public RandomIntArray(int minimumLength, int maximumLength, int minimumValue, int maximumValue) {
        if (minimumLength < 0 || minimumLength > maximumLength || maximumLength > 100_000
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
