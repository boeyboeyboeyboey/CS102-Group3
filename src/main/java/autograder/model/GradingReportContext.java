package autograder.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** The completed run and the withheld submissions, as handed to report generators. */
public record GradingReportContext(BatchResult batch, Map<SubmissionResult, String> withheld) {
    public GradingReportContext {
        Objects.requireNonNull(batch);
        withheld = Map.copyOf(withheld);
        Set<SubmissionResult> submissions = new HashSet<>();
        for (StudentGrade grade : batch.getGrades()) {
            submissions.add(grade.getSubmission());
        }
        for (Map.Entry<SubmissionResult, String> entry : withheld.entrySet()) {
            if (!submissions.contains(entry.getKey()) || entry.getValue().isBlank()) {
                throw new IllegalArgumentException("withholding requires a batch submission and reason");
            }
        }
    }
}
