import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Q2aAdapter extends Q2a {
    public static void main(String[] args) {
        Path resultPath = args.length == 2 ? Path.of(args[1]) : null;
        String status = "ERROR";
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("expected case number and result path");
            }
            int testCase = Integer.parseInt(args[0]);
            String fileName;
            String surname;
            double expected;
            if (testCase == 1) {
                fileName = "is442.txt";
                surname = "LEE";
                expected = -1.0;
            } else if (testCase == 2) {
                fileName = "personstester.txt";
                surname = "TAY";
                expected = 30.0;
            } else if (testCase == 3) {
                fileName = "personstester.txt";
                surname = "LEE";
                expected = 37.0;
            } else if (testCase == 4) {
                fileName = "personstester.txt";
                surname = "Wong";
                expected = 26.0;
            } else if (testCase == 5) {
                fileName = "personstester.txt";
                surname = "Ong";
                expected = 0.0;
            } else {
                throw new IllegalArgumentException("unsupported case number: " + testCase);
            }
            try {
                double result = getAverageAge(fileName, surname);
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
