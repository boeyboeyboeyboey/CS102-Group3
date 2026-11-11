package autograder.csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Parses and writes quoted CSV text. */
public final class CsvCodec {
    private CsvCodec() {
    }

    public static List<List<String>> parse(String text) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        if (text.isEmpty()) {
            return rows;
        }

        int position = 0;
        while (position < text.length()) {
            if (onlyLineBreaksRemain(text, position)) {
                break;
            }
            List<String> row = new ArrayList<>();
            boolean rowComplete = false;
            while (!rowComplete) {
                StringBuilder field = new StringBuilder();
                if (text.charAt(position) == '"') {
                    position = readQuotedField(text, position + 1, field);
                } else {
                    position = readUnquotedField(text, position, field);
                }
                row.add(field.toString());

                if (position == text.length()) {
                    rowComplete = true;
                } else if (text.charAt(position) == ',') {
                    position++;
                    if (position == text.length()) {
                        row.add("");
                        rowComplete = true;
                    }
                } else {
                    position = skipLineBreak(text, position);
                    rowComplete = true;
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private static int readQuotedField(String text, int start, StringBuilder field)
            throws IOException {
        int position = start;
        boolean closed = false;
        while (position < text.length()) {
            char current = text.charAt(position);
            if (current == '"') {
                if (position + 1 < text.length()
                        && text.charAt(position + 1) == '"') {
                    field.append('"');
                    position += 2;
                } else {
                    position++;
                    closed = true;
                    break;
                }
            } else {
                field.append(current);
                position++;
            }
        }
        if (!closed) {
            throw new IOException("unterminated quoted CSV field");
        }
        if (position < text.length() && text.charAt(position) != ','
                && !isLineBreak(text.charAt(position))) {
            throw new IOException("unexpected character after quoted CSV field");
        }
        return position;
    }

    private static int readUnquotedField(String text, int start, StringBuilder field)
            throws IOException {
        int position = start;
        while (position < text.length() && text.charAt(position) != ','
                && !isLineBreak(text.charAt(position))) {
            char current = text.charAt(position);
            if (current == '"') {
                throw new IOException("quote inside unquoted CSV field");
            }
            field.append(current);
            position++;
        }
        return position;
    }

    public static String write(List<List<String>> rows) {
        StringBuilder text = new StringBuilder();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                if (columnIndex > 0) {
                    text.append(',');
                }
                appendField(text, row.get(columnIndex));
            }
            if (rowIndex + 1 < rows.size()) {
                text.append("\r\n");
            }
        }
        return text.toString();
    }

    private static void appendField(StringBuilder text, String value) {
        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
        if (!quote) {
            text.append(value);
            return;
        }
        text.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '"') {
                text.append("\"\"");
            } else {
                text.append(current);
            }
        }
        text.append('"');
    }

    private static boolean onlyLineBreaksRemain(String text, int position) {
        for (int index = position; index < text.length(); index++) {
            if (!isLineBreak(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLineBreak(char value) {
        return value == '\r' || value == '\n';
    }

    private static int skipLineBreak(String text, int position) {
        if (text.charAt(position) == '\r' && position + 1 < text.length()
                && text.charAt(position + 1) == '\n') {
            return position + 2;
        }
        return position + 1;
    }
}
