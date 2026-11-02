package autograder.analysis.boilerplate;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

final class ComparisonOverview {
    private ComparisonOverview() {
    }

    static void row(StringBuilder rows, String id, String label,
                    List<MethodComparison.Difference> differences, String state) {
        rows.append("<tr><th scope=row>").append(MethodDiffHtml.escape(label))
                .append("</th><td>").append(MethodDiffHtml.escape(state)).append("</td>");
        for (String status : List.of("MODIFIED", "UNCHANGED", "ADDED", "REMOVED")) {
            long count = differences.stream().filter(item -> item.status().equals(status)).count();
            rows.append("<td>").append(count).append("</td>");
        }
        rows.append("<td><a class=why href=\"#").append(id)
                .append("\" aria-label=\"View why: ").append(MethodDiffHtml.escape(label))
                .append("\">View why</a></td></tr>");
    }

    static void write(Writer output, StringBuilder rows) throws IOException {
        output.write("<section id=overview><h2 tabindex=\"-1\">Comparison overview</h2>"
                + "<p>One row per submission and file. Counts describe methods, not similarity percentages. "
                + "View why opens the matching method differences and token edit explanations. "
                + "Overview rows remain visible when detail filters are applied.</p>"
                + "<div class=scroll><table class=overview-table><thead><tr>"
                + "<th scope=col>Submission / file</th><th scope=col>Comparison status</th>"
                + "<th scope=col>Modified</th><th scope=col>Unchanged</th>"
                + "<th scope=col>Added</th><th scope=col>Removed</th><th scope=col>Evidence</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table></div></section>");
    }
}
