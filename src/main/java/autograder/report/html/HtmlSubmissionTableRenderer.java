package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders the submissions table: controls, legend, header and one row per received archive. */
final class HtmlSubmissionTableRenderer {
    /** Table columns besides the per-question ones: submission, preparation, decision, total, details. */
    static final int FIXED_COLUMNS = 5;
    static final String WITHHELD = "WITHHELD";
    static final String EXPORTED = "EXPORTED";

    private HtmlSubmissionTableRenderer() { }

    static void appendSection(StringBuilder html, List<StudentGrade> grades,
                              Map<SubmissionResult, String> withheld,
                              List<QuestionDefinition> questions,
                              BigDecimal assessmentMaximum, int received) {
        int columnCount = questions.size() + FIXED_COLUMNS;
        appendSectionHeading(html);
        if (!withheld.isEmpty()) {
            html.append("<p class=\"scope-note\">Scores for WITHHELD submissions are diagnostic only. ")
                    .append("They were excluded from the CSV and exported-only summary statistics.</p>\n");
        }
        appendControls(html);
        html.append("<p class=\"result-count\" id=\"result-count\" role=\"status\" aria-live=\"polite\">Showing ")
                .append(received).append(" of ").append(received).append(" submissions.</p>\n");
        appendLegend(html);
        html.append("<div class=\"table-scroll\" role=\"region\" aria-label=\"Submission results table\" ")
                .append("tabindex=\"0\">\n")
                .append("<table><thead><tr><th scope=\"col\">Submission</th><th scope=\"col\">Preparation</th>")
                .append("<th scope=\"col\">Export decision</th>");
        appendQuestionHeaders(html, questions, assessmentMaximum);
        appendRows(html, grades, withheld, questions, assessmentMaximum, columnCount);
        html.append("<tr id=\"no-matches\" hidden><td colspan=\"").append(columnCount)
                .append("\">No submissions match these filters.</td></tr>\n")
                .append("</tbody></table></div>\n</section>\n");
    }

    private static void appendSectionHeading(StringBuilder html) {
        html.append("<section class=\"submissions-section\" aria-labelledby=\"submissions-title\">\n")
                .append("<div class=\"section-heading\"><div><h2 id=\"submissions-title\">Submissions</h2>")
                .append("<p>Each row represents one received archive, including duplicate identities.</p>")
                .append("</div></div>\n");
    }

    private static void appendControls(StringBuilder html) {
        html.append("<div class=\"controls\">\n")
                .append("<div class=\"control\"><label for=\"submission-search\">")
                .append("Search name, username, or archive</label>")
                .append("<input id=\"submission-search\" type=\"search\" autocomplete=\"off\" ")
                .append("placeholder=\"Search submissions\"></div>\n")
                .append("<div class=\"control\"><label for=\"export-filter\">Export decision</label>")
                .append("<select id=\"export-filter\">")
                .append("<option value=\"all\">All submissions</option><option value=\"exported\">Exported</option>")
                .append("<option value=\"withheld\">Withheld</option></select></div>\n")
                .append("<div class=\"control\"><label for=\"warning-filter\">Preparation warnings</label>")
                .append("<select id=\"warning-filter\">")
                .append("<option value=\"all\">All submissions</option>")
                .append("<option value=\"warning\">With warnings</option>")
                .append("<option value=\"no-warning\">No warnings</option></select></div>\n")
                .append("<div class=\"control\"><label for=\"sort-order\">Sort submissions</label>")
                .append("<select id=\"sort-order\">")
                .append("<option value=\"name-asc\">Name, A to Z</option>")
                .append("<option value=\"name-desc\">Name, Z to A</option>")
                .append("<option value=\"score-asc\">Diagnostic total, low to high</option>")
                .append("<option value=\"score-desc\">Diagnostic total, high to low</option></select></div>\n</div>\n");
    }

    private static void appendLegend(StringBuilder html) {
        html.append("<div class=\"credit-legend\" role=\"group\" aria-label=\"Question score shading legend\">")
                .append("<strong>Question score shading, based on earned points / configured maximum:</strong>")
                .append("<ul><li><span class=\"credit-swatch credit-band-zero\"></span>0%</li>")
                .append("<li><span class=\"credit-swatch credit-band-low\"></span>&gt;0–&lt;25%</li>")
                .append("<li><span class=\"credit-swatch credit-band-quarter\"></span>25–&lt;50%</li>")
                .append("<li><span class=\"credit-swatch credit-band-half\"></span>50–&lt;75%</li>")
                .append("<li><span class=\"credit-swatch credit-band-three-quarter\"></span>75–&lt;100%</li>")
                .append("<li><span class=\"credit-swatch credit-band-full\"></span>100%</li></ul>")
                .append("<p>Neutral cells have incomplete or invalid evidence. ")
                .append("Scores remain visible, and withheld scores are diagnostic.</p></div>\n");
    }

    private static void appendQuestionHeaders(StringBuilder html, List<QuestionDefinition> questions,
                                              BigDecimal assessmentMaximum) {
        for (QuestionDefinition question : questions) {
            html.append("<th scope=\"col\">Question ").append(HtmlEscaper.escape(question.getId()))
                    .append("<span class=\"th-sub\"> / ")
                    .append(HtmlScoreFormatter.formatOrUnavailable(QuestionOutcome.configuredMaximum(question)))
                    .append(" max</span></th>");
        }
        html.append("<th scope=\"col\">Total points<span class=\"th-sub\"> / ")
                .append(HtmlScoreFormatter.formatOrUnavailable(assessmentMaximum)).append(" max</span></th>")
                .append("<th scope=\"col\">Details</th></tr></thead><tbody id=\"submission-rows\">\n");
    }

    private static void appendRows(StringBuilder html, List<StudentGrade> grades,
                                   Map<SubmissionResult, String> withheld,
                                   List<QuestionDefinition> questions,
                                   BigDecimal assessmentMaximum, int columnCount) {
        if (grades.isEmpty()) {
            html.append("<tr><td colspan=\"").append(columnCount)
                    .append("\">No submissions were received in this run.</td></tr>\n");
        }
        int ordinal = 0;
        for (StudentGrade grade : grades) {
            ordinal++;
            appendStudentRow(html, grade, ordinal, withheld.get(grade.getSubmission()),
                    questions, assessmentMaximum);
        }
    }

    private static void appendStudentRow(StringBuilder html, StudentGrade grade, int ordinal,
                                         String withheldReason, List<QuestionDefinition> questions,
                                         BigDecimal assessmentMaximum) {
        SubmissionResult submission = grade.getSubmission();
        StudentIdentity identity = submission.getIdentity();
        String displayName = HtmlSubmissionPresentation.displayName(identity, ordinal);
        String username = HtmlSubmissionPresentation.preferredUsername(identity);
        String archive = HtmlSubmissionPresentation.pathText(submission.getArchivePath());
        String archiveName = HtmlSubmissionPresentation.archiveName(submission.getArchivePath());
        boolean isWithheld = withheldReason != null;
        boolean hasWarnings = !submission.getAnomalies().isEmpty();
        BigDecimal score = HtmlScoreFormatter.totalScore(grade);
        appendRowStart(html, grade, ordinal, displayName, username, archive, isWithheld, hasWarnings, score);
        appendSubmissionCell(html, displayName, username, archiveName, ordinal);
        appendDecisionCell(html, submission, isWithheld, withheldReason);
        List<QuestionSection> questionSections = HtmlCaseRenderer.questionSections(grade, questions, ordinal);
        for (QuestionSection section : questionSections) {
            HtmlCaseRenderer.appendQuestionScoreCell(html, section, isWithheld);
        }
        appendTotalAndDetailLink(html, score, assessmentMaximum, isWithheld, ordinal);
        HtmlSubmissionDetailsRenderer.appendDetailRow(html, grade, ordinal, displayName, withheldReason,
                questions.size() + FIXED_COLUMNS, questionSections,
                HtmlCaseRenderer.unmatchedCases(grade, questionSections));
    }

    private static void appendRowStart(StringBuilder html, StudentGrade grade, int ordinal,
                                       String displayName, String username, String archive,
                                       boolean isWithheld, boolean hasWarnings, BigDecimal score) {
        StudentIdentity identity = grade.getSubmission().getIdentity();
        String rawName = identity == null ? "" : HtmlSubmissionPresentation.nullToEmpty(identity.getName());
        String rawUsername = identity == null ? "" : HtmlSubmissionPresentation.nullToEmpty(identity.getUsername());
        String searchText = (displayName + " " + username + " " + rawName + " " + rawUsername
                + " " + archive).toLowerCase(Locale.ROOT);
        html.append("<tr class=\"submission-row\" id=\"submission-").append(ordinal)
                .append("\" data-search=\"").append(HtmlEscaper.escape(searchText))
                .append("\" data-sort-name=\"").append(HtmlEscaper.escape(displayName.toLowerCase(Locale.ROOT)))
                .append("\" data-sort-score=\"").append(HtmlScoreFormatter.format(score))
                .append("\" data-export=\"").append(isWithheld ? "withheld" : "exported")
                .append("\" data-warning=\"").append(hasWarnings).append("\">\n");
    }

    private static void appendSubmissionCell(StringBuilder html, String displayName, String username,
                                             String archiveName, int ordinal) {
        html.append("<td><strong>").append(HtmlEscaper.escape(displayName))
                .append("</strong><span class=\"row-ordinal\">Submission #")
                .append(ordinal).append("</span>");
        if (!username.isBlank()) {
            html.append("<span class=\"row-secondary\">Username: ").append(HtmlEscaper.escape(username))
                    .append("</span>");
        }
        html.append("<span class=\"row-secondary\">Archive: ").append(HtmlEscaper.escape(archiveName))
                .append("</span></td>");
    }

    private static void appendDecisionCell(StringBuilder html, SubmissionResult submission,
                                           boolean isWithheld, String withheldReason) {
        html.append("<td>").append(statusLabel(submission.getStatus())).append("</td><td>")
                .append(isWithheld ? "<span class=\"decision withheld\">" + WITHHELD + "</span>"
                        : "<span class=\"decision exported\">" + EXPORTED + "</span>");
        if (isWithheld) {
            html.append("<span class=\"row-secondary\">").append(HtmlEscaper.escape(withheldReason)).append("</span>");
        }
        html.append("</td>");
    }

    private static void appendTotalAndDetailLink(StringBuilder html, BigDecimal score,
                                                 BigDecimal assessmentMaximum,
                                                 boolean isWithheld, int ordinal) {
        html.append("<td class=\"numeric total-score");
        if (isWithheld) {
            html.append(" diagnostic-score\">");
            html.append("<span class=\"diagnostic-label\">Diagnostic: </span>");
        } else {
            html.append("\">");
        }
        html.append(HtmlScoreFormatter.format(score)).append(" / ")
                .append(HtmlScoreFormatter.formatOrUnavailable(assessmentMaximum))
                .append("</td><td><a class=\"details-link\" href=\"#submission-")
                .append(ordinal).append("-details\">Open details</a></td></tr>\n");
    }

    private static String statusLabel(SubmissionStatus status) {
        return "<span class=\"prep-status\">" + HtmlEscaper.escape(status == null ? "UNKNOWN" : status.name())
                + "</span>";
    }
}
