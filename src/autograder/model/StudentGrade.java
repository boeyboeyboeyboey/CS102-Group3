package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete grading outcome for a rostered student. */
public final class StudentGrade {
    private final StudentRecord student;
    private final Map<String, QuestionResult> questionResults;
    private final List<Anomaly> anomalies;

    public StudentGrade(StudentRecord student, Map<String, QuestionResult> questionResults,
            List<Anomaly> anomalies) {
        this.student = student;
        this.questionResults = Collections.unmodifiableMap(new LinkedHashMap<>(questionResults));
        this.anomalies = Collections.unmodifiableList(new ArrayList<>(anomalies));
    }

    public StudentRecord getStudent() {
        return student;
    }

    public Map<String, QuestionResult> getQuestionResults() {
        return questionResults;
    }

    public List<Anomaly> getAnomalies() {
        return anomalies;
    }

    public double getTotalScore() {
        double total = 0.0;
        for (QuestionResult result : questionResults.values()) {
            total += result.getScore();
        }
        return total;
    }
}
