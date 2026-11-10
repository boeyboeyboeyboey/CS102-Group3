package autograder.api;

import autograder.model.GradingReportContext;
import java.io.IOException;
import java.nio.file.Path;

/** Renders the offline HTML dashboard for a completed grading run. */
public interface HtmlReportGenerator {
    /** Writes the dashboard into the directory and returns its path. */
    Path generate(GradingReportContext context, Path outputDirectory) throws IOException;
}
