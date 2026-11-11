package autograder.grading;

import autograder.config.AppConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicCaseCountTest {
    @Test
    void configuredCountSetsDynamicCasesPerPartAndKeepsEachQuestionMaximum() throws Exception {
        AssessmentDefinition assessment = AppConfig.load(Path.of("config.properties")).getAssessment();

        List<TestCaseDefinition> defaults = combined(assessment, new DynamicTestCaseProvider());
        List<TestCaseDefinition> three = combined(assessment, new DynamicTestCaseProvider(3));

        for (QuestionDefinition question : assessment.getQuestions()) {
            assertEquals(5, dynamicCount(defaults, question.getId()), question.getId());
            assertEquals(3, dynamicCount(three, question.getId()), question.getId());
            assertEquals(0, question.getMaxScoreDecimal().compareTo(total(three, question.getId())),
                    question.getId());
        }
        assertEquals("Q1a-dynamic-3", three.stream().filter(c -> c.getId().endsWith("dynamic-3"))
                .findFirst().orElseThrow().getId());
        assertThrows(IllegalArgumentException.class, () -> new DynamicTestCaseProvider(0));
    }

    private static List<TestCaseDefinition> combined(AssessmentDefinition assessment,
                                                      DynamicTestCaseProvider dynamic) throws Exception {
        return new CombinedTestCaseProvider(new InstructorTestCaseProvider(), dynamic).load(assessment);
    }

    private static long dynamicCount(List<TestCaseDefinition> cases, String questionId) {
        return cases.stream().filter(c -> c.getQuestionId().equals(questionId))
                .filter(c -> c.getId().contains("-dynamic-")).count();
    }

    private static BigDecimal total(List<TestCaseDefinition> cases, String questionId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TestCaseDefinition testCase : cases) {
            if (testCase.getQuestionId().equals(questionId)) {
                sum = sum.add(BigDecimal.valueOf(testCase.getMaximumScore()));
            }
        }
        return sum;
    }
}
