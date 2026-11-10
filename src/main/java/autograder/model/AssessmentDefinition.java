package autograder.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The assessment being graded: its identity and questions. */
public final class AssessmentDefinition {
    private final String id;
    private final String name;
    private final List<QuestionDefinition> questions;

    public AssessmentDefinition(String id, String name, List<QuestionDefinition> questions) {
        this.id = id;
        this.name = name;
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));
        BigDecimal maxScore = BigDecimal.ZERO;
        for (QuestionDefinition question : this.questions) {
            maxScore = maxScore.add(question.getMaxScoreDecimal());
        }
        double numericMaxScore = maxScore.doubleValue();
        if (!Double.isFinite(numericMaxScore) || numericMaxScore <= 0.0) {
            throw new IllegalArgumentException("assessment max score must be finite and positive");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<QuestionDefinition> getQuestions() {
        return questions;
    }

    public double getMaxScore() {
        BigDecimal maxScore = BigDecimal.ZERO;
        for (QuestionDefinition question : questions) {
            maxScore = maxScore.add(question.getMaxScoreDecimal());
        }
        return maxScore.doubleValue();
    }
}
