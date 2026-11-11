package autograder.roster;

import autograder.csv.CsvCodec;
import autograder.model.ScoreSheet;
import autograder.model.StudentRecord;
import autograder.util.ExceptionMessages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads the roster score sheet CSV into a ScoreSheet using the configured column names. */
public final class ScoreSheetRepository {
    private static final List<String> REQUIRED_SEMANTIC_COLUMNS = List.of(
            "id", "username", "lastName", "firstName", "email",
            "numerator", "denominator", "end");

    private final Path path;
    private final Map<String, String> columns;

    public ScoreSheetRepository(Path path, Map<String, String> columns) {
        this.path = path;
        this.columns = new LinkedHashMap<>(columns);
    }

    public ScoreSheet load() throws IOException {
        Path sourcePath;
        String text;
        try {
            sourcePath = path.toRealPath();
            text = Files.readString(sourcePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw ExceptionMessages.unreadableFile(path, exception);
        }
        boolean byteOrderMark = !text.isEmpty() && text.charAt(0) == '\uFEFF';
        if (byteOrderMark) {
            text = text.substring(1);
        }

        List<List<String>> parsedRows = CsvCodec.parse(text);
        if (parsedRows.isEmpty()) {
            throw new IOException("score sheet is empty");
        }
        List<String> headers = parsedRows.get(0);
        Map<String, Integer> columnIndexes = findRequiredColumns(headers);
        List<List<String>> rows = new ArrayList<>();
        List<StudentRecord> students = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> usernames = new HashSet<>();
        Set<String> emails = new HashSet<>();

        for (int rowIndex = 1; rowIndex < parsedRows.size(); rowIndex++) {
            List<String> row = parsedRows.get(rowIndex);
            int csvLine = rowIndex + 1;
            if (row.size() != headers.size()) {
                throw new IOException("score sheet row " + csvLine
                        + " has " + row.size() + " fields; expected " + headers.size());
            }
            rows.add(row);

            students.add(readStudent(row, columnIndexes, csvLine, ids, usernames, emails));
        }
        if (students.isEmpty()) {
            throw new IOException("score sheet has no student rows");
        }

        return new ScoreSheet(sourcePath, byteOrderMark, headers, rows, students, columns);
    }

    private StudentRecord readStudent(List<String> row, Map<String, Integer> columnIndexes,
            int csvLine, Set<String> ids, Set<String> usernames, Set<String> emails)
            throws IOException {
        String id = value(row, columnIndexes, "id");
        String username = value(row, columnIndexes, "username");
        String firstName = value(row, columnIndexes, "firstName");
        String lastName = value(row, columnIndexes, "lastName");
        String email = value(row, columnIndexes, "email");
        requireNonempty(id, "id", csvLine);
        requireNonempty(username, "username", csvLine);
        requireNonempty(email, "email", csvLine);
        addUnique(ids, id.trim(), "id", csvLine);
        addUnique(usernames, username.trim().toLowerCase(Locale.ROOT), "username", csvLine);
        addUnique(emails, email.trim().toLowerCase(Locale.ROOT), "email", csvLine);
        validateDenominator(value(row, columnIndexes, "denominator"), csvLine);
        return new StudentRecord(id, username, firstName, lastName, email);
    }

    private Map<String, Integer> findRequiredColumns(List<String> headers) throws IOException {
        Set<String> configuredHeaders = new HashSet<>();
        for (String semanticColumn : REQUIRED_SEMANTIC_COLUMNS) {
            String header = columns.get(semanticColumn);
            if (header == null || header.isEmpty()) {
                throw new IOException("missing configured score sheet column: " + semanticColumn);
            }
            if (!configuredHeaders.add(header)) {
                throw new IOException("configured score sheet columns must be distinct: " + header);
            }
        }

        Map<String, Integer> indexes = new HashMap<>();
        for (String semanticColumn : REQUIRED_SEMANTIC_COLUMNS) {
            String expectedHeader = columns.get(semanticColumn);
            int foundIndex = -1;
            for (int index = 0; index < headers.size(); index++) {
                if (headers.get(index).equals(expectedHeader)) {
                    if (foundIndex >= 0) {
                        throw new IOException("score sheet contains duplicate configured header: "
                                + expectedHeader);
                    }
                    foundIndex = index;
                }
            }
            if (foundIndex < 0) {
                throw new IOException("score sheet is missing configured header: " + expectedHeader);
            }
            indexes.put(semanticColumn, foundIndex);
        }
        return indexes;
    }

    private String value(List<String> row, Map<String, Integer> indexes, String semanticColumn) {
        return row.get(indexes.get(semanticColumn));
    }

    private void requireNonempty(String value, String field, int csvLine) throws IOException {
        if (value.isBlank()) {
            throw new IOException("score sheet row " + csvLine + " has an empty " + field);
        }
    }

    private void addUnique(Set<String> values, String value, String field, int csvLine)
            throws IOException {
        if (!values.add(value)) {
            throw new IOException("score sheet row " + csvLine + " duplicates " + field);
        }
    }

    private void validateDenominator(String rawValue, int csvLine) throws IOException {
        try {
            double denominator = Double.parseDouble(rawValue.trim());
            if (!Double.isFinite(denominator) || denominator <= 0.0) {
                throw new IOException("score sheet row " + csvLine
                        + " has a nonpositive or nonfinite denominator");
            }
        } catch (NumberFormatException exception) {
            throw new IOException("score sheet row " + csvLine + " has an invalid denominator", exception);
        }
    }
}
