package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseResult;

import java.util.List;

/** The cases and outcome of one question within one submission's detail view. */
record QuestionSection(QuestionDefinition question, int questionIndex,
                       List<TestCaseResult> results, int submissionOrdinal,
                       QuestionOutcome outcome) {
    QuestionSection {
        results = List.copyOf(results);
    }
}
