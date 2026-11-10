import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Q1bAdapter extends Q1b {
    public static void main(String[] args) {
        Path resultPath = args.length == 2 ? Path.of(args[1]) : null;
        String status = "ERROR";
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("expected case number and result path");
            }
            int testCase = Integer.parseInt(args[0]);
            ArrayList<Object> inputs = new ArrayList<>();
            int expected;
            if (testCase == 1) {
                inputs.add(2);
                inputs.add(4);
                expected = 6;
            } else if (testCase == 2) {
                inputs.add(2);
                inputs.add(false);
                inputs.add(4.0);
                inputs.add("two");
                expected = 2;
            } else if (testCase == 3) {
                inputs.add(9);
                inputs.add(2);
                inputs.add(10);
                inputs.add(3);
                inputs.add(5);
                inputs.add(8);
                expected = 20;
            } else {
                throw new IllegalArgumentException("unsupported case number: " + testCase);
            }
            try {
                int result = getSumOfEvenIntegers(inputs);
                status = expected == result ? "PASSED" : "FAILED";
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
