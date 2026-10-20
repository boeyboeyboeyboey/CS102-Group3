package autograder.grading;

import autograder.api.TestCaseProvider;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines fixed and dynamic test case providers.
 */
public class CombinedTestCaseProvider implements TestCaseProvider {

    private final TestCaseProvider fixedProvider;
    private final TestCaseProvider dynamicProvider;

    public CombinedTestCaseProvider(TestCaseProvider fixedProvider, TestCaseProvider dynamicProvider) {
        this.fixedProvider = fixedProvider;
        this.dynamicProvider = dynamicProvider;
    }

    @Override
    public List<TestCaseDefinition> load(AssessmentDefinition assessment) throws IOException {
        List<TestCaseDefinition> fixed = fixedProvider.load(assessment);
        List<TestCaseDefinition> dynamic = dynamicProvider.load(assessment);
        List<TestCaseDefinition> combined = new ArrayList<>(fixed.size() + dynamic.size());
        combined.addAll(fixed);
        combined.addAll(dynamic);
        return applyQuestionWeights(assessment, combined);
    }

    private List<TestCaseDefinition> applyQuestionWeights(AssessmentDefinition assessment,
                                                           List<TestCaseDefinition> cases) {
        Map<String, List<TestCaseDefinition>> casesByQuestion = new HashMap<>();
        for (TestCaseDefinition testCase : cases) {
            casesByQuestion.computeIfAbsent(testCase.getQuestionId(), ignored -> new ArrayList<>())
                    .add(testCase);
        }

        Map<String, TestCaseDefinition> weightedCasesById = new HashMap<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            List<TestCaseDefinition> questionCases = casesByQuestion.get(question.getId());
            if (questionCases == null || questionCases.isEmpty()) {
                throw new IllegalArgumentException("no test cases were provided for " + question.getId());
            }

            BigDecimal questionMaximum = question.getMaxScoreDecimal();
            BigDecimal caseMaximum = questionMaximum.divide(
                    BigDecimal.valueOf(questionCases.size()), MathContext.DECIMAL128);
            BigDecimal recordedCaseMaximum = BigDecimal.valueOf(caseMaximum.doubleValue());
            BigDecimal assignedMaximum = BigDecimal.ZERO;
            for (int index = 0; index < questionCases.size(); index++) {
                TestCaseDefinition original = questionCases.get(index);
                BigDecimal maximum = index == questionCases.size() - 1
                        ? questionMaximum.subtract(assignedMaximum)
                        : recordedCaseMaximum;
                double numericMaximum = maximum.doubleValue();
                if (index == questionCases.size() - 1
                        && BigDecimal.valueOf(numericMaximum).compareTo(maximum) > 0) {
                    numericMaximum = Math.nextDown(numericMaximum);
                }
                weightedCasesById.put(original.getId(), new TestCaseDefinition(
                        original.getId(), original.getQuestionId(), original.getRunnerClass(),
                        original.getArguments(), numericMaximum));
                assignedMaximum = assignedMaximum.add(maximum);
            }
        }

        List<TestCaseDefinition> weighted = new ArrayList<>(cases.size());
        for (TestCaseDefinition testCase : cases) {
            TestCaseDefinition replacement = weightedCasesById.get(testCase.getId());
            if (replacement == null) {
                throw new IllegalArgumentException("test case refers to an unknown question: "
                        + testCase.getQuestionId());
            }
            weighted.add(replacement);
        }
        return List.copyOf(weighted);
    }
}
