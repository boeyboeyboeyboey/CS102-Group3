package autograder.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The instructor score sheet CSV: its headers, rows, students and column mapping. */
public final class ScoreSheet {
    private final Path sourcePath;
    private final boolean byteOrderMark;
    private final List<String> headers;
    private final List<List<String>> rows;
    private final List<StudentRecord> students;
    private final Map<String, String> columns;

    public ScoreSheet(Path sourcePath, boolean byteOrderMark, List<String> headers,
                      List<List<String>> rows, List<StudentRecord> students,
                      Map<String, String> columns) {
        this.sourcePath = sourcePath;
        this.byteOrderMark = byteOrderMark;
        this.headers = Collections.unmodifiableList(new ArrayList<>(headers));
        List<List<String>> rowCopies = new ArrayList<>();
        for (List<String> row : rows) {
            rowCopies.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.rows = Collections.unmodifiableList(rowCopies);
        this.students = Collections.unmodifiableList(new ArrayList<>(students));
        this.columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public boolean hasByteOrderMark() {
        return byteOrderMark;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public List<StudentRecord> getStudents() {
        return students;
    }

    public Map<String, String> getColumns() {
        return columns;
    }
}
