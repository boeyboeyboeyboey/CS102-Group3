package autograder.api;

import autograder.model.StudentGrade;
import java.io.IOException;
import java.nio.file.Path;

/** Writes the per-student feedback file for one graded submission. */
public interface FeedbackGenerator {
    /** Writes the feedback file for the student into the directory and returns its path. */
    Path generate(StudentGrade grade, Path outputDirectory) throws IOException;
}
