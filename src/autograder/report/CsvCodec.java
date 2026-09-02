package autograder.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Minimal RFC 4180-style CSV reader and atomic writer. */
public final class CsvCodec {
    public CsvDocument read(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        List<List<String>> records = parse(content);
        if (records.isEmpty()) {
            throw new IOException("CSV file is empty: " + path);
        }
        return new CsvDocument(records.get(0), records.subList(1, records.size()));
    }

    public void write(Path path, CsvDocument document) throws IOException {
        Path parent = path.toAbsolutePath().normalize().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        StringBuilder output = new StringBuilder();
        appendRecord(output, document.getHeader());
        for (List<String> row : document.getRows()) {
            appendRecord(output, row);
        }
        Files.writeString(temporary, output.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<List<String>> parse(String content) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == '"' && field.length() == 0) {
                quoted = true;
            } else if (current == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (current == '\n' || current == '\r') {
                row.add(field.toString());
                field.setLength(0);
                records.add(row);
                row = new ArrayList<>();
                if (current == '\r' && index + 1 < content.length()
                        && content.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IOException("CSV contains an unterminated quoted field");
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            records.add(row);
        }
        return records;
    }

    private void appendRecord(StringBuilder output, List<String> record) {
        for (int index = 0; index < record.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            output.append(escape(record.get(index)));
        }
        output.append(System.lineSeparator());
    }

    private String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
