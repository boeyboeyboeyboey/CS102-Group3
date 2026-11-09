package autograder.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTableModelTest {
    @TempDir
    Path root;

    private CsvTableModel load(String text) throws Exception {
        return CsvTableModel.load(Files.writeString(root.resolve("report.csv"), text));
    }

    @Test
    void firstRowNamesTheColumns() throws Exception {
        CsvTableModel model = load("Student,Score\r\nchee,11.5\r\nping,20.00");
        assertEquals(2, model.getColumnCount());
        assertEquals("Score", model.getColumnName(1));
        assertEquals(2, model.getRowCount());
        assertEquals("20.00", model.getValueAt(1, 1));
    }

    @Test
    void headerOnlyReportHasNoRows() throws Exception {
        assertEquals(0, load("Check,Message").getRowCount());
    }

    @Test
    void shortRowsReadAsBlank() throws Exception {
        assertEquals("", load("A,B\r\nonly").getValueAt(0, 1));
    }

    @Test
    void detailsKeepMultiLineValuesInFull() throws Exception {
        String details = load("Pair,Evidence\r\nx,\"line 1\nline 2\"").details(0);
        assertTrue(details.contains("Evidence: line 1\nline 2"), details);
    }

    @Test
    void numbersSortByValueAndBeforeText() {
        List<String> values = new ArrayList<>(List.of("abc", "11.5", "9.88", "0"));
        values.sort(CsvTableModel.VALUE_ORDER);
        assertEquals(List.of("0", "9.88", "11.5", "abc"), values);
    }
}
