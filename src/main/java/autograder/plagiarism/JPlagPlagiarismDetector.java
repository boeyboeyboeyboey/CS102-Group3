package autograder.plagiarism;

import autograder.api.PlagiarismDetector;
import autograder.config.PlagiarismConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.SubmissionResult;
import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.Match;
import de.jplag.Token;
import de.jplag.exceptions.ExitException;
import de.jplag.java.Language;
import de.jplag.options.JPlagOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// uses jplag's greedy string tiling over java syntax tokens; starter files are passed as base code
public final class JPlagPlagiarismDetector implements PlagiarismDetector {
    public static final String ALGORITHM_NAME = "JPlag";
    private static final int MAX_EVIDENCE_BLOCKS = 5;
    private static final int MINIMUM_SUBMISSIONS = 2;
    private static final double PERCENT = 100.0;
    private static final String SUBMISSION_PREFIX = "s";

    private final PlagiarismConfig config;

    public JPlagPlagiarismDetector(PlagiarismConfig config) {
        this.config = config;
    }

    @Override
    public List<SimilarityFinding> compare(List<SubmissionResult> submissions,
                                           AssessmentDefinition assessment) throws IOException {
        List<SimilarityFinding> findings = new ArrayList<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            findings.addAll(compareQuestion(submissions, question));
        }
        findings.sort(Comparator.comparingDouble(SimilarityFinding::getSimilarity).reversed());
        return findings;
    }

    private List<SimilarityFinding> compareQuestion(List<SubmissionResult> submissions,
                                                    QuestionDefinition question) throws IOException {
        Path workspace = Files.createTempDirectory("autograder-jplag-");
        try {
            Path submissionsFolder = Files.createDirectory(workspace.resolve("submissions"));
            Map<String, SubmissionResult> owners = copySubmissions(submissions, question, submissionsFolder);
            if (owners.size() < MINIMUM_SUBMISSIONS) {
                return List.of();
            }
            JPlagOptions options = new JPlagOptions(new Language(), Set.of(submissionsFolder.toFile()), Set.of())
                    .withMinimumTokenMatch(config.jplagMinimumTokens());
            boolean starterExcluded = copyFiles(QuestionFiles.locate(config.starterDirectory(), question),
                    workspace.resolve("starter"));
            if (starterExcluded) {
                options = options.withBaseCodeSubmissionDirectory(workspace.resolve("starter").toFile());
            }
            JPlagResult result;
            try {
                result = new JPlag(options).run();
            } catch (ExitException exception) {
                // jplag needs two parsable submissions; other questions are still compared
                System.err.println("JPlag skipped " + question.getId() + ": " + exception.getMessage());
                return List.of();
            }
            return findingsFor(question, result, owners, starterExcluded);
        } finally {
            deleteRecursively(workspace);
        }
    }

    private Map<String, SubmissionResult> copySubmissions(List<SubmissionResult> submissions,
                                                          QuestionDefinition question,
                                                          Path submissionsFolder) throws IOException {
        Map<String, SubmissionResult> owners = new LinkedHashMap<>();
        for (int index = 0; index < submissions.size(); index++) {
            SubmissionResult submission = submissions.get(index);
            if (submission.getWorkingRoot() == null) {
                continue;
            }
            String name = String.format(Locale.ROOT, "%s%04d", SUBMISSION_PREFIX, index);
            if (copyFiles(QuestionFiles.locate(submission.getWorkingRoot(), question),
                    submissionsFolder.resolve(name))) {
                owners.put(name, submission);
            }
        }
        return owners;
    }

    private boolean copyFiles(List<Path> files, Path target) throws IOException {
        if (files.isEmpty()) {
            return false;
        }
        Files.createDirectories(target);
        for (Path file : files) {
            Files.copy(file, target.resolve(file.getFileName()));
        }
        return true;
    }

    private List<SimilarityFinding> findingsFor(QuestionDefinition question, JPlagResult result,
                                                Map<String, SubmissionResult> owners, boolean starterExcluded) {
        List<SimilarityFinding> findings = new ArrayList<>();
        for (JPlagComparison comparison : result.getAllComparisons()) {
            double similarity = comparison.similarity();
            if (similarity < config.jplagThreshold()) {
                continue;
            }
            String firstName = comparison.firstSubmission().getName();
            String secondName = comparison.secondSubmission().getName();
            boolean swapped = firstName.compareTo(secondName) > 0;
            List<String> evidence = new ArrayList<>();
            evidence.add(String.format(Locale.ROOT,
                    "%s (%s): %.1f%% similar; %d matching blocks of at least %d tokens, %s",
                    question.getId(), ALGORITHM_NAME, similarity * PERCENT, comparison.matches().size(),
                    config.jplagMinimumTokens(),
                    starterExcluded ? "starter code excluded" : "no starter file found"));
            evidence.addAll(matchEvidence(comparison, swapped));
            SubmissionResult first = owners.get(swapped ? secondName : firstName);
            SubmissionResult second = owners.get(swapped ? firstName : secondName);
            findings.add(new SimilarityFinding(first.getArchivePath(), second.getArchivePath(), similarity,
                    evidence, question.getId(), ALGORITHM_NAME));
        }
        return findings;
    }

    private List<String> matchEvidence(JPlagComparison comparison, boolean swapped) {
        List<Match> matches = new ArrayList<>(comparison.matches());
        matches.sort(Comparator.comparingInt(Match::length).reversed());
        List<Token> firstTokens = comparison.firstSubmission().getTokenList();
        List<Token> secondTokens = comparison.secondSubmission().getTokenList();
        List<String> evidence = new ArrayList<>();
        for (Match match : matches) {
            if (evidence.size() == MAX_EVIDENCE_BLOCKS) {
                break;
            }
            LineRange firstRange = lines(firstTokens, match.startOfFirst(), match.length());
            LineRange secondRange = lines(secondTokens, match.startOfSecond(), match.length());
            LineRange shownFirst = swapped ? secondRange : firstRange;
            LineRange shownSecond = swapped ? firstRange : secondRange;
            String line = "first " + shownFirst.describe() + " matches second " + shownSecond.describe()
                    + " (" + match.length() + " tokens)";
            if (!evidence.contains(line)) {
                evidence.add(line);
            }
        }
        return evidence;
    }

    private LineRange lines(List<Token> tokens, int start, int length) {
        Token first = tokens.get(start);
        Token last = tokens.get(start + length - 1);
        return new LineRange(first.getFile().getName(), first.getLine(), Math.max(first.getLine(), last.getLine()));
    }

    private void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> ordered = new ArrayList<>(paths.toList());
            ordered.sort(Comparator.reverseOrder());
            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }
        }
    }
}
