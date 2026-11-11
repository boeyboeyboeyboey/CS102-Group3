package autograder.roster;

import autograder.csv.CsvCodec;
import autograder.model.ScoreSheet;
import autograder.model.StudentRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreSheetRepositoryTest {
    private static final String HEADER = "OrgDefinedId,Username,Last Name,First Name,Email,"
            + "Calculated Final Grade Numerator,Calculated Final Grade Denominator,"
            + "End-of-Line Indicator";

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsProvidedSchemaAndKeepsRawStudentFields() throws Exception {
        Path csv = writeCsv("\uFEFF" + HEADER + "\r\n"
                + "#01400001,#ping.lee.2023,_,PING LEE,ping.lee.2023@computing.smu.edu.sg,,20,#\r\n");

        ScoreSheet sheet = new ScoreSheetRepository(csv, columns()).load();
        StudentRecord student = sheet.getStudents().get(0);

        assertTrue(sheet.hasByteOrderMark());
        assertEquals(List.of("#01400001", "#ping.lee.2023", "_", "PING LEE",
                "ping.lee.2023@computing.smu.edu.sg", "", "20", "#"), sheet.getRows().get(0));
        assertEquals("#01400001", student.getId());
        assertEquals("#ping.lee.2023", student.getUsername());
        assertEquals("PING LEE", student.getFirstName());
        assertEquals("_", student.getLastName());
        assertEquals("ping.lee.2023@computing.smu.edu.sg", student.getEmail());
        assertEquals(HEADER.split(",", -1).length, sheet.getHeaders().size());
    }

    @Test
    void missingScoreSheetSaysFileNotFoundWithThePath() {
        Path missing = temporaryDirectory.resolve("absent.csv");

        IOException exception = assertThrows(IOException.class,
                () -> new ScoreSheetRepository(missing, columns()).load());

        assertEquals("file not found: " + missing, exception.getMessage());
    }

    @Test
    void unreadableScoreSheetSaysCannotReadFileWithThePath() throws Exception {
        Path invalid = temporaryDirectory.resolve("latin1.csv");
        Files.write(invalid, new byte[] {(byte) 0xC3, (byte) 0x28});

        IOException exception = assertThrows(IOException.class,
                () -> new ScoreSheetRepository(invalid, columns()).load());

        assertEquals("cannot read file: " + invalid, exception.getMessage());
    }

    @Test
    void parsesAndWritesEscapedCommasQuotesNewlinesAndTrailingEmptyFields() throws Exception {
        String source = "one,\"last, first\",\"said \"\"hello\"\"\",\"line 1\r\nline 2\",\r\n"
                + "two,,,value,,";

        List<List<String>> parsed = CsvCodec.parse(source);
        String written = CsvCodec.write(parsed);

        assertEquals(List.of("one", "last, first", "said \"hello\"", "line 1\r\nline 2", ""),
                parsed.get(0));
        assertEquals(List.of("two", "", "", "value", "", ""), parsed.get(1));
        assertEquals(parsed, CsvCodec.parse(written));
    }

    @Test
    void rejectsDuplicateRosterIdentitiesIgnoringUsernameAndEmailCase() throws Exception {
        Path csv = writeCsv(HEADER + "\n"
                + "id-1,user.one,Last,First,first@example.edu,,20,#\n"
                + "id-2,USER.ONE,Other,Person,other@example.edu,,20,#\n");

        IOException exception = assertThrows(IOException.class,
                () -> new ScoreSheetRepository(csv, columns()).load());

        assertTrue(exception.getMessage().contains("duplicates username"));
    }

    @Test
    void rejectsDuplicateConfiguredColumnsAndHeaderOnlyScoreSheets() throws Exception {
        Path csv = writeCsv(HEADER + "\n"
                + "id-1,user.one,Last,First,first@example.edu,,20,#\n");
        Map<String, String> duplicateColumns = new HashMap<>(columns());
        duplicateColumns.put("username", "OrgDefinedId");

        IOException duplicateException = assertThrows(IOException.class,
                () -> new ScoreSheetRepository(csv, duplicateColumns).load());
        assertTrue(duplicateException.getMessage().contains("must be distinct"));

        Path headerOnly = writeCsv(HEADER + "\n");
        IOException emptyRosterException = assertThrows(IOException.class,
                () -> new ScoreSheetRepository(headerOnly, columns()).load());
        assertTrue(emptyRosterException.getMessage().contains("no student rows"));
    }

    @Test
    void rejectsEmailDuplicatesMalformedRowsAndInvalidDenominators() throws Exception {
        Path duplicateEmail = writeCsv(HEADER + "\n"
                + "id-1,user.one,Last,First,first@example.edu,,20,#\n"
                + "id-2,user.two,Other,Person,FIRST@example.edu,,20,#\n");
        assertTrue(assertThrows(IOException.class,
                () -> new ScoreSheetRepository(duplicateEmail, columns()).load())
                .getMessage().contains("duplicates email"));

        Path shortRow = writeCsv(HEADER + "\n" + "id-1,user.one,Last,First,email,,20\n");
        assertTrue(assertThrows(IOException.class,
                () -> new ScoreSheetRepository(shortRow, columns()).load())
                .getMessage().contains("expected 8"));

        Path badDenominator = writeCsv(HEADER + "\n"
                + "id-1,user.one,Last,First,email,,NaN,#\n");
        assertTrue(assertThrows(IOException.class,
                () -> new ScoreSheetRepository(badDenominator, columns()).load())
                .getMessage().contains("denominator"));
    }

    @Test
    void ignoresBlankLinesAfterRowsButKeepsQuotedEmptyFields() throws Exception {
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")),
                CsvCodec.parse("a,b\r\nc,d\r\n\r\n\n"));
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")),
                CsvCodec.parse("a,b\nc,d\n\n"));
        assertEquals(List.of(List.of("a"), List.of(""), List.of("b")),
                CsvCodec.parse("a\n\"\"\nb\n"));
        assertEquals(List.of(List.of("a"), List.of(""), List.of("b")), CsvCodec.parse("a\n\nb\n"));
        assertEquals(List.of(List.of("a", ""), List.of("", "b")), CsvCodec.parse("a,\n,b\n"));

        Path csv = writeCsv(HEADER + "\r\n"
                + "id-1,user.one,Last,First,first@example.edu,,20,#\r\n\r\n");
        assertEquals(1, new ScoreSheetRepository(csv, columns()).load().getStudents().size());
    }

    @Test
    void rejectsMalformedCsvQuotes() throws Exception {
        assertThrows(IOException.class, () -> CsvCodec.parse("one,not\"valid\n"));
        assertThrows(IOException.class, () -> CsvCodec.parse("\"unterminated"));
        assertThrows(IOException.class, () -> CsvCodec.parse("\"closed\" trailing"));
    }

    private Path writeCsv(String content) throws IOException {
        Path csv = temporaryDirectory.resolve("scores.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    static Map<String, String> columns() {
        return Map.of(
                "id", "OrgDefinedId",
                "username", "Username",
                "lastName", "Last Name",
                "firstName", "First Name",
                "email", "Email",
                "numerator", "Calculated Final Grade Numerator",
                "denominator", "Calculated Final Grade Denominator",
                "end", "End-of-Line Indicator");
    }
}
