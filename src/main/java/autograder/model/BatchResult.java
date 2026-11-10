package autograder.model;

import java.util.List;
import java.util.Objects;

/** Everything one grading run produced: grades, findings and the assessment they belong to. */
public final class BatchResult {
    private final AssessmentDefinition assessment;
    private final List<StudentGrade> grades;
    private final List<AnalysisFinding> findings;
    private final List<SimilarityFinding> similarities;

    public BatchResult(AssessmentDefinition assessment, List<StudentGrade> grades,
                        List<AnalysisFinding> findings, List<SimilarityFinding> similarities) {
        this.assessment = Objects.requireNonNull(assessment);
        this.grades = List.copyOf(grades);
        this.findings = List.copyOf(findings);
        this.similarities = List.copyOf(similarities);
    }

    public AssessmentDefinition getAssessment() {
        return assessment;
    }
    public List<StudentGrade> getGrades() {
        return grades;
    }
    public List<AnalysisFinding> getFindings() {
        return findings;
    }
    public List<SimilarityFinding> getSimilarities() {
        return similarities;
    }
}
