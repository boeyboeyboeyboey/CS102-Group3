package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.QuestionDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds required question folders and source files that were submitted with the wrong letter case. */
final class CaseVariants {
    private final AppConfig config;

    CaseVariants(AppConfig config) {
        this.config = config;
    }

    /** A required entry found under a different case; paths are relative to the student root. */
    record Variant(Path actual, Path canonical, boolean directory) {
        String actualName() {
            return actual.getFileName().toString();
        }

        String canonicalName() {
            return canonical.getFileName().toString();
        }

        String kind() {
            return directory ? "question directory" : "source file";
        }
    }

    List<Variant> find(Path studentRoot) throws IOException {
        List<Variant> variants = new ArrayList<>();
        Map<String, String> directories = new LinkedHashMap<>();
        Set<String> expectedDirectories = new LinkedHashSet<>(config.getRequiredQuestionDirectories());
        for (QuestionDefinition question : config.getAssessment().getQuestions()) {
            expectedDirectories.add(question.getFolder());
        }
        for (String expected : expectedDirectories) {
            String actual = ExactEntries.resolveDirectory(studentRoot, expected);
            if (actual != null) {
                directories.put(expected, actual);
                if (!actual.equals(expected)) {
                    variants.add(new Variant(Path.of(actual), Path.of(expected), true));
                }
            }
        }
        Set<Path> seen = new LinkedHashSet<>();
        for (QuestionDefinition question : config.getAssessment().getQuestions()) {
            String directory = directories.get(question.getFolder());
            if (directory == null) {
                continue;
            }
            List<String> files = new ArrayList<>();
            files.add(question.getSourceFile());
            files.addAll(question.getCompanionSourceFiles());
            for (String expected : files) {
                String actual = ExactEntries.resolveFile(studentRoot.resolve(directory), expected);
                Path actualPath = Path.of(directory, actual == null ? expected : actual);
                if (actual != null && !actual.equals(expected) && seen.add(actualPath)) {
                    variants.add(new Variant(actualPath, Path.of(question.getFolder(), expected), false));
                }
            }
        }
        return variants;
    }
}
