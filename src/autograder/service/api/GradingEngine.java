package autograder.service.api;

import autograder.model.NormalizedSubmission;
import autograder.model.StudentGrade;
import java.io.IOException;

/** Grades one normalized submission without controlling batch policy. */
public interface GradingEngine {
    StudentGrade grade(NormalizedSubmission submission, ProgressListener listener)
            throws IOException, InterruptedException;
}
