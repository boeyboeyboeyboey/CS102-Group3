package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.TestCaseResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Groups case results by question and renders the per-question score cells and case tables. */
final class HtmlCaseRenderer {
    private HtmlCaseRenderer() { }

    static List<QuestionSection> questionSections(StudentGrade grade,
                                                  List<QuestionDefinition> questions,
                                                  int submissionOrdinal) {
        List<TestCaseResult> remaining = new ArrayList<>(grade.getTestResults());
        List<QuestionSection> sections = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            QuestionDefinition question = questions.get(index);
            List<TestCaseResult> matching = new ArrayList<>();
            for (var iterator = remaining.iterator(); iterator.hasNext();) {
                TestCaseResult result = iterator.next();
                if (Objects.equals(question.getId(), result.getTestCase().getQuestionId())) {
                    matching.add(result);
                    iterator.remove();
                }
            }
            sections.add(new QuestionSection(question, index + 1, matching, submissionOrdinal,
                    QuestionOutcome.from(question,
                            HtmlScoreFormatter.questionScore(grade, question.getId()), matching)));
        }
        return List.copyOf(sections);
    }

    static List<TestCaseResult> unmatchedCases(StudentGrade grade,
                                               List<QuestionSection> questionSections) {
        List<TestCaseResult> remaining = new ArrayList<>(grade.getTestResults());
        for (QuestionSection section : questionSections) {
            for (TestCaseResult result : section.results()) {
                remaining.remove(result);
            }
        }
        return List.copyOf(remaining);
    }

    static void appendCases(StringBuilder html, List<QuestionSection> questionSections,
                            List<TestCaseResult> unmatchedCases) {
        html.append("<section class=\"detail-block case-results\"><h3>Question and case results</h3>");
        if (questionSections.isEmpty() && unmatchedCases.isEmpty()) {
            html.append("<p>No case results recorded.</p></section>\n");
            return;
        }
        boolean hasAnyResults = false;
        for (QuestionSection section : questionSections) {
            hasAnyResults |= !section.results().isEmpty();
            appendQuestionCaseGroup(html, section);
        }
        if (!unmatchedCases.isEmpty()) {
            hasAnyResults = true;
            html.append("<section class=\"question-group other-cases\"><h4>Other recorded cases</h4>");
            appendCaseTable(html, unmatchedCases);
            html.append("</section>\n");
        }
        if (!hasAnyResults) {
            html.append("<p>No case results recorded.</p>");
        } else {
            html.append("<p class=\"metric-description\">COMPILE_ERROR durations refer to ")
                    .append("the compilation attempt and can repeat across its cases.</p>");
        }
        html.append("</section>\n");
    }

    static void appendQuestionScoreCell(StringBuilder html, QuestionSection section,
                                        boolean withheld) {
        QuestionOutcome outcome = section.outcome();
        String bandClass = outcome.hasCreditBand()
                ? "credit-band-" + outcome.creditBand().cssName() : "credit-band-neutral";
        html.append("<td class=\"numeric question-score-cell ").append(bandClass).append("\">")
                .append("<a class=\"question-score-link\" href=\"#submission-")
                .append(section.submissionOrdinal()).append("-question-").append(section.questionIndex())
                .append("\">")
                .append("<span class=\"question-score-value\">")
                .append(HtmlEscaper.escape(HtmlScoreFormatter.formatOrUnavailable(outcome.earnedScore()))).append(" / ")
                .append(HtmlEscaper.escape(HtmlScoreFormatter.formatOrUnavailable(outcome.configuredMaximum())))
                .append("</span>");
        if (outcome.hasCreditBand()) {
            html.append("<span class=\"credit-band-label\">")
                    .append(HtmlEscaper.escape(outcome.creditBand().label())).append(" of maximum</span>");
        }
        if (!outcome.statusCounts().isEmpty()) {
            html.append("<span class=\"question-status-counts\">");
            appendStatusCounts(html, outcome.statusCounts());
            html.append("</span>");
        }
        if (withheld) {
            html.append("<span class=\"diagnostic-label\">Diagnostic</span>");
        }
        for (String note : outcome.evidenceNotes()) {
            html.append("<span class=\"question-evidence-note\">")
                    .append(HtmlEscaper.escape(note)).append("</span>");
        }
        html.append("</a></td>");
    }

    private static void appendQuestionCaseGroup(StringBuilder html, QuestionSection section) {
        QuestionOutcome outcome = section.outcome();
        html.append("<section class=\"question-group\"><h4 class=\"question-group-title\" id=\"submission-")
                .append(section.submissionOrdinal()).append("-question-").append(section.questionIndex())
                .append("\" tabindex=\"-1\">Question ").append(HtmlEscaper.escape(section.question().getId()))
                .append("</h4><p class=\"question-evidence\">Recorded ")
                .append(outcome.recordedCaseCount()).append(" of ").append(outcome.configuredCaseCount())
                .append(" configured cases.</p>");
        if (!outcome.statusCounts().isEmpty()) {
            html.append("<p class=\"question-status-counts\">");
            appendStatusCounts(html, outcome.statusCounts());
            html.append("</p>");
        }
        for (String note : outcome.evidenceNotes()) {
            html.append("<p class=\"question-evidence-note\">").append(HtmlEscaper.escape(note)).append("</p>");
        }
        if (section.results().isEmpty()) {
            html.append("<p>No case results recorded for this question.</p>");
        } else {
            appendCaseTable(html, section.results());
        }
        html.append("</section>\n");
    }

    private static void appendCaseTable(StringBuilder html, List<TestCaseResult> results) {
        html.append("<div class=\"table-scroll compact-table\"><table><thead><tr><th scope=\"col\">Question</th>")
                .append("<th scope=\"col\">Case</th><th scope=\"col\">Status</th><th scope=\"col\">Points</th>")
                .append("<th scope=\"col\">Recorded duration</th><th scope=\"col\">Recorded output</th>")
                .append("</tr></thead><tbody>");
        for (TestCaseResult result : results) {
            html.append("<tr><td>").append(HtmlEscaper.escape(result.getTestCase().getQuestionId()))
                    .append("</td><td>").append(HtmlEscaper.escape(result.getTestCase().getId()))
                    .append("</td><td><span class=\"case-status\">")
                    .append(HtmlEscaper.escape(result.getStatus().name()))
                    .append("</span></td><td class=\"numeric\">")
                    .append(HtmlScoreFormatter.format(HtmlScoreFormatter.caseScore(result))).append(" / ")
                    .append(HtmlScoreFormatter.format(BigDecimal.valueOf(result.getTestCase().getMaximumScore())))
                    .append("</td><td class=\"numeric\">").append(result.getDurationMillis())
                    .append(" ms</td><td>");
            appendOutputDetails(html, "stdout", result.getStandardOutput());
            appendOutputDetails(html, "stderr", result.getStandardError());
            html.append("</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private static void appendStatusCounts(StringBuilder html, List<QuestionOutcome.StatusCount> counts) {
        boolean first = true;
        for (QuestionOutcome.StatusCount count : counts) {
            if (!first) {
                html.append(" · ");
            }
            first = false;
            html.append(HtmlEscaper.escape(count.label())).append(": ").append(count.count());
        }
    }

    private static void appendOutputDetails(StringBuilder html, String label, String output) {
        html.append("<details class=\"output-details\"><summary>").append(label).append("</summary>");
        if (output.isEmpty()) {
            html.append("<p>No output recorded.</p>");
        } else {
            html.append("<pre class=\"output-block\">").append(HtmlEscaper.escape(output)).append("</pre>");
        }
        html.append("</details>");
    }
}
