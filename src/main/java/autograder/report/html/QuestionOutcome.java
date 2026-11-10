package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/** One student's result for one question: earned and maximum score, status counts and credit band. */
final class QuestionOutcome {
    enum CreditBand {
        ZERO("zero", "0%"),
        LOW("low", ">0–<25%"),
        QUARTER("quarter", "25–<50%"),
        HALF("half", "50–<75%"),
        THREE_QUARTER("three-quarter", "75–<100%"),
        FULL("full", "100%");

        private final String cssName;
        private final String label;

        CreditBand(String cssName, String label) {
            this.cssName = cssName;
            this.label = label;
        }

        String cssName() {
            return cssName;
        }

        String label() {
            return label;
        }
    }

    record StatusCount(TestStatus status, String label, int count) { }

    private final int configuredCaseCount;
    private final int recordedCaseCount;
    private final BigDecimal configuredMaximum;
    private final BigDecimal earnedScore;
    private final List<StatusCount> statusCounts;
    private final List<String> evidenceNotes;
    private final CreditBand creditBand;

    private QuestionOutcome(QuestionDefinition question, BigDecimal earnedScore,
                            List<TestCaseResult> results) {
        configuredCaseCount = question.getCaseCount();
        recordedCaseCount = results.size();
        configuredMaximum = configuredMaximum(question);
        this.earnedScore = earnedScore;

        EnumMap<TestStatus, Integer> counts = countStatuses(results);
        statusCounts = nonzeroStatusCounts(counts);
        evidenceNotes = List.copyOf(buildNotes(configuredCaseCount, recordedCaseCount, counts,
                configuredMaximum, earnedScore));
        creditBand = evidenceNotes.isEmpty()
                ? bandFor(earnedScore, configuredMaximum) : null;
    }

    private static EnumMap<TestStatus, Integer> countStatuses(List<TestCaseResult> results) {
        EnumMap<TestStatus, Integer> counts = new EnumMap<>(TestStatus.class);
        for (TestStatus status : TestStatus.values()) {
            counts.put(status, 0);
        }
        for (TestCaseResult result : results) {
            counts.compute(result.getStatus(), (status, count) -> count + 1);
        }
        return counts;
    }

    private List<StatusCount> nonzeroStatusCounts(EnumMap<TestStatus, Integer> counts) {
        List<StatusCount> nonzeroCounts = new ArrayList<>();
        for (TestStatus status : TestStatus.values()) {
            int count = counts.get(status);
            if (count > 0) {
                nonzeroCounts.add(new StatusCount(status, statusLabel(status), count));
            }
        }
        return List.copyOf(nonzeroCounts);
    }

    private static List<String> buildNotes(int configuredCaseCount, int recordedCaseCount,
                                           EnumMap<TestStatus, Integer> counts,
                                           BigDecimal configuredMaximum, BigDecimal earnedScore) {
        List<String> notes = new ArrayList<>();
        if (recordedCaseCount == 0) {
            notes.add("No case results recorded.");
        }
        if (recordedCaseCount != configuredCaseCount) {
            notes.add("Recorded " + recordedCaseCount + " of " + configuredCaseCount
                    + " configured cases.");
        }
        if (recordedCaseCount > 0 && counts.get(TestStatus.NOT_RUN) == recordedCaseCount) {
            notes.add("All recorded cases are NOT_RUN.");
        }
        if (configuredMaximum == null) {
            notes.add("Configured maximum is unavailable because the question score configuration is non-finite.");
        } else if (configuredMaximum.signum() <= 0) {
            notes.add("Configured maximum is not positive; credit shading is unavailable.");
        }
        if (earnedScore == null) {
            notes.add("Question score is unavailable.");
        } else if (configuredMaximum != null && configuredMaximum.signum() > 0
                && (earnedScore.signum() < 0 || earnedScore.compareTo(configuredMaximum) > 0)) {
            notes.add("Question score is outside the configured range.");
        }
        return notes;
    }

    static QuestionOutcome from(QuestionDefinition question, BigDecimal earnedScore,
                                List<TestCaseResult> matchingResults) {
        Objects.requireNonNull(question, "question is required");
        return new QuestionOutcome(question, earnedScore, List.copyOf(matchingResults));
    }

    int configuredCaseCount() {
        return configuredCaseCount;
    }

    int recordedCaseCount() {
        return recordedCaseCount;
    }

    BigDecimal configuredMaximum() {
        return configuredMaximum;
    }

    BigDecimal earnedScore() {
        return earnedScore;
    }

    List<StatusCount> statusCounts() {
        return statusCounts;
    }

    List<String> evidenceNotes() {
        return evidenceNotes;
    }

    CreditBand creditBand() {
        return creditBand;
    }

    boolean hasCreditBand() {
        return creditBand != null;
    }

    static BigDecimal configuredMaximum(QuestionDefinition question) {
        if (!Double.isFinite(question.getPointsPerCase())) {
            return null;
        }
        return BigDecimal.valueOf(question.getPointsPerCase())
                .multiply(BigDecimal.valueOf(question.getCaseCount()));
    }

    private CreditBand bandFor(BigDecimal score, BigDecimal maximum) {
        if (score.signum() == 0) {
            return CreditBand.ZERO;
        }
        if (score.multiply(BigDecimal.valueOf(4)).compareTo(maximum) < 0) {
            return CreditBand.LOW;
        }
        if (score.multiply(BigDecimal.valueOf(2)).compareTo(maximum) < 0) {
            return CreditBand.QUARTER;
        }
        if (score.multiply(BigDecimal.valueOf(4))
                .compareTo(maximum.multiply(BigDecimal.valueOf(3))) < 0) {
            return CreditBand.HALF;
        }
        if (score.compareTo(maximum) < 0) {
            return CreditBand.THREE_QUARTER;
        }
        return CreditBand.FULL;
    }

    private String statusLabel(TestStatus status) {
        return switch (status) {
            case PASSED -> "Passed";
            case FAILED -> "Failed";
            case TIMED_OUT -> "Timed out";
            case COMPILE_ERROR -> "Compile error";
            case RUNTIME_ERROR -> "Runtime error";
            case NOT_RUN -> "Not run";
        };
    }
}
