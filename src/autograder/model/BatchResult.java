package autograder.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Final batch results and generated report location. */
public final class BatchResult {
    private final List<StudentGrade> grades;
    private final Path reportPath;

    public BatchResult(List<StudentGrade> grades, Path reportPath) {
        this.grades = Collections.unmodifiableList(new ArrayList<>(grades));
        this.reportPath = reportPath;
    }

    public List<StudentGrade> getGrades() {
        return grades;
    }

    public Path getReportPath() {
        return reportPath;
    }
}
