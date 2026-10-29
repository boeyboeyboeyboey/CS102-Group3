package autograder.autotester;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class Q3 extends Question {
    public Q3() {
        staticMethod();
        signature("Q3", "sortShapes", List.class, "List<Shape>");
        expectedSourceType("List<String>");
        describe("Keep shapes whose area is at most 1000, then sort by descending area and descending perimeter.");
        param("shapeList", List.class, "List<Shape>");
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
    public Object answerKey(List<Object> arguments) {
        List<ShapeInput> inputs = (List<ShapeInput>) arguments.get(0);
        List<ShapeInput> eligible = inputs.stream().filter(shape -> shape.area() <= 1_000)
                .sorted(Comparator.comparingDouble(ShapeInput::area).reversed()
                        .thenComparing(Comparator.comparingDouble(ShapeInput::perimeter).reversed()))
                .toList();
        List<String> expected = new ArrayList<>();
        for (ShapeInput shape : eligible) expected.add(shape.description());
        return expected;
    }

    private static ArrayList<ShapeInput> randomShapes(Random random) {
        ArrayList<ShapeInput> shapes = new ArrayList<>();
        int count = 3 + random.nextInt(5);
        shapes.add(rectangle("OVER", 1001 + random.nextInt(100), 2));
        for (int index = 1; index < count; index++) {
            String id = "S" + index;
            if (index % 2 == 0) {
                shapes.add(new ShapeInput("circle", id, 2 + index, 0));
            } else {
                shapes.add(rectangle(id, 3 + index * 2, 2 + index));
            }
        }
        return shapes;
    }

    private static ShapeInput rectangle(String id, double length, double breadth) {
        return new ShapeInput("rectangle", id, length, breadth);
    }
}
