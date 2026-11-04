package autograder.analysis.boilerplate;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

final class MethodDiffHtml {
    private static final long MAX_LINE_CELLS = 250_000;

    private MethodDiffHtml() {
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static void write(Writer output, MethodComparison.Difference difference)
            throws IOException {
        output.write("<details class=method data-status=\""
                + difference.status()
                + "\"><summary><strong>" + difference.status() + "</strong> "
                + escape(difference.signature()) + "</summary><p>"
                + escape(difference.explanation()) + "</p>");
        JavaMethods.Method first = difference.starter();
        JavaMethods.Method second = difference.answer();
        List<String> before = first == null ? List.of()
                : first.source().lines().toList();
        List<String> after = second == null ? List.of()
                : second.source().lines().toList();
        output.write("<div class=scroll><table><caption>Original source lines; "
                + "red − removed, green + added. Line highlighting includes "
                + "formatting changes.</caption>"
                + "<colgroup><col style=\"width:52px\"><col>"
                + "<col style=\"width:52px\"><col></colgroup>"
                + "<thead><tr><th colspan=2>Starter</th>"
                + "<th colspan=2>Student</th>"
                + "</tr></thead><tbody>");
        if ((long) (before.size() + 1) * (after.size() + 1) > MAX_LINE_CELLS) {
            output.write("<tr><td colspan=4>Line alignment budget exceeded; "
                    + "showing complete methods without change highlighting."
                    + "</td></tr>");
            for (int i = 0; i < Math.max(before.size(), after.size()); i++) {
                row(output, first, second, before, after,
                        i < before.size() ? i : -1,
                        i < after.size() ? i : -1, false);
            }
        } else {
            aligned(output, first, second, before, after);
        }
        output.write("</tbody></table></div></details>");
    }

    private static void aligned(Writer output, JavaMethods.Method first,
            JavaMethods.Method second, List<String> before, List<String> after)
            throws IOException {
        int[][] common = new int[before.size() + 1][after.size() + 1];
        for (int i = before.size() - 1; i >= 0; i--) {
            for (int j = after.size() - 1; j >= 0; j--) {
                common[i][j] = before.get(i).equals(after.get(j))
                        ? common[i + 1][j + 1] + 1
                        : Math.max(common[i + 1][j], common[i][j + 1]);
            }
        }
        int i = 0;
        int j = 0;
        while (i < before.size() || j < after.size()) {
            if (i < before.size() && j < after.size()
                    && before.get(i).equals(after.get(j))) {
                row(output, first, second, before, after, i++, j++, false);
            } else if (i < before.size() && (j == after.size()
                    || common[i + 1][j] >= common[i][j + 1])) {
                row(output, first, second, before, after, i++, -1, true);
            } else {
                row(output, first, second, before, after, -1, j++, true);
            }
        }
    }

    private static void row(Writer output, JavaMethods.Method first,
            JavaMethods.Method second, List<String> before, List<String> after,
            int i, int j, boolean changed) throws IOException {
        output.write("<tr>");
        cell(output, first, before, i, changed ? "removed" : "", "−");
        cell(output, second, after, j, changed ? "added" : "", "+");
        output.write("</tr>");
    }

    private static void cell(Writer output, JavaMethods.Method method,
            List<String> lines, int index, String style, String marker)
            throws IOException {
        if (index < 0) {
            output.write("<td></td><td></td>");
            return;
        }
        output.write("<td class=number>" + (method.line() + index)
                + "</td><td class=\"" + style + "\"><pre>"
                + (style.isEmpty() ? "  " : marker + " ")
                + escape(lines.get(index)) + "</pre></td>");
    }
}
