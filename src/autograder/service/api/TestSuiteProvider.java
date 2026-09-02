package autograder.service.api;

import autograder.model.QuestionSpec;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Converts an instructor-owned tester into independently runnable cases. */
public interface TestSuiteProvider {
    List<TestCaseDefinition> createCases(QuestionSpec question, Path testerSource,
            Path outputDirectory) throws IOException;
}
