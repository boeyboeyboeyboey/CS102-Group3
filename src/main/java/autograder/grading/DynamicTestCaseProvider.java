package autograder.grading;

import autograder.api.TestCaseProvider;
import autograder.execution.StudentCompiler;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic test case provider for the CS102 auto-grader (dynamic parameterized tests).
 *
 * It emits a fixed number of dynamic cases per question. Each case is executed by
 * {@code DynamicRunner} (a default-package class compiled alongside the student code, see
 * {@code StudentCompiler}) which generates random inputs and checks a property/invariant of the
 * question -- it never relies on a pre-computed expected answer, so the team does not have to act
 * as an oracle. The seed makes every run reproducible.
 *
 * The case count is deliberately fixed so that a run is comparable between students; the combined
 * provider divides each question's configured maximum over all of its cases, so the case count
 * changes the points per case but not the question total.
 */
public class DynamicTestCaseProvider implements TestCaseProvider {

    private static final int CASES_PER_QUESTION = 5;
    private static final long SEED_BASE = 0x9E3779B1L;
    private static final long QUESTION_SEED_STEP = 1_000_003L;
    private static final long CASE_SEED_STEP = 7_919L;

    @Override
    public List<TestCaseDefinition> load(AssessmentDefinition assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("assessment is required");
        }

        List<TestCaseDefinition> definitions = new ArrayList<>();
        int questionIndex = 0;
        for (QuestionDefinition question : assessment.getQuestions()) {
            String questionId = question.getId();
            double pointsPerCase = question.getPointsPerCase();
            for (int caseNumber = 0; caseNumber < CASES_PER_QUESTION; caseNumber++) {
                long seed = SEED_BASE + questionIndex * QUESTION_SEED_STEP + caseNumber * CASE_SEED_STEP;
                definitions.add(new TestCaseDefinition(
                        questionId + "-dynamic-" + (caseNumber + 1),
                        questionId,
                        StudentCompiler.DYNAMIC_RUNNER_CLASS,
                        List.of(questionId, Long.toString(seed)),
                        pointsPerCase));
            }
            questionIndex++;
        }
        return List.copyOf(definitions);
    }
}
