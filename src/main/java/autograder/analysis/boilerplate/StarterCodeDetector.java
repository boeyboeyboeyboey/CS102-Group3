package autograder.analysis.boilerplate;

import autograder.api.BoilerplateDetector;
import autograder.config.BoilerplateConfig;
import autograder.model.AnalysisFinding;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Flags answer and companion files whose code tokens match, or nearly match, the starter files. */
public final class StarterCodeDetector implements BoilerplateDetector {
    private static final String UNAVAILABLE = "STARTER_COMPARISON_UNAVAILABLE";
    private static final String UNCHANGED = "UNCHANGED_STARTER";
    private static final String MINIMAL_CHANGES = "MINIMAL_STARTER_CHANGES";

    private final AssessmentDefinition assessment;
    private final BoilerplateConfig config;

    public StarterCodeDetector(AssessmentDefinition assessment, BoilerplateConfig config) {
        this.assessment = assessment;
        this.config = config;
    }

    @Override
    public List<AnalysisFinding> analyze(SubmissionResult submission, Path starterDirectory) {
        List<AnalysisFinding> findings = new ArrayList<>();
        Path root = submission.getWorkingRoot();
        if (root == null) {
            findings.add(new AnalysisFinding(UNAVAILABLE, submission.getArchivePath(),
                    "No working copy; starter comparison did not run"));
            return List.copyOf(findings);
        }
        for (Path relative : comparedFiles(assessment)) {
            compareFile(findings, submission, starterDirectory, relative);
        }
        return List.copyOf(findings);
    }

    // answer and companion files in assessment order, shared with the comparison report
    static Set<Path> comparedFiles(AssessmentDefinition assessment) {
        Set<Path> relativeFiles = new LinkedHashSet<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            relativeFiles.add(Path.of(question.getFolder(), question.getSourceFile()));
            for (String companion : question.getCompanionSourceFiles()) {
                relativeFiles.add(Path.of(question.getFolder(), companion));
            }
        }
        return relativeFiles;
    }

    private void compareFile(List<AnalysisFinding> findings, SubmissionResult submission,
                             Path starterDirectory, Path relative) {
        Path root = submission.getWorkingRoot();
        Path source = root.resolve(relative).toAbsolutePath().normalize();
        try {
            List<String> starter = read(starterDirectory, relative);
            List<String> answer = read(root, relative);
            if (starter.isEmpty()) {
                throw new IOException("starter has no comparable code tokens");
            }
            int edits = starter.equals(answer) ? 0 : JavaSourceTokens.editDistance(starter, answer);
            double similarity = 1.0 - (double) edits / Math.max(starter.size(), answer.size());
            if (edits == 0 || similarity >= config.similarityThreshold()) {
                findings.add(starterFinding(submission, source, edits, similarity,
                        starter.size(), answer.size()));
            }
        } catch (IOException | IllegalArgumentException exception) {
            findings.add(new AnalysisFinding(UNAVAILABLE, source,
                    submission.getArchivePath().getFileName() + ": " + relative + ": " + exception.getMessage()));
        }
    }

    private static AnalysisFinding starterFinding(SubmissionResult submission, Path source, int edits,
                                                  double similarity, int starterTokens, int answerTokens) {
        String code = edits == 0 ? UNCHANGED : MINIMAL_CHANGES;
        String description = edits == 0 ? "Code tokens match the starter after ignoring comments and formatting"
                : "High whole-file starter similarity; inspect the implementation before drawing conclusions";
        return new AnalysisFinding(code, source, String.format(Locale.ROOT,
                "%s: %s; %d token edits, %.2f%% similarity (%d starter / %d answer tokens). "
                        + "Review warning only; marks unchanged", submission.getArchivePath().getFileName(),
                description, edits, similarity * 100, starterTokens, answerTokens));
    }

    private List<String> read(Path directory, Path relative) throws IOException {
        return JavaSourceTokens.read(ComparisonSourceReader.read(directory, relative, config),
                config.maxTokens());
    }
}
