package autograder.autotester;

import autograder.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomizedBatchGraderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void gradesMultipleArchivesWithOneSeedAndContinuesAfterInvalidArchive() throws Exception {
        Path input = Files.createDirectory(temporaryDirectory.resolve("submissions"));
        createZip(input.resolve("alice.zip"), Map.of("alice/Q1.java", correctAnswer()));
        createZip(input.resolve("bob.zip"), Map.of("nested/bob/Q1.java", incorrectAnswer()));
        Files.writeString(input.resolve("broken.zip"), "not a zip archive");
        Files.writeString(input.resolve("notes.txt"), "ignored");
        RandomizedConfig config = config();
        AppConfig appConfig = AppConfig.load(Path.of("config.properties"));

        RandomizedBatchGrader.BatchResult result = new RandomizedBatchGrader().grade(
                QuestionRegistry.find("Q1"), input, appConfig, config, 719L);

        assertEquals(3, result.submissions().size());
        assertEquals(1, result.passedCount());
        assertEquals(2, result.failedCount());
        assertTrue(result.submissions().get(0).result().passed());
        assertFalse(result.submissions().get(1).result().passed());
        assertFalse(result.submissions().get(2).result().errorReason().isBlank());
        assertTrue(Files.readString(result.summaryFile()).contains("broken.zip"));
        assertTrue(Files.readString(result.summaryFile()).contains("719"));
        try (var reports = Files.list(result.outputDirectory())) {
            assertEquals(3, reports.filter(path -> path.getFileName().toString().endsWith(".txt")).count());
        }
    }

    @Test
    void reportsMissingAndAmbiguousSourcesWithoutStoppingTheBatch() throws Exception {
        Path input = Files.createDirectory(temporaryDirectory.resolve("ambiguous-submissions"));
        createZip(input.resolve("ambiguous.zip"), Map.of(
                "one/Q1.java", correctAnswer(), "two/Q1.java", correctAnswer()));
        createZip(input.resolve("missing.zip"), Map.of("answer.java", correctAnswer()));

        RandomizedBatchGrader.BatchResult result = new RandomizedBatchGrader().grade(
                QuestionRegistry.find("Q1"), input, AppConfig.load(Path.of("config.properties")), config(), 9L);

        assertEquals(2, result.failedCount());
        assertTrue(result.submissions().stream().anyMatch(submission ->
                submission.result().errorReason().contains("ambiguous")));
        assertTrue(result.submissions().stream().anyMatch(submission ->
                submission.result().errorReason().contains("could not find Q1.java")));
    }

    private RandomizedConfig config() {
        return new RandomizedConfig(temporaryDirectory.resolve("resources"),
                temporaryDirectory.resolve("results"), "java", "javac", 2,
                10_000, 3, 1, 1_048_576);
    }

    private void createZip(Path archive, Map<String, String> entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : new LinkedHashMap<>(entries).entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private String correctAnswer() {
        return "public class Q1 { public boolean isEven(int n) { return n % 2 == 0; } }";
    }

    private String incorrectAnswer() {
        return "public class Q1 { public boolean isEven(int n) { return true; } }";
    }
}
