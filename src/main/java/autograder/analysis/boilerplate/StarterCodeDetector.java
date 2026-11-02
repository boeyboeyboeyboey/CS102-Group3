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

public final class StarterCodeDetector implements BoilerplateDetector {
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
            findings.add(new AnalysisFinding("STARTER_COMPARISON_UNAVAILABLE", submission.getArchivePath(),
                    "No working copy; starter comparison did not run"));
            return List.copyOf(findings);
        }
        Set<Path> relativeFiles = new LinkedHashSet<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            relativeFiles.add(Path.of(question.getFolder(), question.getSourceFile()));
            for (String companion : question.getCompanionSourceFiles()) {
                relativeFiles.add(Path.of(question.getFolder(), companion));
            }
        }
        for (Path relative : relativeFiles) {
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
                    String code = edits == 0 ? "UNCHANGED_STARTER" : "MINIMAL_STARTER_CHANGES";
                    String description = edits == 0 ? "Code tokens match the starter after ignoring comments and formatting"
                            : "High whole-file starter similarity; inspect the implementation before drawing conclusions";
                    findings.add(new AnalysisFinding(code, source, String.format(Locale.ROOT,
                            "%s: %s; %d token edits, %.2f%% similarity (%d starter / %d answer tokens). "
                                    + "Review warning only; marks unchanged", submission.getArchivePath().getFileName(),
                            description, edits, similarity * 100, starter.size(), answer.size())));
                }
            } catch (IOException | IllegalArgumentException exception) {
                findings.add(new AnalysisFinding("STARTER_COMPARISON_UNAVAILABLE", source,
                        submission.getArchivePath().getFileName() + ": " + relative + ": " + exception.getMessage()));
            }
        }
        return List.copyOf(findings);
    }

    private List<String> read(Path directory, Path relative) throws IOException {
        return JavaSourceTokens.read(ComparisonSourceReader.read(directory, relative, config),
                config.maxTokens());
    }
}
