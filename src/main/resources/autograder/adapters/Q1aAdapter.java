import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Q1aAdapter extends Q1a {
    public static void main(String[] args) {
        Path resultPath = args.length == 2 ? Path.of(args[1]) : null;
        String status = "ERROR";
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("expected case number and result path");
            }
            status = evaluate(Integer.parseInt(args[0]));
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

    private static String evaluate(int testCase) {
        ArrayList<String> inputs = new ArrayList<>();
        ArrayList<String> expected;
        if (testCase == 1) {
            inputs.add("alice");
            inputs.add("ben");
            inputs.add("cathy");
            expected = new ArrayList<>(Arrays.asList("alice", "ben", "cathy"));
        } else if (testCase == 2) {
            inputs.add("ellen");
            inputs.add("alan");
            inputs.add("zoe");
            expected = new ArrayList<>(Arrays.asList("zoe"));
        } else if (testCase == 3) {
            inputs.add("Elle");
            inputs.add("alan");
            inputs.add("Daniel");
            inputs.add("Canny");
            expected = new ArrayList<>(Arrays.asList("Daniel"));
        } else {
            throw new IllegalArgumentException("unsupported case number: " + testCase);
        }
        try {
            ArrayList<String> result = getIsogramWords(inputs);
            return expected.equals(result) ? "PASSED" : "FAILED";
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            return "ERROR";
        }
    }
}
