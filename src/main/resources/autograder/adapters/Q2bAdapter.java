import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Q2bAdapter extends Q2b {
    public static void main(String[] args) {
        Path resultPath = args.length == 2 ? Path.of(args[1]) : null;
        String status = "ERROR";
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("expected case number and result path");
            }
            int testCase = Integer.parseInt(args[0]);
            String fileName;
            String courseName;
            String expected = null;
            if (testCase == 1) {
                fileName = "is442.txt";
                courseName = "IS101";
            } else if (testCase == 2) {
                fileName = "studentstester.txt";
                courseName = "SMU101";
            } else if (testCase == 3) {
                fileName = "studentstester.txt";
                courseName = "CS101";
                expected = "LEE Teck Yang-4.0";
            } else if (testCase == 4) {
                fileName = "studentstester.txt";
                courseName = "CS102";
                expected = "Jason LEE-4.0";
            } else if (testCase == 5) {
                fileName = "studentstester.txt";
                courseName = "CS103";
                expected = "TAY Evelyn-4.0";
            } else {
                throw new IllegalArgumentException("unsupported case number: " + testCase);
            }
            try {
                String result = getTopStudent(fileName, courseName);
                status = expected != null && expected.equals(result) ? "PASSED" : "FAILED";
            } catch (DataException expectedException) {
                if (testCase <= 2) {
                    status = "PASSED";
                } else {
                    expectedException.printStackTrace(System.err);
                    status = "ERROR";
                }
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
