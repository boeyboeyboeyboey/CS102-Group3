package autograder.autotester;

import autograder.config.AppConfig;
import autograder.submission.SafeZipExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class RandomizedBatchGrader {
    private final RandomizedGrader grader;

    public RandomizedBatchGrader() {
        this(new RandomizedGrader());
    }

    RandomizedBatchGrader(RandomizedGrader grader) {
        this.grader = grader;
    }

    public BatchResult grade(Question question, Path zipDirectory, AppConfig appConfig,
                             RandomizedConfig config, long seed)
            throws IOException, InterruptedException {
        question.validate();
        Path input = zipDirectory.toAbsolutePath().normalize();
        Path results = config.resultsDirectory().toAbsolutePath().normalize();
        if (!Files.isDirectory(input)) {
            throw new IOException("ZIP submission directory does not exist: " + input);
        }
        if (overlaps(input, results)) {
            throw new IOException("randomized results directory must not overlap the ZIP submission directory");
        }

        List<Path> archives = findArchives(input, appConfig);
        if (archives.isEmpty()) {
            throw new IOException("no ZIP submissions matched in " + input);
        }
        Files.createDirectories(results);
        Path outputDirectory = Files.createTempDirectory(results,
                "randomized-batch-" + question.getClassName() + "-" + seed + "-");
        Path temporaryDirectory = Files.createTempDirectory("randomized-batch-work-");
        List<SubmissionResult> submissions = new ArrayList<>();
        try {
            for (int index = 0; index < archives.size(); index++) {
                submissions.add(gradeArchive(question, archives.get(index), index, temporaryDirectory,
                        outputDirectory, new SafeZipExtractor(appConfig), config, seed));
            }
        } finally {
            deleteTree(temporaryDirectory);
        }

        Path summary = outputDirectory.resolve("summary.csv");
        writeSummary(summary, submissions);
        return new BatchResult(outputDirectory, summary, submissions);
    }

    private SubmissionResult gradeArchive(Question question, Path archive, int index,
                                          Path temporaryDirectory, Path outputDirectory,
                                          SafeZipExtractor extractor, RandomizedConfig config, long seed)
            throws IOException, InterruptedException {
        String label = String.format(Locale.ROOT, "%04d-%s", index + 1,
                safeName(archive.getFileName().toString().replaceFirst("(?i)\\.zip$", "")));
        Path studentDirectory = temporaryDirectory.resolve(label);
        RandomizedConfig batchConfig = configForBatch(config, outputDirectory);
        RandomizedResult result;
        try {
            Path extracted = temporaryDirectory.resolve("extract-" + (index + 1));
            extractor.extract(archive, extracted);
            Path source = findStudentSource(extracted, question);
            stageSources(question, source, studentDirectory);
            result = grader.grade(question, studentDirectory, batchConfig, seed);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (IOException | RuntimeException exception) {
            result = failedSubmission(question, config, seed, exception.getMessage());
        }
        Path report = grader.writeResult(question, studentDirectory, batchConfig, result);
        return new SubmissionResult(archive.getFileName().toString(), report.getFileName().toString(), result);
    }

    private void stageSources(Question question, Path source, Path studentDirectory) throws IOException {
        Files.createDirectory(studentDirectory);
        Files.copy(source, studentDirectory.resolve(question.getClassName() + ".java"),
                StandardCopyOption.REPLACE_EXISTING);
        for (String companion : question.getCompanionSourceNames()) {
            Path companionSource = source.getParent().resolve(companion);
            if (!Files.isRegularFile(companionSource)) {
                throw new IOException("could not find required companion source " + companion);
            }
            Files.copy(companionSource, studentDirectory.resolve(companion), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<Path> findArchives(Path directory, AppConfig appConfig) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> appConfig.getArchiveFilePattern().matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString(),
                            String.CASE_INSENSITIVE_ORDER).thenComparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private Path findStudentSource(Path extracted, Question question) throws IOException {
        String expectedName = question.getClassName() + ".java";
        try (Stream<Path> entries = Files.walk(extracted)) {
            List<Path> matches = entries.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(expectedName))
                    .toList();
            if (matches.isEmpty()) {
                throw new IOException("could not find " + expectedName + " in the ZIP contents");
            }
            if (matches.size() > 1) {
                throw new IOException("ZIP contains multiple " + expectedName + " files; source is ambiguous");
            }
            return matches.get(0);
        }
    }

    private RandomizedResult failedSubmission(Question question, RandomizedConfig config,
                                               long seed, String reason) {
        int total = Math.multiplyExact(question.getTestCases().size(), config.runsPerCase());
        return new RandomizedResult(false, 0, total, seed, List.of(),
                reason == null || reason.isBlank() ? "submission could not be graded" : reason);
    }

    private RandomizedConfig configForBatch(RandomizedConfig config, Path outputDirectory) {
        return new RandomizedConfig(config.resourcesDirectory(), outputDirectory, config.javaCommand(),
                config.javacCommand(), config.runsPerCase(), config.timeoutMillis(), config.maxFailuresShown(),
                config.examplesPerCase(), config.maxOutputBytes());
    }

    private boolean overlaps(Path first, Path second) throws IOException {
        Path canonicalFirst = canonicalize(first);
        Path canonicalSecond = canonicalize(second);
        return canonicalFirst.startsWith(canonicalSecond) || canonicalSecond.startsWith(canonicalFirst);
    }

    private Path canonicalize(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path existing = absolute;
        List<Path> missingNames = new ArrayList<>();
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            Path name = existing.getFileName();
            if (name != null) missingNames.add(name);
            existing = existing.getParent();
        }
        if (existing == null) return absolute;
        Path canonical = existing.toRealPath();
        for (int index = missingNames.size() - 1; index >= 0; index--) {
            canonical = canonical.resolve(missingNames.get(index));
        }
        return canonical.normalize();
    }

    private String safeName(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank()) return "submission";
        return sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
    }

    private void writeSummary(Path output, List<SubmissionResult> submissions) throws IOException {
        StringBuilder csv = new StringBuilder("archive,result,status,passed,total,seed,error\n");
        for (SubmissionResult submission : submissions) {
            RandomizedResult result = submission.result();
            csv.append(csv(submission.archive())).append(',')
                    .append(csv(submission.report())).append(',')
                    .append(result.passed() ? "PASS" : "FAIL").append(',')
                    .append(result.passedCount()).append(',').append(result.totalCount()).append(',')
                    .append(result.seed()).append(',').append(csv(result.errorReason())).append('\n');
        }
        Files.writeString(output, csv, StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> entries = Files.walk(root)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    public record BatchResult(Path outputDirectory, Path summaryFile, List<SubmissionResult> submissions) {
        public BatchResult {
            submissions = List.copyOf(submissions);
        }

        public long passedCount() {
            return submissions.stream().filter(submission -> submission.result().passed()).count();
        }

        public long failedCount() {
            return submissions.size() - passedCount();
        }
    }

    public record SubmissionResult(String archive, String report, RandomizedResult result) { }
}
