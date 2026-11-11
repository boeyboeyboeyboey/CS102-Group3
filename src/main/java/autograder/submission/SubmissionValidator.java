package autograder.submission;

import autograder.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Selects the student root inside an extracted archive and checks its required question directories and their case. */
final class SubmissionValidator {
    private final AppConfig config;
    private final CaseVariants caseVariants;

    SubmissionValidator(AppConfig config) {
        this.config = config;
        this.caseVariants = new CaseVariants(config);
    }

    RootSelection selectStudentRoot(Path extractionPath) throws IOException {
        List<Path> candidates = new ArrayList<>();
        List<Path> wrongCaseCandidates = new ArrayList<>();
        try (var paths = Files.walk(extractionPath)) {
            for (Path path : paths.toList()) {
                if (!Files.isDirectory(path) || containsMetadataDirectory(extractionPath, path)) {
                    continue;
                }
                if (containsRequiredQuestionDirectory(path, true)) {
                    candidates.add(path);
                } else if (containsRequiredQuestionDirectory(path, false)) {
                    wrongCaseCandidates.add(path);
                }
            }
        }
        // a folder with exact names always wins; wrong-case folders only matter when none has them
        if (candidates.isEmpty()) {
            candidates = wrongCaseCandidates;
        }
        if (candidates.isEmpty()) {
            return new RootSelection(null,
                    List.of("no directory contains a configured required question folder"));
        }
        if (candidates.size() > 1) {
            return new RootSelection(null,
                    List.of("multiple plausible submission roots found; root selection is ambiguous"));
        }
        return new RootSelection(candidates.get(0), List.of());
    }

    List<String> findQuestionDirectories(Path studentRoot) throws IOException {
        List<String> questionDirectories = new ArrayList<>();
        for (String questionDirectory : config.getRequiredQuestionDirectories()) {
            if (ExactEntries.resolveDirectory(studentRoot, questionDirectory) != null) {
                questionDirectories.add(questionDirectory);
            }
        }
        return questionDirectories;
    }

    List<String> findMissingQuestionDirectories(Path studentRoot) throws IOException {
        List<String> missingDirectories = new ArrayList<>();
        for (String questionDirectory : config.getRequiredQuestionDirectories()) {
            if (ExactEntries.resolveDirectory(studentRoot, questionDirectory) == null) {
                missingDirectories.add("required question directory is missing: " + questionDirectory);
            }
        }
        return missingDirectories;
    }

    List<String> findWrongCaseQuestionDirectories(Path studentRoot) throws IOException {
        List<String> warnings = new ArrayList<>();
        for (CaseVariants.Variant variant : caseVariants.find(studentRoot)) {
            if (variant.directory()) {
                warnings.add("required question directory has wrong case: " + variant.actualName()
                        + " (expected " + variant.canonicalName() + ")");
            }
        }
        return warnings;
    }

    private boolean containsMetadataDirectory(Path extractionPath, Path path) {
        Path relativePath = extractionPath.relativize(path);
        for (Path segment : relativePath) {
            if (config.getMetadataDirectoryPattern().matcher(segment.toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRequiredQuestionDirectory(Path path, boolean exactOnly) throws IOException {
        for (String questionDirectory : config.getRequiredQuestionDirectories()) {
            boolean found = exactOnly
                    ? ExactEntries.hasDirectory(path, questionDirectory)
                    : ExactEntries.resolveDirectory(path, questionDirectory) != null;
            if (found) {
                return true;
            }
        }
        return false;
    }

    static final class RootSelection {
        private final Path root;
        private final List<String> anomalies;

        private RootSelection(Path root, List<String> anomalies) {
            this.root = root;
            this.anomalies = anomalies;
        }

        Path getRoot() {
            return root;
        }

        List<String> getAnomalies() {
            return anomalies;
        }
    }
}
