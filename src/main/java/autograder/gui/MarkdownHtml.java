package autograder.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns the Markdown our feedback files use into HTML that a Swing text pane can show. */
final class MarkdownHtml {
    private static final Pattern FENCE = Pattern.compile("`{3,}");
    private static final Pattern HEADING = Pattern.compile("(#{1,6}) (.*)");
    private static final Pattern TABLE_RULE = Pattern.compile("\\|( *:?-+:? *\\|)+");
    private static final String ESCAPABLE = "\\`*_[]<>#|!~";

    private MarkdownHtml() {
    }

    static String render(String markdown) {
        String[] lines = markdown.split("\\R", -1);
        StringBuilder html = new StringBuilder("<html><body>");
        int index = 0;
        while (index < lines.length) {
            index = appendBlock(html, lines, index);
        }
        return html.append("</body></html>").toString();
    }

    // appends the block starting at index and returns the index of the line after it
    private static int appendBlock(StringBuilder html, String[] lines, int index) {
        String line = lines[index];
        Matcher fence = FENCE.matcher(line);
        Matcher heading = HEADING.matcher(line);
        if (fence.lookingAt()) {
            return appendCode(html, lines, index, fence.group());
        }
        if (heading.matches()) {
            int level = heading.group(1).length();
            html.append("<h").append(level).append('>').append(inline(heading.group(2)))
                    .append("</h").append(level).append('>');
            return index + 1;
        }
        if (index + 1 < lines.length && line.startsWith("|") && TABLE_RULE.matcher(lines[index + 1]).matches()) {
            return appendTable(html, lines, index);
        }
        if (line.startsWith("- ")) {
            return appendPrefixed(html, lines, index, "- ", "<ul>", "</ul>", "<li>", "</li>");
        }
        if (line.startsWith("> ")) {
            return appendPrefixed(html, lines, index, "> ", "<blockquote>", "</blockquote>", "", "<br>");
        }
        if (!line.isBlank()) {
            html.append("<p>").append(inline(line)).append("</p>");
        }
        return index + 1;
    }

    private static int appendCode(StringBuilder html, String[] lines, int start, String fence) {
        html.append("<pre>");
        int index = start + 1;
        while (index < lines.length && !lines[index].equals(fence)) {
            html.append(escape(lines[index])).append('\n');
            index++;
        }
        html.append("</pre>");
        return index + 1;
    }

    private static int appendTable(StringBuilder html, String[] lines, int start) {
        List<String> rule = cells(lines[start + 1]);
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"4\">");
        appendRow(html, cells(lines[start]), rule, "th");
        int index = start + 2;
        while (index < lines.length && lines[index].startsWith("|")) {
            appendRow(html, cells(lines[index]), rule, "td");
            index++;
        }
        html.append("</table>");
        return index;
    }

    private static void appendRow(StringBuilder html, List<String> cells, List<String> rule, String tag) {
        html.append("<tr>");
        for (int column = 0; column < cells.size(); column++) {
            boolean right = column < rule.size() && rule.get(column).endsWith(":");
            html.append('<').append(tag).append(right ? " align=\"right\">" : ">")
                    .append(inline(cells.get(column))).append("</").append(tag).append('>');
        }
        html.append("</tr>");
    }

    // splits on the pipes between cells, leaving escaped pipes for inline() to unescape
    private static List<String> cells(String row) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        String body = row.strip();
        for (int index = 1; index < body.length(); index++) {
            char current = body.charAt(index);
            if (current == '\\' && index + 1 < body.length()) {
                cell.append(current).append(body.charAt(++index));
            } else if (current == '|') {
                cells.add(cell.toString().strip());
                cell.setLength(0);
            } else {
                cell.append(current);
            }
        }
        return cells;
    }

    private static int appendPrefixed(StringBuilder html, String[] lines, int start, String prefix,
                                      String open, String close, String itemOpen, String itemClose) {
        html.append(open);
        int index = start;
        while (index < lines.length && lines[index].startsWith(prefix)) {
            html.append(itemOpen).append(inline(lines[index].substring(prefix.length()))).append(itemClose);
            index++;
        }
        html.append(close);
        return index;
    }

    private static String inline(String text) {
        StringBuilder html = new StringBuilder();
        boolean bold = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '\\' && index + 1 < text.length() && ESCAPABLE.indexOf(text.charAt(index + 1)) >= 0) {
                html.append(escape(String.valueOf(text.charAt(++index))));
            } else if (text.startsWith("**", index)) {
                html.append(bold ? "</b>" : "<b>");
                bold = !bold;
                index++;
            } else {
                html.append(escape(String.valueOf(current)));
            }
        }
        return bold ? html.append("</b>").toString() : html.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
