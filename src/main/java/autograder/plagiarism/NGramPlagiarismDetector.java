package autograder.plagiarism;

import autograder.api.PlagiarismDetector;
import autograder.config.PlagiarismConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// flags pairs for instructor review; a high score is evidence to inspect, not proof of copying
public final class NGramPlagiarismDetector implements PlagiarismDetector {
    public static final String ALGORITHM_NAME = "n-gram";
    private static final int MAX_EVIDENCE_BLOCKS = 5;
    private static final double PERCENT = 100.0;

    private final PlagiarismConfig config;
    private final JavaTokenizer tokenizer = new JavaTokenizer();
    private final NGramBuilder builder;
    private final SimilarityCalculator calculator = new SimilarityCalculator();

    public NGramPlagiarismDetector(PlagiarismConfig config) {
        this.config = config;
        this.builder = new NGramBuilder(config.ngramSize());
    }

    @Override
    public List<SimilarityFinding> compare(List<SubmissionResult> submissions,
                                           AssessmentDefinition assessment) throws IOException {
        List<SimilarityFinding> findings = new ArrayList<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            Map<String, LineRange> starter = fingerprint(config.starterDirectory(), question);
            Set<String> starterGrams = starter == null ? Set.of() : starter.keySet();
            List<SubmissionResult> owners = new ArrayList<>();
            List<Map<String, LineRange>> fingerprints = new ArrayList<>();
            for (SubmissionResult submission : submissions) {
                if (submission.getWorkingRoot() == null) {
                    continue;
                }
                Map<String, LineRange> grams = fingerprint(submission.getWorkingRoot(), question);
                if (grams == null) {
                    continue;
                }
                grams.keySet().removeAll(starterGrams);
                if (grams.size() >= config.minimumNgrams()) {
                    owners.add(submission);
                    fingerprints.add(grams);
                }
            }
            for (int first = 0; first < owners.size(); first++) {
                for (int second = first + 1; second < owners.size(); second++) {
                    SimilarityFinding finding = comparePair(question, owners.get(first),
                            fingerprints.get(first), owners.get(second), fingerprints.get(second));
                    if (finding != null) {
                        findings.add(finding);
                    }
                }
            }
        }
        findings.sort(Comparator.comparingDouble(SimilarityFinding::getSimilarity).reversed());
        return findings;
    }

    private SimilarityFinding comparePair(QuestionDefinition question,
                                          SubmissionResult firstSubmission, Map<String, LineRange> first,
                                          SubmissionResult secondSubmission, Map<String, LineRange> second) {
        double similarity = calculator.jaccard(first.keySet(), second.keySet());
        if (similarity < config.threshold()) {
            return null;
        }
        int shared = calculator.sharedCount(first.keySet(), second.keySet());
        List<String> evidence = new ArrayList<>();
        evidence.add(String.format(Locale.ROOT,
                "%s (%s): %.1f%% similar; %d shared %d-token sequences out of %d distinct, starter code excluded",
                question.getId(), ALGORITHM_NAME, similarity * PERCENT, shared, config.ngramSize(),
                first.size() + second.size() - shared));
        for (LineRange[] block : matchingBlocks(first, second)) {
            String line = "first " + block[0].describe() + " matches second " + block[1].describe();
            if (!evidence.contains(line)) {
                evidence.add(line);
            }
        }
        return new SimilarityFinding(firstSubmission.getArchivePath(), secondSubmission.getArchivePath(),
                similarity, evidence, question.getId(), ALGORITHM_NAME);
    }

    private List<LineRange[]> matchingBlocks(Map<String, LineRange> first, Map<String, LineRange> second) {
        List<LineRange[]> blocks = new ArrayList<>();
        LineRange[] current = null;
        for (Map.Entry<String, LineRange> entry : first.entrySet()) {
            LineRange other = second.get(entry.getKey());
            if (other == null) {
                continue;
            }
            if (current != null && current[0].continuesInto(entry.getValue())
                    && current[1].continuesInto(other)) {
                current[0] = current[0].extendTo(entry.getValue());
                current[1] = current[1].extendTo(other);
            } else {
                current = new LineRange[] {entry.getValue(), other};
                blocks.add(current);
            }
        }
        blocks.sort(Comparator.comparingInt((LineRange[] block) -> block[0].endLine() - block[0].startLine())
                .reversed());
        if (blocks.size() > MAX_EVIDENCE_BLOCKS) {
            return blocks.subList(0, MAX_EVIDENCE_BLOCKS);
        }
        return blocks;
    }

    // returns null when the main answer file is missing so absent work is never compared
    private Map<String, LineRange> fingerprint(Path root, QuestionDefinition question) {
        List<Path> files = QuestionFiles.locate(root, question);
        if (files.isEmpty()) {
            return null;
        }
        Map<String, LineRange> grams = new LinkedHashMap<>();
        for (Path file : files) {
            try {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Map<String, LineRange> fileGrams = builder.build(file.getFileName().toString(),
                        tokenizer.tokenize(source));
                for (Map.Entry<String, LineRange> entry : fileGrams.entrySet()) {
                    grams.putIfAbsent(entry.getKey(), entry.getValue());
                }
            } catch (IOException exception) {
                if (file.equals(files.get(0))) {
                    return null;
                }
            }
        }
        return grams;
    }
}
