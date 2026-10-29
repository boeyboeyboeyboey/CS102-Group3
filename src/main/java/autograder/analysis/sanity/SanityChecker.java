package autograder.analysis.sanity;

import autograder.config.SanityConfig;
import autograder.grading.GradingRun;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SanityChecker {
    private static final String DYNAMIC_MARKER = "-dynamic-";
    private static final int EXCERPT_LIMIT = 80;

    public List<SanityFinding> check(GradingRun run, SanityConfig config) {
        List<StudentGrade> graded = new ArrayList<>();
        for (StudentGrade grade : run.batch().getGrades()) {
            if (!run.withheld().containsKey(grade.getSubmission())) {
                graded.add(grade);
            }
        }
        List<SanityFinding> findings = new ArrayList<>();
        if (graded.size() < config.minGraded()) {
            return findings;
        }
        for (QuestionDefinition question : run.batch().getAssessment().getQuestions()) {
            checkQuestion(question.getId(), graded, config, findings);
        }
        return findings;
    }

    private void checkQuestion(String questionId, List<StudentGrade> graded, SanityConfig config,
                               List<SanityFinding> findings) {
        Map<String, List<TestCaseResult>> byCase = resultsByCase(questionId, graded);
        int total = graded.size();
        if (allCompileErrors(byCase, graded)) {
            findings.add(new SanityFinding(SanityCheck.ALL_COMPILE_ERRORS, questionId, "", total, total,
                    "every graded submission fails to compile " + questionId
                            + "; check support files or the configured file name"));
        } else {
            checkMostlyZero(questionId, byCase, graded, config, findings);
        }
        for (Map.Entry<String, List<TestCaseResult>> entry : byCase.entrySet()) {
            checkCase(questionId, entry.getKey(), entry.getValue(), total, config, findings);
        }
        checkGap(questionId, byCase, graded, config, findings);
    }

    private void checkMostlyZero(String questionId, Map<String, List<TestCaseResult>> byCase,
                                 List<StudentGrade> graded, SanityConfig config, List<SanityFinding> findings) {
        Set<String> freePass = freePassCases(byCase, graded);
        if (!byCase.isEmpty() && freePass.size() == byCase.size()) {
            // every case passed by all graded students leaves nothing to judge
            return;
        }
        int total = graded.size();
        int zero = 0;
        for (StudentGrade grade : graded) {
            if (scoreExcluding(grade, questionId, freePass) == 0) {
                zero++;
            }
        }
        if (meets(zero, config.zeroPercent(), total)) {
            findings.add(new SanityFinding(SanityCheck.QUESTION_MOSTLY_ZERO, questionId, "", zero, total,
                    "almost every graded submission scores zero on " + questionId
                            + "; check the tester, expected output or timeout"));
        }
    }

    private void checkCase(String questionId, String caseId, List<TestCaseResult> results, int total,
                           SanityConfig config, List<SanityFinding> findings) {
        Map<String, Integer> groups = new HashMap<>();
        Map<String, TestCaseResult> samples = new HashMap<>();
        int timeouts = 0;
        for (TestCaseResult result : results) {
            if (result.getStatus() == TestStatus.TIMED_OUT) {
                timeouts++;
            } else if (isFailure(result) && hasOutput(result)) {
                // silent failures share no evidence, so only matching printed output counts
                String key = result.getStatus() + "\u0000" + result.getStandardOutput().strip()
                        + "\u0000" + result.getStandardError().strip();
                groups.merge(key, 1, Integer::sum);
                samples.putIfAbsent(key, result);
            }
        }
        String largest = null;
        for (Map.Entry<String, Integer> group : groups.entrySet()) {
            if (largest == null || group.getValue() > groups.get(largest)) {
                largest = group.getKey();
            }
        }
        if (largest != null && meets(groups.get(largest), config.sameFailurePercent(), total)) {
            findings.add(new SanityFinding(SanityCheck.SHARED_FAILURE, questionId, caseId,
                    groups.get(largest), total, "submissions fail identically ("
                            + samples.get(largest).getStatus() + ": " + excerpt(samples.get(largest))
                            + "); check the expected output or tester"));
        }
        if (meets(timeouts, config.timeoutPercent(), total)) {
            findings.add(new SanityFinding(SanityCheck.CLUSTERED_TIMEOUTS, questionId, caseId, timeouts,
                    total, "many submissions time out; check the case timeout or tester input"));
        }
    }

    private void checkGap(String questionId, Map<String, List<TestCaseResult>> byCase, List<StudentGrade> graded,
                          SanityConfig config, List<SanityFinding> findings) {
        long fixedPassed = 0;
        long fixedTotal = 0;
        long dynamicPassed = 0;
        long dynamicTotal = 0;
        for (Map.Entry<String, List<TestCaseResult>> entry : byCase.entrySet()) {
            boolean dynamic = entry.getKey().contains(DYNAMIC_MARKER);
            for (TestCaseResult result : entry.getValue()) {
                if (result.getStatus() == TestStatus.COMPILE_ERROR) {
                    continue;
                }
                boolean passed = result.getStatus() == TestStatus.PASSED;
                if (dynamic) {
                    dynamicTotal++;
                    dynamicPassed += passed ? 1 : 0;
                } else {
                    fixedTotal++;
                    fixedPassed += passed ? 1 : 0;
                }
            }
        }
        if (fixedTotal == 0 || dynamicTotal == 0) {
            return;
        }
        long gapScaled = (fixedPassed * dynamicTotal - dynamicPassed * fixedTotal) * 100;
        if (gapScaled >= (long) config.fixedDynamicGapPoints() * fixedTotal * dynamicTotal) {
            DynamicCounts students = dynamicStudentCounts(questionId, graded);
            findings.add(new SanityFinding(SanityCheck.FIXED_DYNAMIC_GAP, questionId, "",
                    students.affected(), students.graded(),
                    "dynamic cases pass far less often than fixed cases on " + questionId
                            + " (" + fixedPassed + "/" + fixedTotal + " fixed case results, " + dynamicPassed + "/"
                            + dynamicTotal + " dynamic case results); check the dynamic test data or expected values"));
        }
    }

    private static DynamicCounts dynamicStudentCounts(String questionId, List<StudentGrade> graded) {
        int withDynamic = 0;
        int affected = 0;
        for (StudentGrade grade : graded) {
            List<TestCaseResult> dynamic = gradedDynamicResults(questionId, grade);
            if (dynamic.isEmpty()) {
                continue;
            }
            withDynamic++;
            if (dynamic.stream().anyMatch(result -> result.getStatus() != TestStatus.PASSED)) {
                affected++;
            }
        }
        return new DynamicCounts(affected, withDynamic);
    }

    private static List<TestCaseResult> gradedDynamicResults(String questionId, StudentGrade grade) {
        List<TestCaseResult> dynamic = new ArrayList<>();
        for (TestCaseResult result : grade.getTestResults()) {
            if (result.getTestCase().getQuestionId().equals(questionId)
                    && result.getTestCase().getId().contains(DYNAMIC_MARKER)
                    && result.getStatus() != TestStatus.COMPILE_ERROR) {
                dynamic.add(result);
            }
        }
        return dynamic;
    }

    private static Set<String> freePassCases(Map<String, List<TestCaseResult>> byCase,
                                             List<StudentGrade> graded) {
        Set<String> freePass = new HashSet<>();
        for (Map.Entry<String, List<TestCaseResult>> entry : byCase.entrySet()) {
            // results hold at most one entry per student, so full size means every graded student has one
            List<TestCaseResult> results = entry.getValue();
            if (results.size() == graded.size()
                    && results.stream().allMatch(result -> result.getStatus() == TestStatus.PASSED)) {
                freePass.add(entry.getKey());
            }
        }
        return freePass;
    }

    private static double scoreExcluding(StudentGrade grade, String questionId, Set<String> excludedCases) {
        BigDecimal total = BigDecimal.ZERO;
        for (TestCaseResult result : grade.getTestResults()) {
            if (result.getTestCase().getQuestionId().equals(questionId)
                    && !excludedCases.contains(result.getTestCase().getId())) {
                total = total.add(BigDecimal.valueOf(result.getScore()));
            }
        }
        return total.doubleValue();
    }

    private record DynamicCounts(int affected, int graded) {
    }

    private static Map<String, List<TestCaseResult>> resultsByCase(String questionId,
                                                                   List<StudentGrade> graded) {
        Map<String, List<TestCaseResult>> byCase = new LinkedHashMap<>();
        for (StudentGrade grade : graded) {
            for (TestCaseResult result : grade.getTestResults()) {
                if (result.getTestCase().getQuestionId().equals(questionId)) {
                    byCase.computeIfAbsent(result.getTestCase().getId(), id -> new ArrayList<>()).add(result);
                }
            }
        }
        return byCase;
    }

    private static boolean allCompileErrors(Map<String, List<TestCaseResult>> byCase,
                                            List<StudentGrade> graded) {
        if (byCase.isEmpty()) {
            return false;
        }
        for (List<TestCaseResult> results : byCase.values()) {
            if (results.size() != graded.size()) {
                return false;
            }
            for (TestCaseResult result : results) {
                if (result.getStatus() != TestStatus.COMPILE_ERROR) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isFailure(TestCaseResult result) {
        return result.getStatus() == TestStatus.FAILED || result.getStatus() == TestStatus.RUNTIME_ERROR;
    }

    private static boolean hasOutput(TestCaseResult result) {
        return !result.getStandardOutput().isBlank() || !result.getStandardError().isBlank();
    }

    private static boolean meets(int affected, int percent, int total) {
        return (long) affected * 100 >= (long) percent * total;
    }

    private static String excerpt(TestCaseResult result) {
        String text = result.getStandardOutput().strip();
        if (text.isEmpty()) {
            text = result.getStandardError().strip();
        }
        if (text.isEmpty()) {
            return "no output";
        }
        int newline = text.indexOf('\n');
        // student output must not send control or escape characters to the terminal
        String first = newline < 0 ? text : text.substring(0, newline);
        String line = first.replaceAll("\\p{Cntrl}", " ").strip();
        return line.length() > EXCERPT_LIMIT ? line.substring(0, EXCERPT_LIMIT) + "..." : line;
    }
}
