package autograder.report.html;

import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Renders the run summary cards: counts plus the average and median exported score. */
final class HtmlSummaryRenderer {
    private HtmlSummaryRenderer() { }

    static void appendSummary(StringBuilder html, List<StudentGrade> grades,
                              Map<SubmissionResult, String> withheld, int received,
                              int exported, int withheldCount, int warningSubmissions,
                              BigDecimal maximumScore) {
        List<BigDecimal> exportedScores = new ArrayList<>();
        BigDecimal exportedTotal = BigDecimal.ZERO;
        for (StudentGrade grade : grades) {
            if (!withheld.containsKey(grade.getSubmission())) {
                BigDecimal score = HtmlScoreFormatter.totalScore(grade);
                exportedScores.add(score);
                exportedTotal = exportedTotal.add(score);
            }
        }

        html.append("<section class=\"summary-section\" aria-labelledby=\"summary-title\">\n")
                .append("<h2 id=\"summary-title\">Run summary</h2>\n")
                .append("<div class=\"summary-grid\">\n");
        metric(html, "Received submissions", Integer.toString(received),
                "Submissions present in this grading run");
        metric(html, "Exported", Integer.toString(exported), "Included in the score CSV");
        metric(html, "Withheld", Integer.toString(withheldCount), "Excluded from the score CSV");
        metric(html, "Submissions with warnings", Integer.toString(warningSubmissions),
                "At least one preparation warning was recorded");
        if (exported == 0) {
            metric(html, "Average exported score", "N/A", "No exported submissions");
            metric(html, "Median exported score", "N/A", "No exported submissions");
        } else {
            BigDecimal average = exportedTotal.divide(BigDecimal.valueOf(exported),
                    HtmlScoreFormatter.AVERAGE_DECIMAL_PLACES, RoundingMode.HALF_UP);
            BigDecimal median = median(exportedScores);
            metric(html, "Average exported score", HtmlScoreFormatter.format(average) + " points",
                    "Exported-only mean rounded to " + HtmlScoreFormatter.AVERAGE_DECIMAL_PLACES
                            + " decimal places; maximum "
                            + HtmlScoreFormatter.formatOrUnavailable(maximumScore) + " points");
            metric(html, "Median exported score", HtmlScoreFormatter.format(median) + " points",
                    "Exported submissions only; maximum "
                            + HtmlScoreFormatter.formatOrUnavailable(maximumScore) + " points");
        }
        html.append("</div>\n<p class=\"scope-note\">These results cover received submissions only. ")
                .append("Students without a received submission are not listed here; ")
                .append("their template values are retained in the CSV.</p>\n")
                .append("</section>\n");
    }

    private static void metric(StringBuilder html, String label, String value, String description) {
        html.append("<article class=\"metric-card\"><h3>").append(HtmlEscaper.escape(label))
                .append("</h3><p class=\"metric-value\">").append(HtmlEscaper.escape(value))
                .append("</p><p class=\"metric-description\">").append(HtmlEscaper.escape(description))
                .append("</p></article>\n");
    }

    private static BigDecimal median(List<BigDecimal> scores) {
        List<BigDecimal> ordered = new ArrayList<>(scores);
        ordered.sort(Comparator.naturalOrder());
        int middle = ordered.size() / 2;
        if (ordered.size() % 2 == 1) {
            return ordered.get(middle);
        }
        return ordered.get(middle - 1).add(ordered.get(middle)).divide(BigDecimal.valueOf(2));
    }
}
