package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.ScoreSheet;

import java.io.IOException;
import java.util.List;

/** Checks that every score sheet row carries the configured assessment maximum as its denominator. */
final class AssessmentScoreCheck {
    private final AppConfig config;

    AssessmentScoreCheck(AppConfig config) {
        this.config = config;
    }

    void validate(ScoreSheet scoreSheet) throws IOException {
        int denominatorIndex = -1;
        String denominatorColumn = scoreSheet.getColumns().get("denominator");
        for (int index = 0; index < scoreSheet.getHeaders().size(); index++) {
            if (scoreSheet.getHeaders().get(index).equals(denominatorColumn)) {
                denominatorIndex = index;
                break;
            }
        }
        if (denominatorIndex < 0) {
            throw new IOException("score sheet denominator column is unavailable");
        }
        for (int rowIndex = 0; rowIndex < scoreSheet.getRows().size(); rowIndex++) {
            List<String> row = scoreSheet.getRows().get(rowIndex);
            if (row.size() <= denominatorIndex) {
                throw new IOException("score sheet row " + (rowIndex + 1)
                        + " has too few columns for the denominator: " + row);
            }
            double denominator;
            try {
                denominator = Double.parseDouble(row.get(denominatorIndex).trim());
            } catch (NumberFormatException exception) {
                throw new IOException("score sheet row has an invalid denominator", exception);
            }
            if (Double.compare(denominator, config.getAssessment().getMaxScore()) != 0) {
                throw new IOException("score sheet denominator does not match configured assessment max score");
            }
        }
    }
}
