package autograder.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxWorkbookWriterTest {
    @TempDir
    Path root;

    private Document part(Path workbook, String name) throws Exception {
        try (ZipFile zip = new ZipFile(workbook.toFile())) {
            byte[] bytes = zip.getInputStream(zip.getEntry(name)).readAllBytes();
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        }
    }

    private Path write(List<XlsxWorkbookWriter.Sheet> sheets) throws Exception {
        Path target = root.resolve("out.xlsx");
        XlsxWorkbookWriter.write(target, sheets);
        return target;
    }

    private Element cell(Path workbook, int index) throws Exception {
        return (Element) part(workbook, "xl/worksheets/sheet1.xml").getElementsByTagName("c").item(index);
    }

    private static XlsxWorkbookWriter.Sheet oneCell(String value) {
        return new XlsxWorkbookWriter.Sheet("A", List.of(List.of("Header"), List.of(value)));
    }

    @Test
    void writesOneSheetPerTable() throws Exception {
        Path workbook = write(List.of(
                new XlsxWorkbookWriter.Sheet("Scores", List.of(List.of("Name"), List.of("chee"))),
                new XlsxWorkbookWriter.Sheet("Case results", List.of(List.of("Case")))));

        var sheets = part(workbook, "xl/workbook.xml").getElementsByTagName("sheet");
        assertEquals(2, sheets.getLength());
        assertEquals("Case results", ((Element) sheets.item(1)).getAttribute("name"));
        assertEquals("chee", cell(workbook, 1).getTextContent());
        assertEquals("A2", cell(workbook, 1).getAttribute("r"));
        assertFalse(Files.exists(root.resolve("out.xlsx.part")));
    }

    @Test
    void numbersAreNumericButIdentifiersKeepTheirDigits() throws Exception {
        assertEquals("", cell(write(List.of(oneCell("9.88"))), 1).getAttribute("t"));
        assertEquals("inlineStr", cell(write(List.of(oneCell("0012"))), 1).getAttribute("t"));
    }

    @Test
    void formulaTextStaysText() throws Exception {
        Element cell = cell(write(List.of(oneCell("=HYPERLINK(\"http://x\")"))), 1);
        assertEquals("inlineStr", cell.getAttribute("t"));
        assertEquals(0, cell.getElementsByTagName("f").getLength());
    }

    @Test
    void hostileStudentOutputStaysReadable() throws Exception {
        Path workbook = write(List.of(oneCell("a\u0000b<c>&\uD800d")));
        assertEquals("ab<c>&d", cell(workbook, 1).getTextContent());
    }

    @Test
    void longValuesAreCutToExcelsCellLimit() throws Exception {
        String text = cell(write(List.of(oneCell("x".repeat(70_000)))), 1).getTextContent();
        assertTrue(text.length() <= 32_767);
        assertTrue(text.endsWith("see the CSV report]"));
    }

    @Test
    void rejectsSheetNamesExcelCannotOpen() {
        List<List<String>> rows = List.of(List.of("A"));
        assertThrows(IllegalArgumentException.class, () -> write(List.of(new XlsxWorkbookWriter.Sheet("a/b", rows))));
        assertThrows(IllegalArgumentException.class, () -> write(List.of(new XlsxWorkbookWriter.Sheet("x", rows),
                new XlsxWorkbookWriter.Sheet("X", rows))));
    }
}
