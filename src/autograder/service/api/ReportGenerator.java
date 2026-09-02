package autograder.service.api;

import autograder.model.StudentGrade;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Extension point for CSV, HTML, or other report formats. */
public interface ReportGenerator {
    Path generate(List<StudentGrade> grades) throws IOException;
}
