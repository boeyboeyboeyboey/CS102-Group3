package autograder.autotester.question;

import java.io.Serializable;

public record ShapeInput(String kind, String id, double firstDimension, double secondDimension)
        implements Serializable {
    public ShapeInput {
        if (!(kind.equals("circle") || kind.equals("rectangle")) || id == null || id.isBlank()) {
            throw new IllegalArgumentException("shape input requires a supported kind and non-empty id");
        }
        if (!Double.isFinite(firstDimension) || firstDimension <= 0
                || kind.equals("rectangle") && (!Double.isFinite(secondDimension) || secondDimension <= 0)) {
            throw new IllegalArgumentException("shape dimensions must be finite and positive");
        }
    }

    public double area() {
        if (kind.equals("circle")) return Math.round(3.14 * firstDimension * firstDimension);
        return Math.round(firstDimension * secondDimension);
    }

    public double perimeter() {
        if (kind.equals("circle")) return Math.round(6.28 * firstDimension);
        return Math.round(2 * (firstDimension + secondDimension));
    }

    public String description() {
        return id + "=>" + area() + ":" + perimeter();
    }

    public Object[] runnerValue() {
        return new Object[] {kind, id, firstDimension, secondDimension};
    }
}
