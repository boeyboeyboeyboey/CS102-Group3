import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Q3Adapter extends Q3 {
    public static void main(String[] args) {
        Path resultPath = args.length == 2 ? Path.of(args[1]) : null;
        String status = "ERROR";
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("expected case number and result path");
            }
            int testCase = Integer.parseInt(args[0]);
            ArrayList<Shape> inputs = new ArrayList<>();
            String expected;
            if (testCase == 1) {
                inputs.add(new Rectangle("R1", 10, 2));
                inputs.add(new Rectangle("R2", 5, 3));
                inputs.add(new Rectangle("R3", 20, 3));
                inputs.add(new Rectangle("R4", 500, 3));
                expected = "[R3=>60.0:46.0, R1=>20.0:24.0, R2=>15.0:16.0]";
            } else if (testCase == 2) {
                inputs.add(new Rectangle("R1", 100, 2));
                inputs.add(new Rectangle("R2", 200, 1));
                inputs.add(new Rectangle("R3", 4, 50));
                inputs.add(new Rectangle("R4", 40, 5));
                expected = "[R2=>200.0:402.0, R1=>200.0:204.0, R3=>200.0:108.0, R4=>200.0:90.0]";
            } else if (testCase == 3) {
                inputs.add(new Circle("C1", 500));
                inputs.add(new Circle("C2", 3));
                inputs.add(new Rectangle("R1", 10000, 2));
                inputs.add(new Rectangle("R2", 5, 3));
                expected = "[C2=>28.0:19.0, R2=>15.0:16.0]";
            } else if (testCase == 4) {
                inputs.add(new Circle("C1", 5));
                inputs.add(new Circle("C2", 3));
                inputs.add(new Rectangle("R2", 8, 3));
                inputs.add(new Rectangle("R1", 12, 2));
                inputs.add(new Rectangle("R3", 4, 6));
                expected = "[C1=>79.0:31.0, C2=>28.0:19.0, R1=>24.0:28.0, R2=>24.0:22.0, R3=>24.0:20.0]";
            } else {
                throw new IllegalArgumentException("unsupported case number: " + testCase);
            }
            try {
                List<Shape> result = sortShapes(inputs);
                status = expected.equals(result.toString()) ? "PASSED" : "FAILED";
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
                status = "ERROR";
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
        writeResult(resultPath, status);
    }

    private static void writeResult(Path resultPath, String status) {
        if (resultPath == null) {
            return;
        }
        try {
            Files.writeString(resultPath, status, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
