package autograder.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes one .xlsx workbook with one sheet per table, using only the JDK. */
public final class XlsxWorkbookWriter {
    /** One worksheet; the first row is shown as a bold, frozen header. */
    public record Sheet(String name, List<List<String>> rows) {
    }

    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String STYLES = "<styleSheet xmlns=\"" + MAIN_NS + "\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>"
            + "</styleSheet>";
    private static final int MAX_CELL_CHARS = 32_767;
    private static final int MAX_NAME_CHARS = 31;
    private static final String TRUNCATED = "... [cut to fit an Excel cell; see the CSV report]";
    // leading zeros and very long digit runs stay text so their exact digits are kept
    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9]\\d{0,13})(\\.\\d{1,12})?");
    private static final Pattern BAD_NAME = Pattern.compile(".*[\\[\\]:*?/\\\\].*");
    private static final int MIN_COLUMN_WIDTH = 8;
    private static final int MAX_COLUMN_WIDTH = 60;
    private static final int SAMPLED_ROWS = 200;

    private XlsxWorkbookWriter() {
    }

    /** Replaces target only once the whole workbook has been written. */
    public static void write(Path target, List<Sheet> sheets) throws IOException {
        validate(sheets);
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(partial))) {
                entry(zip, "[Content_Types].xml", contentTypes(sheets.size()));
                entry(zip, "_rels/.rels", rootRelationships());
                entry(zip, "xl/workbook.xml", workbook(sheets));
                entry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size()));
                entry(zip, "xl/styles.xml", XML_HEADER + STYLES);
                for (int index = 0; index < sheets.size(); index++) {
                    entry(zip, "xl/worksheets/sheet" + (index + 1) + ".xml", worksheet(sheets.get(index).rows()));
                }
            }
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    private static void validate(List<Sheet> sheets) {
        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("a workbook needs at least one sheet");
        }
        Set<String> names = new HashSet<>();
        for (Sheet sheet : sheets) {
            String name = sheet.name();
            if (name.isBlank() || name.length() > MAX_NAME_CHARS || BAD_NAME.matcher(name).matches()
                    || !names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("invalid or duplicate sheet name: " + name);
            }
        }
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes(int sheetCount) {
        StringBuilder xml = new StringBuilder(XML_HEADER)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"")
                .append("application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"")
                .append("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"")
                .append("application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int number = 1; number <= sheetCount; number++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(number)
                    .append(".xml\" ContentType=\"")
                    .append("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private static String rootRelationships() {
        return XML_HEADER + "<Relationships xmlns=\"" + PACKAGE_REL_NS + "\"><Relationship Id=\"rId1\" Type=\""
                + REL_NS + "/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";
    }

    private static String workbook(List<Sheet> sheets) {
        StringBuilder xml = new StringBuilder(XML_HEADER).append("<workbook xmlns=\"").append(MAIN_NS)
                .append("\" xmlns:r=\"").append(REL_NS).append("\"><bookViews><workbookView/></bookViews><sheets>");
        for (int index = 0; index < sheets.size(); index++) {
            xml.append("<sheet name=\"").append(escape(sheets.get(index).name()).replace("\"", "&quot;"))
                    .append("\" sheetId=\"").append(index + 1).append("\" r:id=\"rId").append(index + 1).append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private static String workbookRelationships(int sheetCount) {
        StringBuilder xml = new StringBuilder(XML_HEADER).append("<Relationships xmlns=\"")
                .append(PACKAGE_REL_NS).append("\">");
        for (int number = 1; number <= sheetCount; number++) {
            xml.append("<Relationship Id=\"rId").append(number).append("\" Type=\"").append(REL_NS)
                    .append("/worksheet\" Target=\"worksheets/sheet").append(number).append(".xml\"/>");
        }
        return xml.append("<Relationship Id=\"rId").append(sheetCount + 1).append("\" Type=\"").append(REL_NS)
                .append("/styles\" Target=\"styles.xml\"/></Relationships>").toString();
    }

    private static String worksheet(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder(XML_HEADER).append("<worksheet xmlns=\"").append(MAIN_NS)
                .append("\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" ")
                .append("activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        appendColumns(xml, rows);
        xml.append("<sheetData>");
        for (int row = 0; row < rows.size(); row++) {
            xml.append("<row r=\"").append(row + 1).append("\">");
            List<String> values = rows.get(row);
            for (int column = 0; column < values.size(); column++) {
                appendCell(xml, columnName(column) + (row + 1), values.get(column), row == 0);
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static void appendColumns(StringBuilder xml, List<List<String>> rows) {
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns == 0) {
            return;
        }
        xml.append("<cols>");
        for (int column = 0; column < columns; column++) {
            int widest = MIN_COLUMN_WIDTH;
            for (List<String> row : rows.subList(0, Math.min(SAMPLED_ROWS, rows.size()))) {
                if (column < row.size()) {
                    widest = Math.max(widest, row.get(column).length() + 2);
                }
            }
            xml.append("<col min=\"").append(column + 1).append("\" max=\"").append(column + 1)
                    .append("\" width=\"").append(Math.min(MAX_COLUMN_WIDTH, widest)).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols>");
    }

    // text is written as an inline string, so a student's "=..." can never run as a formula
    private static void appendCell(StringBuilder xml, String reference, String value, boolean header) {
        xml.append("<c r=\"").append(reference).append('"');
        if (header) {
            xml.append(" s=\"1\"");
        } else if (NUMBER.matcher(value).matches()) {
            xml.append("><v>").append(value).append("</v></c>");
            return;
        }
        xml.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(escape(fit(legalCharacters(value)))).append("</t></is></c>");
    }

    private static String columnName(int column) {
        StringBuilder name = new StringBuilder();
        for (int remaining = column + 1; remaining > 0; remaining = (remaining - 1) / 26) {
            name.insert(0, (char) ('A' + (remaining - 1) % 26));
        }
        return name.toString();
    }

    // student output can hold control characters that make an xml file unreadable
    private static String legalCharacters(String value) {
        StringBuilder kept = new StringBuilder(value.length());
        value.replace("\r\n", "\n").replace('\r', '\n').codePoints().filter(XlsxWorkbookWriter::legalInXml)
                .forEach(kept::appendCodePoint);
        return kept.toString();
    }

    private static boolean legalInXml(int point) {
        return point == '\t' || point == '\n' || (point >= 0x20 && point <= 0xD7FF)
                || (point >= 0xE000 && point <= 0xFFFD) || point >= 0x10000;
    }

    private static String fit(String value) {
        if (value.length() <= MAX_CELL_CHARS) {
            return value;
        }
        int end = MAX_CELL_CHARS - TRUNCATED.length();
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end) + TRUNCATED;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
