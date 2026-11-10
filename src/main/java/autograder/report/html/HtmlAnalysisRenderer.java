package autograder.report.html;

import autograder.model.AnalysisFinding;
import autograder.model.SimilarityFinding;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

/** Renders the code-analysis and similarity findings section of the dashboard. */
final class HtmlAnalysisRenderer {
    private HtmlAnalysisRenderer() { }

    static void appendAnalysisSection(StringBuilder html, List<AnalysisFinding> findings,
                                     List<SimilarityFinding> similarities) {
        html.append("<section class=\"analysis-section\" aria-labelledby=\"analysis-title\">")
                .append("<h2 id=\"analysis-title\">Analysis findings</h2>\n");
        if (findings.isEmpty() && similarities.isEmpty()) {
            html.append("<p>No analysis data supplied for this run.</p>\n</section>\n");
            return;
        }
        appendCodeFindings(html, findings);
        appendSimilarityFindings(html, similarities);
        html.append("</section>\n");
    }

    private static void appendCodeFindings(StringBuilder html, List<AnalysisFinding> findings) {
        if (findings.isEmpty()) {
            html.append("<h3>Code analysis findings</h3><p>No code analysis findings supplied.</p>\n");
        } else {
            html.append("<h3>Code analysis findings</h3><ul class=\"analysis-list\">");
            for (AnalysisFinding finding : findings) {
                html.append("<li><strong>").append(HtmlEscaper.escape(finding.getCode())).append("</strong><span>")
                        .append(HtmlEscaper.escape(pathText(finding.getSourceFile()))).append("</span><p>")
                        .append(HtmlEscaper.escape(finding.getMessage())).append("</p></li>");
            }
            html.append("</ul>\n");
        }
    }

    private static void appendSimilarityFindings(StringBuilder html,
                                                 List<SimilarityFinding> similarities) {
        if (similarities.isEmpty()) {
            html.append("<h3>Similarity findings</h3><p>No similarity findings supplied.</p>\n");
        } else {
            html.append("<h3>Similarity findings</h3><ul class=\"analysis-list\">");
            for (SimilarityFinding finding : similarities) {
                BigDecimal percent = BigDecimal.valueOf(finding.getSimilarity())
                        .multiply(BigDecimal.valueOf(100));
                html.append("<li><strong>Similarity: ").append(HtmlScoreFormatter.format(percent))
                        .append("%</strong><span>")
                        .append(HtmlEscaper.escape(pathText(finding.getFirstArchive()))).append(" ↔ ")
                        .append(HtmlEscaper.escape(pathText(finding.getSecondArchive()))).append("</span>");
                if (finding.getEvidence().isEmpty()) {
                    html.append("<p>No evidence details recorded.</p>");
                } else {
                    html.append("<ul class=\"text-list\">");
                    for (String evidence : finding.getEvidence()) {
                        html.append("<li>").append(HtmlEscaper.escape(evidence)).append("</li>");
                    }
                    html.append("</ul>");
                }
                html.append("</li>");
            }
            html.append("</ul>\n");
        }
    }

    private static String pathText(Path path) {
        return path == null ? "Not recorded" : path.toString();
    }
}
