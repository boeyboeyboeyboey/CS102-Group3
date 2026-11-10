package autograder.grading;

import autograder.api.TestCaseProvider;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides the fixed instructor cases for the supplied assessment. The per-question case counts and
 * tester SHA-256 pins are deliberate: the bundled adapters are written against exactly these
 * testers, so an edited tester or a different case count must be rejected until its adapter is reviewed.
 */
public final class InstructorTestCaseProvider implements TestCaseProvider {
    private static final Map<String, Integer> CASE_COUNTS = Map.of(
            "Q1a", 3, "Q1b", 3, "Q2a", 5, "Q2b", 5, "Q3", 4);
    private static final Map<String, String> TESTER_SHA256 = Map.of(
            "Q1a", "b372e3289311057d8d4aae8144ee08e30411d6070242b2a0e0c4341707e5802d",
            "Q1b", "f7a8b5d31048c82c165f2444215e106ba58957bddb755214ea81c32c554812de",
            "Q2a", "b90d0d9d163a26edb00939f840737758c4e6a96814a184fa2b5022748e10a5ca",
            "Q2b", "324e250d8a42f97d515b04a13ec614f2d550a79318a1bc42fb6982c835a11940",
            "Q3", "d55d3937b4e8e49c423ed33b4d9f307300e4fbb9f72dbc2482c2e84eee516e29");

    @Override
    public List<TestCaseDefinition> load(AssessmentDefinition assessment) throws IOException {
        if (assessment == null) {
            throw new IllegalArgumentException("assessment is required");
        }
        if (assessment.getQuestions().size() != CASE_COUNTS.size()) {
            throw new IllegalArgumentException("unsupported assessment question set");
        }

        List<TestCaseDefinition> definitions = new ArrayList<>();
        Set<String> seenQuestionIds = new HashSet<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            String questionId = question.getId();
            Integer expectedCount = CASE_COUNTS.get(questionId);
            if (expectedCount == null || !seenQuestionIds.add(questionId)) {
                throw new IllegalArgumentException("unsupported or duplicate question: " + questionId);
            }
            if (question.getCaseCount() != expectedCount) {
                throw new IllegalArgumentException("unsupported case count for " + questionId
                        + ": expected " + expectedCount + " but found " + question.getCaseCount());
            }
            validateTester(questionId, question.getTesterFile());
            if (!Double.isFinite(question.getPointsPerCase()) || question.getPointsPerCase() <= 0) {
                throw new IllegalArgumentException("invalid points per case for " + questionId);
            }
            String adapterClass = questionId + "Adapter";
            for (int caseNumber = 1; caseNumber <= expectedCount; caseNumber++) {
                definitions.add(new TestCaseDefinition(questionId + "-" + caseNumber,
                        questionId, adapterClass, List.of(Integer.toString(caseNumber)),
                        question.getPointsPerCase()));
            }
        }
        if (seenQuestionIds.size() != CASE_COUNTS.size()) {
            throw new IllegalArgumentException("assessment is missing an instructor question");
        }
        return List.copyOf(definitions);
    }

    private static void validateTester(String questionId, java.nio.file.Path testerFile)
            throws IOException {
        if (!Files.isRegularFile(testerFile)) {
            throw new IOException("fixed instructor tester is missing for " + questionId + ": "
                    + testerFile);
        }
        String actualHash;
        try {
            String source = Files.readString(testerFile, StandardCharsets.UTF_8)
                    .replace("\r\n", "\n").replace('\r', '\n');
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte value : digest) {
                hexadecimal.append(String.format("%02x", value));
            }
            actualHash = hexadecimal.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        if (!TESTER_SHA256.get(questionId).equals(actualHash)) {
            throw new IllegalArgumentException("unsupported instructor tester for " + questionId
                    + " at " + testerFile + "; fixed adapters require the supplied original tester");
        }
    }
}
