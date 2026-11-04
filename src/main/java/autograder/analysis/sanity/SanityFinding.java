package autograder.analysis.sanity;

import java.util.Objects;

public final class SanityFinding {
    private final SanityCheck check;
    private final String questionId;
    private final String caseId;
    private final int affected;
    private final int graded;
    private final String message;

    public SanityFinding(SanityCheck check, String questionId, String caseId, int affected,
                         int graded, String message) {
        this.check = Objects.requireNonNull(check);
        this.questionId = Objects.requireNonNull(questionId);
        this.caseId = Objects.requireNonNull(caseId);
        this.affected = affected;
        this.graded = graded;
        this.message = Objects.requireNonNull(message);
    }

    public SanityCheck getCheck() {
        return check;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getCaseId() {
        return caseId;
    }

    public int getAffected() {
        return affected;
    }

    public int getGraded() {
        return graded;
    }

    public String getMessage() {
        return message;
    }
}
