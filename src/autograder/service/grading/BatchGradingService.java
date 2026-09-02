package autograder.service.grading;

import autograder.config.AppConfig;
import autograder.model.Anomaly;
import autograder.model.BatchResult;
import autograder.model.NormalizedSubmission;
import autograder.model.QuestionResult;
import autograder.model.QuestionSpec;
import autograder.model.Severity;
import autograder.model.StudentGrade;
import autograder.model.StudentRecord;
import autograder.service.api.AnomalyDetector;
import autograder.service.api.BatchAnalyzer;
import autograder.service.api.GradingEngine;
import autograder.service.api.IdentityResolver;
import autograder.service.api.ProgressListener;
import autograder.service.api.ReportGenerator;
import autograder.service.submission.SecureZipExtractor;
import autograder.service.submission.SubmissionNormalizer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Coordinates extraction, normalization, grading, and reporting for a complete batch. */
public final class BatchGradingService {
    private final AppConfig config;
    private final List<StudentRecord> roster;
    private final SecureZipExtractor extractor;
    private final IdentityResolver identityResolver;
    private final List<AnomalyDetector> anomalyDetectors;
    private final SubmissionNormalizer normalizer;
    private final GradingEngine gradingEngine;
    private final List<BatchAnalyzer> batchAnalyzers;
    private final ReportGenerator reportGenerator;
    private final ProgressListener listener;
    private final Path batchWorkspace;

    public BatchGradingService(AppConfig config, List<StudentRecord> roster,
            SecureZipExtractor extractor, IdentityResolver identityResolver,
            List<AnomalyDetector> anomalyDetectors, SubmissionNormalizer normalizer,
            GradingEngine gradingEngine, List<BatchAnalyzer> batchAnalyzers,
            ReportGenerator reportGenerator, ProgressListener listener,
            Path batchWorkspace) {
        this.config = config;
        this.roster = roster;
        this.extractor = extractor;
        this.identityResolver = identityResolver;
        this.anomalyDetectors = anomalyDetectors;
        this.normalizer = normalizer;
        this.gradingEngine = gradingEngine;
        this.batchAnalyzers = batchAnalyzers;
        this.reportGenerator = reportGenerator;
        this.listener = listener;
        this.batchWorkspace = batchWorkspace;
    }

    public BatchResult run() throws IOException, InterruptedException {
        List<Path> archives = discoverArchives(config.getPath("submissions.directory"));
        listener.onProgress("Discovered " + archives.size() + " ZIP submission(s).");
        Map<String, StudentGrade> gradesByUsername = new HashMap<>();
        List<NormalizedSubmission> normalizedSubmissions = new ArrayList<>();
        int number = 0;
        for (Path archive : archives) {
            number++;
            listener.onProgress("[" + number + "/" + archives.size() + "] "
                    + archive.getFileName());
            Path extractionDirectory = batchWorkspace.resolve("extracted")
                    .resolve(String.format(Locale.ROOT, "%03d", number));
            List<Anomaly> anomalies = new ArrayList<>();
            try {
                extractor.extract(archive, extractionDirectory, anomalies);
                Optional<StudentRecord> identity = identityResolver.resolve(archive,
                        extractionDirectory, roster, anomalies);
                if (identity.isEmpty()) {
                    printAnomalies(anomalies);
                    continue;
                }
                StudentRecord student = identity.get();
                String key = student.getUsername().toLowerCase(Locale.ROOT);
                if (gradesByUsername.containsKey(key)) {
                    anomalies.add(new Anomaly("DUPLICATE_SUBMISSION", Severity.ERROR,
                            "A submission was already graded for " + student.getUsername()));
                    printAnomalies(anomalies);
                    continue;
                }
                for (AnomalyDetector detector : anomalyDetectors) {
                    anomalies.addAll(detector.detect(extractionDirectory, student));
                }
                NormalizedSubmission normalized = normalizer.normalize(student, archive,
                        extractionDirectory, config.getQuestions(), anomalies);
                normalizedSubmissions.add(normalized);
                StudentGrade grade = gradingEngine.grade(normalized, listener);
                gradesByUsername.put(key, grade);
                listener.onProgress("  Total: " + format(grade.getTotalScore()) + "/"
                        + format(maximumTotal()));
                printAnomalies(grade.getAnomalies());
            } catch (RuntimeException | IOException exception) {
                anomalies.add(new Anomaly("SUBMISSION_FAILED", Severity.ERROR,
                        exception.getMessage() == null
                                ? exception.getClass().getSimpleName() : exception.getMessage()));
                printAnomalies(anomalies);
            }
        }

        for (BatchAnalyzer analyzer : batchAnalyzers) {
            List<Anomaly> batchAnomalies = analyzer.analyze(normalizedSubmissions);
            printAnomalies(batchAnomalies);
        }

        List<StudentGrade> orderedGrades = new ArrayList<>();
        for (StudentRecord student : roster) {
            String key = student.getUsername().toLowerCase(Locale.ROOT);
            StudentGrade grade = gradesByUsername.get(key);
            if (grade == null) {
                grade = missingSubmissionGrade(student);
                listener.onProgress(student.getUsername() + ": no gradeable submission; score 0.0");
            }
            orderedGrades.add(grade);
        }
        Path report = reportGenerator.generate(orderedGrades);
        return new BatchResult(orderedGrades, report);
    }

    private List<Path> discoverArchives(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Submission directory does not exist: " + directory);
        }
        List<Path> archives = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString()
                            .toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(archives::add);
        }
        return archives;
    }

    private StudentGrade missingSubmissionGrade(StudentRecord student) {
        Map<String, QuestionResult> results = new LinkedHashMap<>();
        for (QuestionSpec question : config.getQuestions()) {
            results.put(question.getId(), new QuestionResult(question.getId(), 0.0,
                    question.getMaximumScore(), List.of()));
        }
        List<Anomaly> anomalies = List.of(new Anomaly("MISSING_SUBMISSION", Severity.ERROR,
                "No gradeable archive was matched to this roster row"));
        return new StudentGrade(student, results, anomalies);
    }

    private void printAnomalies(List<Anomaly> anomalies) {
        for (Anomaly anomaly : anomalies) {
            listener.onProgress("  " + anomaly);
        }
    }

    private double maximumTotal() {
        double total = 0.0;
        for (QuestionSpec question : config.getQuestions()) {
            total += question.getMaximumScore();
        }
        return total;
    }

    private String format(double score) {
        return String.format(Locale.ROOT, "%.1f", score);
    }
}
