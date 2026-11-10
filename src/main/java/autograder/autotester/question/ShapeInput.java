package autograder.autotester.question;

import java.io.Serializable;

/** Serializable circle or rectangle test input, with the area and perimeter rules the answer keys use. */
public record ShapeInput(String kind, String id, double firstDimension, double secondDimension)
        implements Serializable {
    public static final String CIRCLE = "circle";
    public static final String RECTANGLE = "rectangle";
    private static final double PI_APPROXIMATION = 3.14;
    private static final double TWO_PI_APPROXIMATION = 6.28;

    public ShapeInput {
        if (!(kind.equals(CIRCLE) || kind.equals(RECTANGLE)) || id == null || id.isBlank()) {
            throw new IllegalArgumentException("shape input requires a supported kind and non-empty id");
        }
        if (!Double.isFinite(firstDimension) || firstDimension <= 0
                || kind.equals(RECTANGLE) && (!Double.isFinite(secondDimension) || secondDimension <= 0)) {
            throw new IllegalArgumentException("shape dimensions must be finite and positive");
        }
    }

    public double area() {
        if (kind.equals(CIRCLE)) {
            return Math.round(PI_APPROXIMATION * firstDimension * firstDimension);
        }
        return Math.round(firstDimension * secondDimension);
    }

    public double perimeter() {
        if (kind.equals(CIRCLE)) {
            return Math.round(TWO_PI_APPROXIMATION * firstDimension);
        }
        return Math.round(2 * (firstDimension + secondDimension));
    }

    public String description() {
        return id + "=>" + area() + ":" + perimeter();
    }

    public Object[] runnerValue() {
        return new Object[] {kind, id, firstDimension, secondDimension};
    }
}
