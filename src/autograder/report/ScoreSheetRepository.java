package autograder.report;

import autograder.model.StudentRecord;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads roster identity fields from the supplied scoresheet. */
public final class ScoreSheetRepository {
    private final Path inputPath;
    private final CsvCodec csvCodec;

    public ScoreSheetRepository(Path inputPath, CsvCodec csvCodec) {
        this.inputPath = inputPath;
        this.csvCodec = csvCodec;
    }

    public List<StudentRecord> loadRoster() throws IOException {
        CsvDocument document = csvCodec.read(inputPath);
        int organizationId = document.columnIndex("OrgDefinedId");
        int username = document.columnIndex("Username");
        int displayName = document.columnIndex("First Name");
        int email = document.columnIndex("Email");
        List<StudentRecord> roster = new ArrayList<>();
        for (List<String> row : document.getRows()) {
            roster.add(new StudentRecord(value(row, organizationId), value(row, username),
                    value(row, displayName), value(row, email), row));
        }
        return roster;
    }

    private String value(List<String> row, int index) {
        return index < row.size() ? row.get(index) : "";
    }
}
