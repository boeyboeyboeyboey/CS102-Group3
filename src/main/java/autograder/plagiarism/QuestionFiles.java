package autograder.plagiarism;

import autograder.model.QuestionDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// locates the answer and companion files for one question inside one submission or starter folder
final class QuestionFiles {
    private QuestionFiles() {
    }

    // empty when the main answer file is missing so absent work is never compared
    static List<Path> locate(Path folderRoot, QuestionDefinition question) {
        Path root = folderRoot.toAbsolutePath().normalize();
        Path directory = root.resolve(question.getFolder()).normalize();
        Path answer = existingFile(root, directory, question.getSourceFile());
        if (answer == null) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        files.add(answer);
        for (String companion : question.getCompanionSourceFiles()) {
            Path companionFile = existingFile(root, directory, companion);
            if (companionFile != null) {
                files.add(companionFile);
            }
        }
        return files;
    }

    private static Path existingFile(Path root, Path directory, String fileName) {
        Path file = directory.resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return null;
        }
        return file;
    }
}
