package autograder.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory CSV document that preserves input column order. */
public final class CsvDocument {
    private final List<String> header;
    private final List<List<String>> rows;

    public CsvDocument(List<String> header, List<List<String>> rows) {
        this.header = Collections.unmodifiableList(new ArrayList<>(header));
        List<List<String>> copiedRows = new ArrayList<>();
        for (List<String> row : rows) {
            copiedRows.add(new ArrayList<>(row));
        }
        this.rows = copiedRows;
    }

    public List<String> getHeader() {
        return header;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public int columnIndex(String name) {
        int index = header.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("CSV column is missing: " + name);
        }
        return index;
    }
}
