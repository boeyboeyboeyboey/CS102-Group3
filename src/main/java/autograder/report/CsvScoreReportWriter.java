package autograder.report;

import autograder.csv.CsvCodec;
import autograder.model.ScoreSheet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CsvScoreReportWriter {
    public void write(ScoreSheet sheet, Map<String, Double> scoresByStudentId, Path output)
            throws IOException {
        if (sheet == null || scoresByStudentId == null || output == null) {
            throw new IllegalArgumentException("score sheet, scores, and output path are required");
        }

        Path outputPath = output.toAbsolutePath().normalize();
        Path parent = outputPath.getParent();
        if (parent == null) {
            throw new IOException("output path must have a parent directory");
        }
        Files.createDirectories(parent);
        parent = parent.toRealPath();
        outputPath = parent.resolve(outputPath.getFileName());
        rejectSourceOverwrite(sheet, outputPath);

        List<String> headers = sheet.getHeaders();
        List<List<String>> originalRows = sheet.getRows();
        int numeratorIndex = findHeaderIndex(headers, sheet.getColumns().get("numerator"));
        Map<String, Integer> studentRows = new HashMap<>();
        int idIndex = findHeaderIndex(headers, sheet.getColumns().get("id"));
        for (int rowIndex = 0; rowIndex < originalRows.size(); rowIndex++) {
            studentRows.put(originalRows.get(rowIndex).get(idIndex), rowIndex);
        }

        List<List<String>> outputRows = new ArrayList<>();
        outputRows.add(new ArrayList<>(headers));
        for (List<String> originalRow : originalRows) {
            outputRows.add(new ArrayList<>(originalRow));
        }

        for (Map.Entry<String, Double> scoreEntry : scoresByStudentId.entrySet()) {
            String studentId = scoreEntry.getKey();
            Double score = scoreEntry.getValue();
            Integer rowIndex = studentRows.get(studentId);
            if (rowIndex == null) {
                throw new IOException("score supplied for unknown student id: " + studentId);
            }
            if (score == null || !Double.isFinite(score) || score < 0.0) {
                throw new IOException("score for student " + studentId
                        + " must be finite and nonnegative");
            }
            List<String> row = outputRows.get(rowIndex + 1);
            double denominator = parseDenominator(row, findHeaderIndex(
                    headers, sheet.getColumns().get("denominator")), studentId);
            if (score > denominator) {
                throw new IOException("score for student " + studentId
                        + " exceeds the configured denominator");
            }
            row.set(numeratorIndex, BigDecimal.valueOf(score)
                    .setScale(2, RoundingMode.HALF_UP).toPlainString());
        }

        String content = CsvCodec.write(outputRows);
        if (sheet.hasByteOrderMark()) {
            content = '\uFEFF' + content;
        }
        writeAtomically(outputPath, content);
    }

    private int findHeaderIndex(List<String> headers, String name) throws IOException {
        if (name == null) {
            throw new IOException("score sheet column mapping is missing");
        }
        int foundIndex = -1;
        for (int index = 0; index < headers.size(); index++) {
            if (headers.get(index).equals(name)) {
                if (foundIndex >= 0) {
                    throw new IOException("score sheet contains duplicate configured header: " + name);
                }
                foundIndex = index;
            }
        }
        if (foundIndex < 0) {
            throw new IOException("score sheet is missing configured header: " + name);
        }
        return foundIndex;
    }

    private double parseDenominator(List<String> row, int denominatorIndex, String studentId)
            throws IOException {
        try {
            double denominator = Double.parseDouble(row.get(denominatorIndex).trim());
            if (!Double.isFinite(denominator) || denominator <= 0.0) {
                throw new IOException("denominator for student " + studentId + " is invalid");
            }
            return denominator;
        } catch (NumberFormatException exception) {
            throw new IOException("denominator for student " + studentId + " is invalid", exception);
        }
    }

    private void rejectSourceOverwrite(ScoreSheet sheet, Path outputPath) throws IOException {
        Path sourcePath = sheet.getSourcePath();
        if (sourcePath == null) {
            throw new IOException("score sheet source path is not available");
        }
        if (outputPath.equals(sourcePath.toAbsolutePath().normalize())) {
            throw new IOException("output path cannot overwrite the source score sheet");
        }
        if (Files.isSymbolicLink(outputPath)) {
            throw new IOException("output path cannot be a symbolic link");
        }
        if (Files.exists(outputPath, LinkOption.NOFOLLOW_LINKS)
                && Files.isSameFile(sourcePath, outputPath)) {
            throw new IOException("output path resolves to the source score sheet");
        }
    }

    private void writeAtomically(Path outputPath, String content) throws IOException {
        Path parent = outputPath.getParent();
        String fileName = outputPath.getFileName().toString();
        Path temporaryFile = Files.createTempFile(parent, fileName + "-", ".tmp");
        try {
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, outputPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
