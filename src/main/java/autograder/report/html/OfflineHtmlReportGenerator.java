package autograder.report.html;

import autograder.api.HtmlReportGenerator;
import autograder.config.HtmlReportConfig;
import autograder.model.BatchResult;
import autograder.model.GradingReportContext;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/** Writes the single-file HTML dashboard with its stylesheet and script inlined, so it works offline. */
public final class OfflineHtmlReportGenerator implements HtmlReportGenerator {
    private static final String STYLE_RESOURCE = "/autograder/html/dashboard.css";
    private static final String SCRIPT_RESOURCE = "/autograder/html/dashboard.js";

    private final HtmlReportConfig config;

    public OfflineHtmlReportGenerator(String filename) {
        this.config = new HtmlReportConfig(true, filename);
    }

    @Override
    public Path generate(GradingReportContext context, Path outputDirectory) throws IOException {
        Objects.requireNonNull(context, "grading report context is required");
        Objects.requireNonNull(outputDirectory, "output directory is required");
        Files.createDirectories(outputDirectory);
        Path output = outputDirectory.resolve(config.file());
        Files.writeString(output, render(context), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return output;
    }

    private String render(GradingReportContext context) throws IOException {
        BatchResult batch = context.batch();
        String css = readResource(STYLE_RESOURCE);
        String script = readResource(SCRIPT_RESOURCE);
        List<StudentGrade> grades = batch.getGrades();
        List<QuestionDefinition> questions = batch.getAssessment().getQuestions();
        BigDecimal assessmentMaximum = HtmlScoreFormatter.assessmentMaximum(questions);
        int received = grades.size();
        int withheld = 0;
        int warningSubmissions = 0;
        for (StudentGrade grade : grades) {
            if (context.withheld().containsKey(grade.getSubmission())) {
                withheld++;
            }
            if (!grade.getSubmission().getAnomalies().isEmpty()) {
                warningSubmissions++;
            }
        }
        int exported = received - withheld;

        StringBuilder html = new StringBuilder(24_000);
        appendDocumentHeader(html, batch, assessmentMaximum, css);

        HtmlSummaryRenderer.appendSummary(html, grades, context.withheld(), received, exported,
                withheld, warningSubmissions, assessmentMaximum);
        HtmlSubmissionTableRenderer.appendSection(html, grades, context.withheld(), questions,
                assessmentMaximum, received);
        HtmlAnalysisRenderer.appendAnalysisSection(html, batch.getFindings(), batch.getSimilarities());
        html.append("</main>\n<noscript><p class=\"noscript-note\">Filtering and sorting need JavaScript. ")
                .append("All submission details are still present in this report.</p></noscript>\n")
                .append("<script>\n").append(script).append("\n</script>\n</body>\n</html>\n");
        return html.toString();
    }

    private void appendDocumentHeader(StringBuilder html, BatchResult batch,
                                      BigDecimal assessmentMaximum, String css) {
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(HtmlEscaper.escape(batch.getAssessment().getName()))
                .append(" — Grading report</title>\n<style>\n")
                .append(css).append("\n</style>\n</head>\n<body>\n<main class=\"page\">\n")
                .append("<header class=\"report-header\">\n<p class=\"eyebrow\">Offline grading report</p>\n<h1>")
                .append(HtmlEscaper.escape(batch.getAssessment().getName())).append("</h1>\n")
                .append("<p class=\"assessment-meta\"><span>Assessment ID: <strong>")
                .append(HtmlEscaper.escape(batch.getAssessment().getId())).append("</strong></span>")
                .append("<span>Configured maximum: <strong>")
                .append(HtmlScoreFormatter.formatOrUnavailable(assessmentMaximum))
                .append(" points</strong></span></p>\n</header>\n");
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream input = OfflineHtmlReportGenerator.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("required HTML report resource is missing: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
