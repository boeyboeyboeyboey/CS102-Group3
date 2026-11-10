package autograder.api;

import autograder.model.AssessmentDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.util.List;

/** Compares submissions with each other and reports suspiciously similar pairs. */
public interface PlagiarismDetector {
    /** Returns the pairs of submissions that reach the detector's reporting threshold. */
    List<SimilarityFinding> compare(List<SubmissionResult> submissions,
                                    AssessmentDefinition assessment) throws IOException;
}
