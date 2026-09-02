package autograder.report;

import autograder.model.StudentGrade;
import autograder.service.api.ReportGenerator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Preserves the supplied scoresheet and fills its final-grade numerator column. */
public final class CsvScoreSheetReportGenerator implements ReportGenerator {
    private final Path inputPath;
    private final Path outputPath;
    private final CsvCodec csvCodec;

    public CsvScoreSheetReportGenerator(Path inputPath, Path outputPath, CsvCodec csvCodec) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.csvCodec = csvCodec;
    }

    @Override
    public Path generate(List<StudentGrade> grades) throws IOException {
        CsvDocument document = csvCodec.read(inputPath);
        int usernameIndex = document.columnIndex("Username");
        int numeratorIndex = document.columnIndex("Calculated Final Grade Numerator");
        Map<String, StudentGrade> byUsername = new HashMap<>();
        for (StudentGrade grade : grades) {
            byUsername.put(grade.getStudent().getUsername().toLowerCase(Locale.ROOT), grade);
        }
        for (List<String> row : document.getRows()) {
            ensureSize(row, document.getHeader().size());
            String username = stripMarker(row.get(usernameIndex)).toLowerCase(Locale.ROOT);
            StudentGrade grade = byUsername.get(username);
            if (grade != null) {
                row.set(numeratorIndex, String.format(Locale.ROOT, "%.1f", grade.getTotalScore()));
            }
        }
        csvCodec.write(outputPath, document);
        return outputPath;
    }

    private void ensureSize(List<String> row, int size) {
        while (row.size() < size) {
            row.add("");
        }
    }

    private String stripMarker(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    }
}
