package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionProcessorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void cancellationStopsIntakeBeforeProcessingTheNextArchive() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        createArchive(submissions.resolve("boey.tan.2026.zip"), validSubmission("boey"));
        AppConfig config = createConfig(temporaryDirectory);
        AtomicInteger updates = new AtomicInteger();
        SubmissionProcessor processor = new SubmissionProcessor(config,
                (stage, item, completed, total) -> {
                    updates.incrementAndGet();
                    Thread.currentThread().interrupt();
                });

        try {
            assertThrows(CancellationException.class, processor::processBatch);
            assertEquals(1, updates.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void rejectsTraversalAndContinuesAfterCorruptArchive() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        createArchive(submissions.resolve("unsafe.zip"), mapOf(
                "Q1/Q1a.java", source("boey.tan.2026", "boey.tan.2026@school.edu"),
                "../escape.txt", "outside"));
        Files.writeString(submissions.resolve("broken.zip"), "not a zip archive");
        createArchive(submissions.resolve("boey.tan.2026.zip"), validSubmission("boey"));
        AppConfig config = createConfig(temporaryDirectory);

        List<SubmissionResult> results = new SubmissionProcessor(config).processBatch();

        assertEquals(3, results.size());
        assertEquals(SubmissionStatus.FAILED, resultNamed(results, "unsafe.zip").getStatus());
        assertEquals(SubmissionStatus.FAILED, resultNamed(results, "broken.zip").getStatus());
        assertEquals(SubmissionStatus.READY, resultNamed(results, "boey.tan.2026.zip").getStatus());
        assertFalse(Files.exists(config.getExtractionDirectory().resolve("escape.txt")));
        assertTrue(Files.readString(config.getAnomalyLog()).contains("broken.zip"));
        assertTrue(Files.readString(config.getAnomalyLog()).contains("traversal"));
    }

    @Test
    void repairsOnlyWorkingCopyAndRecordsWrapperLayoutCorrection() throws Exception {
        Path archive = archivePath("boey.tan.2026.zip");
        String q1 = source("boey.tan.2026", "boey.tan.2026@school.edu");
        String q2 = "/*\n * Name:\n * Email ID: boey.tan.2026@school.edu\n */\nclass Q2a {}\n";
        createArchive(archive, mapOf(
                "RenameToYourUsername/Q1/Q1a.java", q1,
                "RenameToYourUsername/Q2/Q2a.java", q2));
        AppConfig config = createConfig(temporaryDirectory);
        byte[] originalArchive = Files.readAllBytes(archive);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertEquals(SubmissionStatus.READY, result.getStatus());
        assertEquals("boey.tan.2026", result.getIdentity().getStudentRecord().getUsername()
                .replaceFirst("^#", ""));
        assertTrue(result.getCorrections().stream().anyMatch(value -> value.getType().equals("layout")));
        assertEquals(q1, Files.readString(result.getWorkingRoot().resolve("Q1/Q1a.java")));
        assertTrue(Files.readString(result.getWorkingRoot().resolve("Q2/Q2a.java"))
                .contains("Name: Boey Tan"));
        assertEquals(q1, Files.readString(result.getStudentRoot().resolve("Q1/Q1a.java")));
        assertArrayEquals(originalArchive, Files.readAllBytes(archive));
        assertTrue(Files.readString(config.getCorrectionLog()).contains("wrapper root"));
        assertTrue(result.getStudentRoot().toString().contains("RenameToYourUsername"));
    }

    @Test
    void missingStructureAndIdentityStayReviewRequiredWithoutCorrections() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        createArchive(submissions.resolve("unknown.zip"), mapOf(
                "odd-wrapper/q1/Q1a.java", "class Q1a {}\n"));
        AppConfig config = createConfig(temporaryDirectory);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertEquals(SubmissionStatus.REVIEW_REQUIRED, result.getStatus());
        assertNull(result.getIdentity().getStudentRecord());
        assertNull(result.getWorkingRoot());
        assertTrue(result.getCorrections().isEmpty());
        assertTrue(result.getAnomalies().stream().anyMatch(value -> value.contains("root")));
        assertTrue(result.getIdentity().getName().contains("archive-derived fallback"));
    }

    @Test
    void strongIdentityConflictBlocksWorkingCopyCorrections() throws Exception {
        Path archive = archivePath("boey.tan.2026.zip");
        createArchive(archive, validSubmission("sera"));
        AppConfig config = createConfig(temporaryDirectory);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertEquals(SubmissionStatus.REVIEW_REQUIRED, result.getStatus());
        assertNull(result.getIdentity().getStudentRecord());
        assertNull(result.getWorkingRoot());
        assertTrue(result.getCorrections().isEmpty());
        assertTrue(result.getAnomalies().stream().anyMatch(value -> value.contains("conflicting strong")));
    }

    @Test
    void duplicateSubmissionsAreAllMarkedForReviewAndWrongCaseSourceIsMissing() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        createArchive(submissions.resolve("boey.tan.2026.zip"), validSubmission("boey"));
        Files.writeString(submissions.resolve("2023-2024-boey.tan.2026.zip"), "corrupt archive");
        createArchive(submissions.resolve("sera.lim.2026.zip"), mapOf(
                "Q1/Q1A.java", source("sera.lim.2026", "sera.lim.2026@school.edu"),
                "Q2/Q2a.java", source("sera.lim.2026", "sera.lim.2026@school.edu")));
        AppConfig config = createConfig(temporaryDirectory);

        List<SubmissionResult> results = new SubmissionProcessor(config).processBatch();

        assertEquals(3, results.size());
        assertEquals(SubmissionStatus.FAILED,
                resultNamed(results, "2023-2024-boey.tan.2026.zip").getStatus());
        assertEquals(SubmissionStatus.REVIEW_REQUIRED,
                resultNamed(results, "boey.tan.2026.zip").getStatus());
        assertTrue(resultNamed(results, "2023-2024-boey.tan.2026.zip").getAnomalies().stream()
                .anyMatch(value -> value.contains("duplicate")));
        assertTrue(resultNamed(results, "boey.tan.2026.zip").getAnomalies().stream()
                .anyMatch(value -> value.contains("duplicate")));
        assertEquals(SubmissionStatus.REVIEW_REQUIRED,
                resultNamed(results, "sera.lim.2026.zip").getStatus());
        assertTrue(resultNamed(results, "sera.lim.2026.zip").getAnomalies().stream()
                .anyMatch(value -> value.contains("Q1a.java")));
    }

    @Test
    void missingQuestionDirectoryDoesNotPreventPreparationOfPresentQuestion() throws Exception {
        Path archive = archivePath("boey.tan.2026.zip");
        createArchive(archive, mapOf("Q1/Q1a.java",
                source("boey.tan.2026", "boey.tan.2026@school.edu")));
        AppConfig config = createConfig(temporaryDirectory);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertEquals(SubmissionStatus.REVIEW_REQUIRED, result.getStatus());
        assertEquals(List.of("Q1"), result.getQuestionDirectories());
        assertTrue(result.getWorkingRoot() != null);
        assertTrue(result.getAnomalies().stream()
                .anyMatch(value -> value.contains("required question directory is missing: Q2")));
    }

    @Test
    void repeatedRunsKeepEarlierExtractionAndSecondAssessmentProfileLoads() throws Exception {
        Path archive = archivePath("boey.tan.2026.zip");
        createArchive(archive, validSubmission("boey"));
        AppConfig config = createConfig(temporaryDirectory);
        SubmissionProcessor processor = new SubmissionProcessor(config);
        SubmissionResult first = processor.processBatch().get(0);
        SubmissionResult second = processor.processBatch().get(0);

        assertFalse(first.getExtractionPath().equals(second.getExtractionPath()));
        assertTrue(Files.exists(first.getStudentRoot()));
        assertTrue(Files.exists(second.getStudentRoot()));

        Path secondProfileRoot = Files.createDirectories(temporaryDirectory.resolve("profile-two"));
        AppConfig secondProfile = createConfig(secondProfileRoot, "Q3", 3, 0.1, "0.3");
        assertEquals(0.3, secondProfile.getAssessment().getMaxScore(), 0.000000001);
        assertEquals("Q3", secondProfile.getAssessment().getQuestions().get(0).getId());
    }

    @Test
    void extractorRemovesPartialTreeAfterPathTraversal() throws Exception {
        Path archive = temporaryDirectory.resolve("unsafe-direct.zip");
        createArchive(archive, mapOf("safe/file.txt", "data", "../escape.txt", "outside"));
        AppConfig config = createConfig(temporaryDirectory);
        Path destination = config.getExtractionDirectory().resolve("direct-extraction");

        IOException exception = assertThrows(IOException.class,
                () -> new SafeZipExtractor(config).extract(archive, destination));

        assertTrue(exception.getMessage().contains("traversal"));
        assertFalse(Files.exists(destination));
    }

    @Test
    void enforcesArchiveEntryAndUncompressedSizeLimits() throws Exception {
        Path countRoot = Files.createDirectories(temporaryDirectory.resolve("count-limit"));
        Path countSubmissions = Files.createDirectories(countRoot.resolve("submissions"));
        createArchive(countSubmissions.resolve("too-many.zip"), mapOf(
                "one.txt", "one", "two.txt", "two"));
        AppConfig countConfig = createConfig(countRoot, "Q1a,Q2a", 1, 1,
                "2", 1, 5, 6);
        SubmissionResult countResult = new SubmissionProcessor(countConfig).processBatch().get(0);
        assertEquals(SubmissionStatus.FAILED, countResult.getStatus());
        assertTrue(countResult.getAnomalies().get(0).contains("entry count"));

        Path sizeRoot = Files.createDirectories(temporaryDirectory.resolve("size-limits"));
        Path sizeSubmissions = Files.createDirectories(sizeRoot.resolve("submissions"));
        createArchive(sizeSubmissions.resolve("entry-too-large.zip"), mapOf(
                "large.txt", "123456"));
        createArchive(sizeSubmissions.resolve("archive-too-large.zip"), mapOf(
                "one.txt", "1234", "two.txt", "5678"));
        AppConfig sizeConfig = createConfig(sizeRoot, "Q1a,Q2a", 1, 1,
                "2", 2, 5, 6);

        List<SubmissionResult> results = new SubmissionProcessor(sizeConfig).processBatch();

        assertEquals(2, results.size());
        SubmissionResult entryResult = resultNamed(results, "entry-too-large.zip");
        SubmissionResult totalResult = resultNamed(results, "archive-too-large.zip");
        assertEquals(SubmissionStatus.FAILED, entryResult.getStatus());
        assertEquals(SubmissionStatus.FAILED, totalResult.getStatus());
        assertFalse(Files.exists(entryResult.getExtractionPath()));
        assertFalse(Files.exists(totalResult.getExtractionPath()));
        assertTrue(entryResult.getAnomalies().get(0).contains("entry exceeds configured size"));
        assertTrue(totalResult.getAnomalies().get(0).contains("uncompressed size limit"));
    }

    @Test
    void detectsStoredEntryChecksumMismatch() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        Path archive = submissions.resolve("crc-damaged.zip");
        createStoredArchive(archive, "payload.txt", "CRC_TARGET_PAYLOAD");
        corruptStoredPayload(archive, "CRC_TARGET_PAYLOAD");
        AppConfig config = createConfig(temporaryDirectory);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertEquals(SubmissionStatus.FAILED, result.getStatus());
        assertTrue(result.getAnomalies().get(0).contains("checksum"));
        assertFalse(Files.exists(result.getExtractionPath()));
    }

    @Test
    void reportsAmbiguousRootsAndIgnoresMetadataDirectories() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        createArchive(submissions.resolve("ambiguous.zip"), mapOf(
                "first/Q1/Q1a.java", "class Q1a {}\n",
                "second/Q1/Q1a.java", "class Q1a {}\n",
                "__MACOSX/Q1/Q1a.java", "class Metadata {}\n"));
        AppConfig config = createConfig(temporaryDirectory);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertNull(result.getStudentRoot());
        assertEquals(SubmissionStatus.REVIEW_REQUIRED, result.getStatus());
        assertTrue(result.getAnomalies().stream().anyMatch(value -> value.contains("ambiguous")));

        Path validRoot = Files.createDirectories(temporaryDirectory.resolve("metadata-valid"));
        Path validSubmissions = Files.createDirectories(validRoot.resolve("submissions"));
        createArchive(validSubmissions.resolve("boey.tan.2026.zip"), mapOf(
                "student/Q1/Q1a.java", source("boey.tan.2026", "boey.tan.2026@school.edu"),
                "student/Q2/Q2a.java", source("boey.tan.2026", "boey.tan.2026@school.edu"),
                "__MACOSX/Q1/Q1a.java", "class Metadata {}\n"));
        AppConfig validConfig = createConfig(validRoot);
        SubmissionResult validResult = new SubmissionProcessor(validConfig).processBatch().get(0);
        assertEquals(SubmissionStatus.READY, validResult.getStatus());
        assertTrue(validResult.getStudentRoot().endsWith("student"));
    }

    @Test
    void progressListenerFailureDoesNotStopBatch() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        createArchive(submissions.resolve("boey.tan.2026.zip"), validSubmission("boey"));
        createArchive(submissions.resolve("sera.lim.2026.zip"), validSubmission("sera"));
        AppConfig config = createConfig(temporaryDirectory);
        SubmissionProcessor processor = new SubmissionProcessor(config,
                (stage, item, completed, total) -> {
                    throw new IllegalStateException("display failure");
                });

        List<SubmissionResult> results = processor.processBatch();

        assertEquals(2, results.size());
        assertEquals(SubmissionStatus.READY, results.get(0).getStatus());
        assertEquals(SubmissionStatus.READY, results.get(1).getStatus());
    }

    @Test
    void readsLineCommentIdentityAndRejectsOutputPrefixTraversal() throws Exception {
        Path archive = archivePath("boey.tan.2026.zip");
        String source = "// Name: Boey Tan\n// Email ID: boey.tan.2026@school.edu\n"
                + "class Submission {}\n";
        createArchive(archive, mapOf("Q1/Q1a.java", source, "Q2/Q2a.java", source));
        AppConfig config = createConfig(temporaryDirectory);

        SubmissionResult result = new SubmissionProcessor(config).processBatch().get(0);

        assertEquals(SubmissionStatus.READY, result.getStatus());
        assertTrue(result.getIdentity().getHeaderNames().contains("Boey Tan"));

        Path configPath = temporaryDirectory.resolve("config.properties");
        Properties properties = new Properties();
        try (var input = Files.newInputStream(configPath)) {
            properties.load(input);
        }
        properties.setProperty("run.directory.prefix", "../outside-");
        try (var output = Files.newOutputStream(configPath)) {
            properties.store(output, "invalid prefix configuration");
        }
        assertThrows(IllegalArgumentException.class, () -> AppConfig.load(configPath));
    }

    private AppConfig createConfig(Path root) throws IOException {
        return createConfig(root, "Q1a,Q2a", 1, 1, "2", 100, 10000, 20000);
    }

    private AppConfig createConfig(Path root, String questionIds, int caseCount,
                                   double pointsPerCase, String denominator) throws IOException {
        return createConfig(root, questionIds, caseCount, pointsPerCase, denominator,
                100, 10000, 20000);
    }

    private AppConfig createConfig(Path root, String questionIds, int caseCount,
                                   double pointsPerCase, String denominator,
                                   int maxEntries, long maxEntryBytes, long maxArchiveBytes)
            throws IOException {
        Path input = Files.createDirectories(root.resolve("submissions"));
        Path scoreSheet = root.resolve("roster.csv");
        Files.writeString(scoreSheet, scoreSheet(denominator), StandardCharsets.UTF_8);
        Path configPath = root.resolve("config.properties");
        Properties properties = new Properties();
        properties.setProperty("input.directory", input.toString());
        properties.setProperty("extraction.directory", root.resolve("extracted").toString());
        properties.setProperty("working.directory", root.resolve("working").toString());
        properties.setProperty("run.directory.prefix", "extract-run-");
        properties.setProperty("working.run.directory.prefix", "working-run-");
        properties.setProperty("working.submission.directory.prefix", "submission-");
        properties.setProperty("anomaly.log", root.resolve("logs/anomalies.log").toString());
        properties.setProperty("correction.log", root.resolve("logs/corrections.log").toString());
        properties.setProperty("archive.file.regex", "(?i).*\\.zip");
        properties.setProperty("java.source.regex", "(?i).*\\.java");
        properties.setProperty("question.directory.regex", "Q[0-9]+");
        List<String> requiredFolders = new java.util.ArrayList<>();
        for (String questionId : questionIds.split(",")) {
            String folder = questionId.replaceFirst("[a-zA-Z]+$", "");
            if (!requiredFolders.contains(folder)) {
                requiredFolders.add(folder);
            }
        }
        properties.setProperty("question.directories.required", String.join(",", requiredFolders));
        properties.setProperty("metadata.directory.regex", "^__MACOSX$");
        properties.setProperty("identity.name.regex", "(?im)^\\s*(?:\\*|//)?\\s*Name\\s*:\\s*(.*?)\\s*$");
        properties.setProperty("identity.email.regex", "(?im)^\\s*(?:\\*|//)?\\s*Email ID\\s*:\\s*(.*?)\\s*$");
        properties.setProperty("identity.username.regex", "(?im)^\\s*(?:\\*|//)?\\s*Username\\s*:\\s*(.*?)\\s*$");
        properties.setProperty("identity.name.label", "Name");
        properties.setProperty("identity.email.label", "Email ID");
        properties.setProperty("identity.username.label", "Username");
        properties.setProperty("identity.archive.username.regex",
                "^(?:2023-2024-)?([a-z]+(?:\\.[a-z]+)*\\.\\d{4})\\.zip$");
        properties.setProperty("identity.wrapper.username.regex", "^[a-z]+(?:\\.[a-z]+)*\\.\\d{4}$");
        properties.setProperty("identity.wrapper.student.id.regex", "^\\d+$");
        properties.setProperty("identity.wrapper.placeholder.regex",
                "^(?:RenameToYourStudentID|RenameToYourUsername)$");
        properties.setProperty("identity.roster.prefix", "#");
        properties.setProperty("identity.roster.last-name.placeholder", "_");
        properties.setProperty("identity.header.max.lines", "12");
        properties.setProperty("io.buffer.bytes", "32");
        properties.setProperty("limits.archive.entries", String.valueOf(maxEntries));
        properties.setProperty("limits.entry.bytes", String.valueOf(maxEntryBytes));
        properties.setProperty("limits.archive.bytes", String.valueOf(maxArchiveBytes));
        properties.setProperty("score.sheet.path", scoreSheet.toString());
        properties.setProperty("score.sheet.columns.id", "id");
        properties.setProperty("score.sheet.columns.username", "username");
        properties.setProperty("score.sheet.columns.lastName", "lastName");
        properties.setProperty("score.sheet.columns.firstName", "firstName");
        properties.setProperty("score.sheet.columns.email", "email");
        properties.setProperty("score.sheet.columns.numerator", "numerator");
        properties.setProperty("score.sheet.columns.denominator", "denominator");
        properties.setProperty("score.sheet.columns.end", "end");
        properties.setProperty("assessment.id", "synthetic-assessment");
        properties.setProperty("assessment.name", "Synthetic assessment");
        properties.setProperty("assessment.question.ids", questionIds);
        for (String questionId : questionIds.split(",")) {
            String folder = questionId.replaceFirst("[a-zA-Z]+$", "");
            String prefix = "assessment.question." + questionId + ".";
            properties.setProperty(prefix + "folder", folder);
            properties.setProperty(prefix + "source.file", questionId + ".java");
            properties.setProperty(prefix + "tester.file", root.resolve("testers/" + questionId + "Tester.java").toString());
            properties.setProperty(prefix + "case.count", String.valueOf(caseCount));
            properties.setProperty(prefix + "points.per.case", String.valueOf(pointsPerCase));
            properties.setProperty(prefix + "support.files", "");
            properties.setProperty(prefix + "fixtures", "");
        }
        try (var output = Files.newOutputStream(configPath)) {
            properties.store(output, "test configuration");
        }
        return AppConfig.load(configPath);
    }

    private String scoreSheet(String denominator) {
        return "id,username,lastName,firstName,email,numerator,denominator,end\n"
                + "001,#boey.tan.2026,Tan,Boey,boey.tan.2026@school.edu,0," + denominator + ",1\n"
                + "002,#sera.lim.2026,Lim,Sera,sera.lim.2026@school.edu,0," + denominator + ",1\n";
    }

    private SubmissionResult resultNamed(List<SubmissionResult> results, String archiveName) {
        for (SubmissionResult result : results) {
            if (result.getArchivePath().getFileName().toString().equals(archiveName)) {
                return result;
            }
        }
        throw new AssertionError("submission result was not found: " + archiveName);
    }

    private Path archivePath(String name) throws IOException {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("submissions"));
        return submissions.resolve(name);
    }

    private LinkedHashMap<String, String> validSubmission(String user) {
        String username = user.equals("boey") ? "boey.tan.2026" : "sera.lim.2026";
        String email = username + "@school.edu";
        return mapOf("Q1/Q1a.java", source(username, email),
                "Q2/Q2a.java", source(username, email));
    }

    private String source(String username, String email) {
        String firstName = username.startsWith("boey") ? "Boey" : "Sera";
        String lastName = username.startsWith("boey") ? "Tan" : "Lim";
        return "/*\n * Name: " + firstName + " " + lastName
                + "\n * Email ID: " + email + "\n */\nclass Submission {}\n";
    }

    private LinkedHashMap<String, String> mapOf(String... values) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            entries.put(values[index], values[index + 1]);
        }
        return entries;
    }

    private void createArchive(Path archive, LinkedHashMap<String, String> entries)
            throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (var entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private void createStoredArchive(Path archive, String entryName, String value)
            throws IOException {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        CRC32 checksum = new CRC32();
        checksum.update(data);
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        entry.setCrc(checksum.getValue());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(entry);
            output.write(data);
            output.closeEntry();
        }
    }

    private void corruptStoredPayload(Path archive, String value) throws IOException {
        byte[] archiveBytes = Files.readAllBytes(archive);
        byte[] payload = value.getBytes(StandardCharsets.UTF_8);
        for (int index = 0; index <= archiveBytes.length - payload.length; index++) {
            boolean matches = true;
            for (int payloadIndex = 0; payloadIndex < payload.length; payloadIndex++) {
                if (archiveBytes[index + payloadIndex] != payload[payloadIndex]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                archiveBytes[index] = (byte) (archiveBytes[index] + 1);
                Files.write(archive, archiveBytes);
                return;
            }
        }
        throw new IOException("stored payload was not found in archive");
    }
}
