package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.TestCaseResult;

import java.math.BigDecimal;
import java.util.List;

/** Computes and formats the scores shown in the dashboard. */
final class HtmlScoreFormatter {
    static final int AVERAGE_DECIMAL_PLACES = 4;

    private HtmlScoreFormatter() { }

    static BigDecimal totalScore(StudentGrade grade) {
        return BigDecimal.valueOf(grade.getTotalScore());
    }

    static BigDecimal questionScore(StudentGrade grade, String questionId) {
        double score = grade.getQuestionScore(questionId);
        return Double.isFinite(score) ? BigDecimal.valueOf(score) : null;
    }

    static BigDecimal caseScore(TestCaseResult result) {
        return BigDecimal.valueOf(result.getScore());
    }

    static String format(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.signum() == 0 ? "0" : normalized.toPlainString();
    }

    static String formatOrUnavailable(BigDecimal value) {
        return value == null ? "N/A" : format(value);
    }

    static BigDecimal assessmentMaximum(List<QuestionDefinition> questions) {
        BigDecimal maximum = BigDecimal.ZERO;
        for (QuestionDefinition question : questions) {
            BigDecimal questionMaximum = QuestionOutcome.configuredMaximum(question);
            if (questionMaximum == null) {
                return null;
            }
            maximum = maximum.add(questionMaximum);
        }
        return maximum;
    }
}
