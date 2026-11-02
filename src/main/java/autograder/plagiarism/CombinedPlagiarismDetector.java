package autograder.plagiarism;

import autograder.api.PlagiarismDetector;
import autograder.model.AssessmentDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// runs each configured algorithm; one failing algorithm is reported without discarding the others
public final class CombinedPlagiarismDetector implements PlagiarismDetector {
    private final Map<String, PlagiarismDetector> detectors;

    public CombinedPlagiarismDetector(Map<String, PlagiarismDetector> detectors) {
        this.detectors = new LinkedHashMap<>(detectors);
    }

    @Override
    public List<SimilarityFinding> compare(List<SubmissionResult> submissions,
                                           AssessmentDefinition assessment) {
        List<SimilarityFinding> findings = new ArrayList<>();
        for (Map.Entry<String, PlagiarismDetector> entry : detectors.entrySet()) {
            try {
                findings.addAll(entry.getValue().compare(submissions, assessment));
            } catch (IOException | RuntimeException exception) {
                System.err.println("similarity algorithm " + entry.getKey() + " skipped: "
                        + exception.getMessage());
            }
        }
        findings.sort(Comparator.comparing(SimilarityFinding::getQuestion)
                .thenComparing(Comparator.comparingDouble(SimilarityFinding::getSimilarity).reversed()));
        return findings;
    }
}
