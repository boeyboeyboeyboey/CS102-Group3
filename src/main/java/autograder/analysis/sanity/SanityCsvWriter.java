package autograder.analysis.sanity;

import autograder.csv.CsvCodec;
import java.util.ArrayList;
import java.util.List;

public final class SanityCsvWriter {
    private SanityCsvWriter() {
    }

    public static String format(List<SanityFinding> findings) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Check", "Question", "Case", "Affected", "Graded", "Message"));
        for (SanityFinding finding : findings) {
            rows.add(List.of(finding.getCheck().name(), finding.getQuestionId(), finding.getCaseId(),
                    String.valueOf(finding.getAffected()), String.valueOf(finding.getGraded()),
                    finding.getMessage()));
        }
        return CsvCodec.write(rows);
    }
}
