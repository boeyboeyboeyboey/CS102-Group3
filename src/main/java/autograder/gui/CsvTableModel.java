package autograder.gui;

import autograder.csv.CsvCodec;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** Read-only table of one CSV report: the first row names the columns. */
final class CsvTableModel extends AbstractTableModel {
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    /** Sorts numbers by value and before text, so 9.88 comes before 11.5. */
    static final Comparator<String> VALUE_ORDER = (first, second) -> {
        boolean firstNumber = NUMBER.matcher(first).matches();
        boolean secondNumber = NUMBER.matcher(second).matches();
        if (firstNumber && secondNumber) {
            return Double.compare(Double.parseDouble(first), Double.parseDouble(second));
        }
        return firstNumber != secondNumber ? (firstNumber ? -1 : 1) : first.compareTo(second);
    };

    private final List<String> columns;
    private final List<List<String>> rows;

    private CsvTableModel(List<String> columns, List<List<String>> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    static CsvTableModel load(Path file) throws IOException {
        List<List<String>> parsed = CsvCodec.parse(Files.readString(file, StandardCharsets.UTF_8));
        if (parsed.isEmpty()) {
            return new CsvTableModel(List.of(), List.of());
        }
        return new CsvTableModel(parsed.get(0), parsed.subList(1, parsed.size()));
    }

    /** Every column of one row in full, for values too long for a table cell. */
    String details(int row) {
        StringBuilder text = new StringBuilder();
        for (int column = 0; column < columns.size(); column++) {
            text.append(columns.get(column)).append(": ").append(getValueAt(row, column)).append('\n');
        }
        return text.toString();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column);
    }

    @Override
    public String getValueAt(int row, int column) {
        List<String> values = rows.get(row);
        return column < values.size() ? values.get(column) : "";
    }
}
