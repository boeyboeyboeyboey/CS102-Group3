package autograder.report.html;

import autograder.model.StudentIdentity;
import autograder.model.SubmissionCorrection;
import autograder.model.SubmissionResult;
import autograder.model.StudentGrade;
import autograder.model.TestCaseResult;

import java.util.List;

/** Renders the expandable details row for one submission: record, identity, warnings and corrections. */
final class HtmlSubmissionDetailsRenderer {
    private HtmlSubmissionDetailsRenderer() { }

    static void appendDetailRow(StringBuilder html, StudentGrade grade, int ordinal,
                                String displayName, String withheldReason, int columnCount,
                                List<QuestionSection> questionSections,
                                List<TestCaseResult> unmatchedCases) {
        SubmissionResult submission = grade.getSubmission();
        html.append("<tr class=\"detail-row\" id=\"submission-").append(ordinal)
                .append("-details\"><td colspan=\"").append(columnCount)
                .append("\"><details><summary>Details for ").append(HtmlEscaper.escape(displayName))
                .append(" (submission #").append(ordinal).append(")</summary><div class=\"detail-content\">\n");
        appendSubmissionRecord(html, submission, withheldReason);
        appendWarnings(html, submission);
        appendCorrections(html, submission.getCorrections());
        HtmlCaseRenderer.appendCases(html, questionSections, unmatchedCases);
        html.append("</div></details></td></tr>\n");
    }

    private static void appendSubmissionRecord(StringBuilder html, SubmissionResult submission,
                                               String withheldReason) {
        StudentIdentity identity = submission.getIdentity();
        html.append("<section class=\"detail-block\"><h3>Submission record</h3><dl class=\"path-list\">");
        detailItem(html, "Archive", HtmlSubmissionPresentation.pathText(submission.getArchivePath()));
        detailItem(html, "Extracted", submission.isExtracted() ? "Yes" : "No");
        detailItem(html, "Extraction path", HtmlSubmissionPresentation.pathText(submission.getExtractionPath()));
        detailItem(html, "Student path", HtmlSubmissionPresentation.pathText(submission.getStudentRoot()));
        detailItem(html, "Working path", HtmlSubmissionPresentation.pathText(submission.getWorkingRoot()));
        appendIdentity(html, identity);
        appendExportDecision(html, withheldReason);
        html.append("</dl></section>\n");
    }

    private static void appendIdentity(StringBuilder html, StudentIdentity identity) {
        if (identity == null) {
            detailItem(html, "Identity", "Not available");
            return;
        }
        detailItem(html, "Raw header name", identity.getName());
        detailItem(html, "Raw header username", identity.getUsername());
        detailItem(html, "Raw header email", identity.getEmailId());
        detailItem(html, "Identity source", identity.getSource());
        if (identity.getStudentRecord() == null) {
            detailItem(html, "Roster identity", "Not matched");
        } else {
            detailItem(html, "Roster name", HtmlSubmissionPresentation.rosterName(identity));
            detailItem(html, "Roster username", identity.getStudentRecord().getUsername());
            detailItem(html, "Roster email", identity.getStudentRecord().getEmail());
            detailItem(html, "Roster ID", identity.getStudentRecord().getId());
        }
    }

    private static void appendExportDecision(StringBuilder html, String withheldReason) {
        if (withheldReason != null) {
            detailItem(html, "Export decision", HtmlSubmissionTableRenderer.WITHHELD + " — " + withheldReason);
        } else {
            detailItem(html, "Export decision", HtmlSubmissionTableRenderer.EXPORTED);
        }
    }

    private static void appendWarnings(StringBuilder html, SubmissionResult submission) {
        html.append("<section class=\"detail-block\"><h3>Preparation warnings</h3>");
        if (submission.getAnomalies().isEmpty()) {
            html.append("<p>No preparation warnings recorded.</p>");
        } else {
            html.append("<ul class=\"text-list\">");
            for (String warning : submission.getAnomalies()) {
                html.append("<li>").append(HtmlEscaper.escape(warning)).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>\n");
    }

    private static void appendCorrections(StringBuilder html, List<SubmissionCorrection> corrections) {
        html.append("<section class=\"detail-block\"><h3>Recorded corrections</h3>");
        if (corrections.isEmpty()) {
            html.append("<p>No corrections recorded.</p>");
        } else {
            html.append("<div class=\"table-scroll compact-table\"><table><thead><tr><th scope=\"col\">Type</th>")
                    .append("<th scope=\"col\">Path</th><th scope=\"col\">Field</th>")
                    .append("<th scope=\"col\">Original value</th>")
                    .append("<th scope=\"col\">Corrected value</th><th scope=\"col\">Header added</th>")
                    .append("</tr></thead><tbody>");
            for (SubmissionCorrection correction : corrections) {
                html.append("<tr><td>").append(HtmlEscaper.escape(correction.getType())).append("</td><td>")
                        .append(HtmlEscaper.escape(HtmlSubmissionPresentation.pathText(correction.getRelativePath())))
                        .append("</td><td>").append(HtmlEscaper.escape(correction.getField()))
                        .append("</td><td><pre class=\"value-block\">")
                        .append(HtmlEscaper.escape(correction.getOriginalValue()))
                        .append("</pre></td><td><pre class=\"value-block\">")
                        .append(HtmlEscaper.escape(correction.getCorrectedValue())).append("</pre></td><td>")
                        .append(correction.isHeaderAdded() ? "Yes" : "No").append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        html.append("</section>\n");
    }

    private static void detailItem(StringBuilder html, String label, String value) {
        html.append("<dt>").append(HtmlEscaper.escape(label)).append("</dt><dd>")
                .append(HtmlEscaper.escape(value)).append("</dd>");
    }
}
