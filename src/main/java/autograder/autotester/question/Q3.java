package autograder.autotester.question;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Randomized question: filter shapes by area and sort by descending area then perimeter. */
public final class Q3 extends Question {
    private static final double MAX_AREA = 1_000;
    private static final int MIN_SHAPE_COUNT = 3;
    private static final int SHAPE_COUNT_SPREAD = 5;
    private static final String OVER_SIZE_ID = "OVER";
    private static final int OVER_SIZE_LENGTH_BASE = 1001;
    private static final int OVER_SIZE_LENGTH_SPREAD = 100;
    private static final int OVER_SIZE_BREADTH = 2;
    private static final String SHAPE_ID_PREFIX = "S";
    private static final int CIRCLE_RADIUS_BASE = 2;
    private static final int RECTANGLE_LENGTH_BASE = 3;
    private static final int RECTANGLE_LENGTH_STEP = 2;
    private static final int RECTANGLE_BREADTH_BASE = 2;

    public Q3() {
        staticMethod();
        signature("Q3", "sortShapes", List.class, "List<Shape>");
        expectedSourceType("List<String>");
        describe("Keep shapes whose area is at most 1000, then sort by descending area and descending perimeter.");
        addParameter("shapeList", List.class, "List<Shape>");
        includeSupportSource("src/main/resources/autograder/autotester/labtest/Shape.java");
        includeSupportSource("src/main/resources/autograder/autotester/labtest/Circle.java");
        includeSupportSource("src/main/resources/autograder/autotester/labtest/Rectangle.java");
        includeStarterSource("Project/RenameToYourUsername/Q3/ShapeComparator.java");
        includeCompanionSource("ShapeComparator.java");
        compareByText();
        addTestCase("random-shapes", Q3::randomShapes);
        addTestCase("equal-area-order", random -> new ArrayList<>(List.of(
                rectangle("R1", 8, 3), rectangle("R2", 6, 4), rectangle("R3", 12, 2))));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object answerKey(List<Object> arguments) {
        List<ShapeInput> inputs = (List<ShapeInput>) arguments.get(0);
        List<ShapeInput> eligible = inputs.stream().filter(shape -> shape.area() <= MAX_AREA)
                .sorted(Comparator.comparingDouble(ShapeInput::area).reversed()
                        .thenComparing(Comparator.comparingDouble(ShapeInput::perimeter).reversed()))
                .toList();
        List<String> expected = new ArrayList<>();
        for (ShapeInput shape : eligible) {
            expected.add(shape.description());
        }
        return expected;
    }

    private static ArrayList<ShapeInput> randomShapes(Random random) {
        ArrayList<ShapeInput> shapes = new ArrayList<>();
        int count = MIN_SHAPE_COUNT + random.nextInt(SHAPE_COUNT_SPREAD);
        shapes.add(rectangle(OVER_SIZE_ID, OVER_SIZE_LENGTH_BASE + random.nextInt(OVER_SIZE_LENGTH_SPREAD),
                OVER_SIZE_BREADTH));
        for (int index = 1; index < count; index++) {
            String id = SHAPE_ID_PREFIX + index;
            if (index % 2 == 0) {
                shapes.add(new ShapeInput(ShapeInput.CIRCLE, id, CIRCLE_RADIUS_BASE + index, 0));
            } else {
                shapes.add(rectangle(id, RECTANGLE_LENGTH_BASE + index * RECTANGLE_LENGTH_STEP,
                        RECTANGLE_BREADTH_BASE + index));
            }
        }
        return shapes;
    }

    private static ShapeInput rectangle(String id, double length, double breadth) {
        return new ShapeInput(ShapeInput.RECTANGLE, id, length, breadth);
    }
}
