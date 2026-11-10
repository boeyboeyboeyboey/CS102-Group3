package autograder.api;

import autograder.model.AnalysisFinding;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Compares one submission against the supplied starter code and reports what is unchanged. */
public interface BoilerplateDetector {
    /** Returns findings for the submission, or an empty list when it differs from the starter code. */
    List<AnalysisFinding> analyze(SubmissionResult submission, Path starterDirectory)
            throws IOException;
}
