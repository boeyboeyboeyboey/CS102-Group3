package autograder.api;

import autograder.model.AssessmentDefinition;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.util.List;

/** Supplies the test cases to run for an assessment. */
public interface TestCaseProvider {
    /** Returns every test case to run for the assessment. */
    List<TestCaseDefinition> load(AssessmentDefinition assessment) throws IOException;
}
