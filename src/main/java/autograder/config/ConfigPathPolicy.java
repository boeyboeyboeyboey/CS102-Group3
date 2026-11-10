package autograder.config;

import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Canonicalizes configured paths and rejects output locations that overlap inputs or protected files. */
final class ConfigPathPolicy {
    private ConfigPathPolicy() {
    }

    static Path canonicalize(Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath().normalize();
        Path existingPath = absolutePath;
        List<Path> missingNames = new ArrayList<>();
        while (existingPath != null && !Files.exists(existingPath, LinkOption.NOFOLLOW_LINKS)) {
            Path name = existingPath.getFileName();
            if (name != null) {
                missingNames.add(name);
            }
            existingPath = existingPath.getParent();
        }
        if (existingPath == null) {
            return absolutePath;
        }
        Path canonicalPath = existingPath.toRealPath();
        for (int index = missingNames.size() - 1; index >= 0; index--) {
            canonicalPath = canonicalPath.resolve(missingNames.get(index));
        }
        return canonicalPath.normalize();
    }

    static void validateOutputPaths(Path inputDirectory, Path extractionDirectory,
                                    Path workingDirectory, Path anomalyLog,
                                    Path correctionLog, Path scoreSheetPath,
                                    AssessmentDefinition assessment) {
        validateOutputDirectories(inputDirectory, extractionDirectory, workingDirectory);
        List<Path> protectedFiles = protectedInputFiles(scoreSheetPath, assessment);
        List<Path> outputDirectories = List.of(extractionDirectory, workingDirectory);
        validateOutputResources(outputDirectories, protectedFiles);
        validateLogPaths(inputDirectory, anomalyLog, correctionLog, outputDirectories, protectedFiles);
    }

    private static void validateOutputDirectories(Path inputDirectory, Path extractionDirectory,
                                                  Path workingDirectory) {
        requireDisjoint(extractionDirectory, workingDirectory,
                "extraction and working directories must be separate");
        requireDisjoint(extractionDirectory, inputDirectory,
                "extraction directory cannot overlap submission input");
        requireDisjoint(workingDirectory, inputDirectory,
                "working directory cannot overlap submission input");
    }

    private static List<Path> protectedInputFiles(Path scoreSheetPath,
                                                  AssessmentDefinition assessment) {
        List<Path> protectedFiles = new ArrayList<>();
        protectedFiles.add(scoreSheetPath);
        for (QuestionDefinition question : assessment.getQuestions()) {
            protectedFiles.add(question.getTesterFile());
            for (Path path : question.getSupportFiles()) {
                protectedFiles.add(path);
            }
            for (Path path : question.getFixtures()) {
                protectedFiles.add(path);
            }
        }
        return protectedFiles;
    }

    private static void validateOutputResources(List<Path> outputDirectories,
                                                List<Path> protectedFiles) {
        for (Path outputDirectory : outputDirectories) {
            for (Path protectedFile : protectedFiles) {
                if (protectedFile.startsWith(outputDirectory)
                        || outputDirectory.equals(protectedFile)
                        || outputDirectory.startsWith(protectedFile)) {
                    throw new IllegalArgumentException("output directory overlaps an input resource: "
                            + protectedFile);
                }
            }
        }
    }

    private static void validateLogPaths(Path inputDirectory, Path anomalyLog, Path correctionLog,
                                         List<Path> outputDirectories, List<Path> protectedFiles) {
        List<Path> logPaths = List.of(anomalyLog, correctionLog);
        requireDisjoint(anomalyLog, correctionLog,
                "anomaly and correction logs must be separate paths");
        for (Path logPath : logPaths) {
            validateLogPath(logPath, inputDirectory, outputDirectories, protectedFiles);
        }
    }

    private static void validateLogPath(Path logPath, Path inputDirectory,
                                        List<Path> outputDirectories,
                                        List<Path> protectedFiles) {
        if (logPath.startsWith(inputDirectory) || inputDirectory.startsWith(logPath)) {
            throw new IllegalArgumentException("log path cannot overlap the submission directory");
        }
        for (Path outputDirectory : outputDirectories) {
            if (logPath.startsWith(outputDirectory) || outputDirectory.startsWith(logPath)) {
                throw new IllegalArgumentException("log path overlaps an output directory");
            }
        }
        for (Path protectedFile : protectedFiles) {
            if (logPath.equals(protectedFile) || logPath.startsWith(protectedFile)
                    || protectedFile.startsWith(logPath)) {
                throw new IllegalArgumentException("log path overlaps an input resource: "
                        + protectedFile);
            }
        }
    }

    static void requireDisjoint(Path first, Path second, String message) {
        if (first.startsWith(second) || second.startsWith(first)) {
            throw new IllegalArgumentException(message + ": " + first + " and " + second);
        }
    }

    static List<Path> pathList(Path baseDirectory, String value) {
        List<Path> paths = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return paths;
        }
        for (String item : value.split(",", -1)) {
            String path = item.trim();
            if (!path.isEmpty()) {
                paths.add(resolvePath(baseDirectory, path));
            }
        }
        return paths;
    }

    static Path resolvePath(Path baseDirectory, String value) {
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return baseDirectory.resolve(path).normalize();
    }
}
