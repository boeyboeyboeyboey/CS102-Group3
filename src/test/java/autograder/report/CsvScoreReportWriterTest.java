package autograder.report;

import autograder.roster.ScoreSheetRepository;
import autograder.model.ScoreSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvScoreReportWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesSchemaRowsAndUnscoredValuesAndWritesOnlyExplicitScores() throws Exception {
        Path source = writeSource();
        ScoreSheet sheet = new ScoreSheetRepository(source, columns()).load();
        Path output = temporaryDirectory.resolve("output").resolve("scorereport.csv");

        new CsvScoreReportWriter().write(sheet,
                Map.of("#01400001", 15.277777777777777), output);

        String result = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(result.startsWith("\uFEFF"));
        ScoreSheet outputSheet = new ScoreSheetRepository(output, columns()).load();
        assertEquals(sheet.getHeaders(), outputSheet.getHeaders());
        assertEquals("15.28", outputSheet.getRows().get(0).get(5));
        assertEquals("9.0", outputSheet.getRows().get(1).get(5));
        assertEquals("#01400002", outputSheet.getRows().get(1).get(0));
        assertEquals("20", outputSheet.getRows().get(1).get(6));
        assertEquals("#", outputSheet.getRows().get(1).get(7));
    }

    @Test
    void rejectsSourceOverwriteAndDoesNotChangeTheTemplate() throws Exception {
        Path source = writeSource();
        String before = Files.readString(source, StandardCharsets.UTF_8);
        ScoreSheet sheet = new ScoreSheetRepository(source, columns()).load();

        assertThrows(IOException.class, () -> new CsvScoreReportWriter().write(
                sheet, Map.of("#01400001", 10.0), source));
        assertEquals(before, Files.readString(source, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsSymbolicLinkOutputToSource() throws Exception {
        Path source = writeSource();
        ScoreSheet sheet = new ScoreSheetRepository(source, columns()).load();
        Path link = temporaryDirectory.resolve("linked-output.csv");
        try {
            Files.createSymbolicLink(link, source);
        } catch (UnsupportedOperationException | IOException exception) {
            return;
        }

        assertThrows(IOException.class, () -> new CsvScoreReportWriter().write(
                sheet, Map.of("#01400001", 10.0), link));
        assertTrue(Files.isSymbolicLink(link));
    }

    @Test
    void rejectsUnknownNegativeNonfiniteAndOverMaximumScoresWithoutOutput() throws Exception {
        Path source = writeSource();
        ScoreSheet sheet = new ScoreSheetRepository(source, columns()).load();
        Path output = temporaryDirectory.resolve("bad-output.csv");
        CsvScoreReportWriter writer = new CsvScoreReportWriter();
        List<Map<String, Double>> invalidScores = List.of(
                Map.of("unknown", 1.0),
                Map.of("#01400001", -1.0),
                Map.of("#01400001", Double.NaN),
                Map.of("#01400001", 20.1));

        for (Map<String, Double> scores : invalidScores) {
            assertThrows(IOException.class, () -> writer.write(sheet, scores, output));
            assertFalse(Files.exists(output));
        }
    }

    private Path writeSource() throws IOException {
        Path source = temporaryDirectory.resolve("template.csv");
        Files.writeString(source, "\uFEFFOrgDefinedId,Username,Last Name,First Name,Email,"
                + "Calculated Final Grade Numerator,Calculated Final Grade Denominator,"
                + "End-of-Line Indicator\r\n"
                + "#01400001,#one.student,_,ONE STUDENT,one@example.edu,,20,#\r\n"
                + "#01400002,#two.student,_,\"TWO, STUDENT\",two@example.edu,9.0,20,#\r\n",
                StandardCharsets.UTF_8);
        return source;
    }

    private Map<String, String> columns() {
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
