package autograder.submission;

import autograder.api.ProgressListener;
import autograder.config.AppConfig;
import autograder.model.ScoreSheet;
import autograder.model.StudentIdentity;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.roster.ScoreSheetRepository;
import autograder.util.AnomalyLog;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SubmissionProcessor {
    private final AppConfig config;
    private ProgressListener progressListener;
    private final SafeZipExtractor zipExtractor;
    private final IdentityResolver identityResolver;
    private final SubmissionValidator validator;
    private final WorkingCopyPreparer workingCopyPreparer;
    private final AnomalyLog anomalyLog;
    private final CorrectionLog correctionLog;

    public SubmissionProcessor(AppConfig config) {
        this(config, null);
    }

    public SubmissionProcessor(AppConfig config, ProgressListener progressListener) {
        this.config = config;
        this.progressListener = progressListener;
        this.zipExtractor = new SafeZipExtractor(config);
        this.identityResolver = new IdentityResolver(config);
        this.validator = new SubmissionValidator(config);
        this.workingCopyPreparer = new WorkingCopyPreparer(config, identityResolver);
        this.anomalyLog = new AnomalyLog(config.getAnomalyLog());
        this.correctionLog = new CorrectionLog(config.getCorrectionLog());
    }

    public List<SubmissionResult> processBatch() throws IOException {
        ScoreSheet scoreSheet = new ScoreSheetRepository(
                config.getScoreSheetPath(), config.getScoreSheetColumns()).load();
        validateAssessmentScore(scoreSheet);
        List<Path> archives = findArchives();
        if (archives.isEmpty()) {
            return List.of();
        }

        Path extractionRun = createRunDirectory(config.getExtractionDirectory(),
                config.getRunDirectoryPrefix());
        Path workingRun = createRunDirectory(config.getWorkingDirectory(),
                config.getWorkingRunDirectoryPrefix());
        List<SubmissionResult> results = new ArrayList<>();
        int completed = 0;
        for (Path archive : archives) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            notifyProgress("ingestion", archive.getFileName().toString(), completed,
                    archives.size());
            Path extractionPath = extractionRun.resolve(
                    config.getRunDirectoryPrefix() + UUID.randomUUID());
            SubmissionResult result = processArchive(archive, extractionPath, workingRun,
                    scoreSheet.getStudents());
            results.add(result);
            completed++;
            notifyProgress("ingestion", archive.getFileName().toString(), completed,
                    archives.size());
        }
        markDuplicateStudents(results);
        return Collections.unmodifiableList(results);
    }

    private SubmissionResult processArchive(Path archive, Path extractionPath,
                                            Path workingRun, List<StudentRecord> roster) {
        List<String> anomalies = new ArrayList<>();
        List<SubmissionCorrection> corrections = new ArrayList<>();
        StudentIdentity identity = fallbackIdentity(archive);
        Path studentRoot = null;
        Path workingRoot = null;
        boolean extracted = false;

        try {
            zipExtractor.extract(archive, extractionPath);
            extracted = true;
        } catch (IOException | RuntimeException exception) {
            addAnomaly(archive, anomalies, "archive extraction failed: " + message(exception));
            try {
                IdentityResolver.Resolution resolution = identityResolver.resolve(archive,
                        extractionPath, null, config.getAssessment(), roster);
                identity = resolution.getIdentity();
                for (String anomaly : resolution.getAnomalies()) {
                    addAnomaly(archive, anomalies, anomaly);
                }
            } catch (IOException | RuntimeException identityException) {
                addAnomaly(archive, anomalies, "identity could not be checked: "
                        + message(identityException));
            }
            return new SubmissionResult(archive, extractionPath, null, null, identity,
                    List.of(), anomalies, corrections, SubmissionStatus.FAILED, false);
        }

        try {
            SubmissionValidator.RootSelection selection = validator.selectStudentRoot(extractionPath);
            studentRoot = selection.getRoot();
            for (String anomaly : selection.getAnomalies()) {
                addAnomaly(archive, anomalies, anomaly);
            }
            List<String> questionDirectories = new ArrayList<>();
            if (studentRoot != null) {
                questionDirectories = validator.findQuestionDirectories(studentRoot);
                for (String anomaly : validator.findMissingQuestionDirectories(studentRoot)) {
                    addAnomaly(archive, anomalies, anomaly);
                }
            }

            IdentityResolver.Resolution resolution = identityResolver.resolve(archive,
                    extractionPath, studentRoot, config.getAssessment(), roster);
            identity = resolution.getIdentity();
            for (String anomaly : resolution.getAnomalies()) {
                addAnomaly(archive, anomalies, anomaly);
            }

            StudentRecord student = identity.getStudentRecord();
            if (student != null && studentRoot != null) {
                WorkingCopyPreparer.Preparation preparation = workingCopyPreparer.prepare(
                        extractionPath, studentRoot, workingRun, student, resolution.getCorrections());
                workingRoot = preparation.getWorkingRoot();
                corrections.addAll(preparation.getCorrections());
                for (SubmissionCorrection correction : corrections) {
                    if (!correctionLog.record(archive, correction)) {
                        addAnomaly(archive, anomalies,
                                "could not persist a submission correction record");
                        break;
                    }
                }
            }

            SubmissionStatus status = SubmissionStatus.REVIEW_REQUIRED;
            if (anomalies.isEmpty() && workingRoot != null) {
                status = SubmissionStatus.READY;
            }
            return new SubmissionResult(archive, extractionPath, studentRoot, workingRoot,
                    identity, questionDirectories, anomalies, corrections, status, extracted);
        } catch (IOException | RuntimeException exception) {
            addAnomaly(archive, anomalies, "submission preparation failed: " + message(exception));
            return new SubmissionResult(archive, extractionPath, studentRoot, workingRoot,
                    identity, List.of(), anomalies, corrections, SubmissionStatus.FAILED, extracted);
        }
    }

    private StudentIdentity fallbackIdentity(Path archive) {
        String fileName = archive.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String name = fileName;
        if (extensionIndex > 0) {
            name = fileName.substring(0, extensionIndex);
        }
        return new StudentIdentity("archive-derived fallback: " + name, "", "",
                List.of(), List.of(), List.of(), null, "archive-derived fallback", List.of());
    }

    private void validateAssessmentScore(ScoreSheet scoreSheet) throws IOException {
        int denominatorIndex = -1;
        String denominatorColumn = scoreSheet.getColumns().get("denominator");
        for (int index = 0; index < scoreSheet.getHeaders().size(); index++) {
            if (scoreSheet.getHeaders().get(index).equals(denominatorColumn)) {
                denominatorIndex = index;
                break;
            }
        }
        if (denominatorIndex < 0) {
            throw new IOException("score sheet denominator column is unavailable");
        }
        for (int rowIndex = 0; rowIndex < scoreSheet.getRows().size(); rowIndex++) {
            List<String> row = scoreSheet.getRows().get(rowIndex);
            double denominator;
            try {
                denominator = Double.parseDouble(row.get(denominatorIndex).trim());
            } catch (NumberFormatException exception) {
                throw new IOException("score sheet row has an invalid denominator", exception);
            }
            if (Double.compare(denominator, config.getAssessment().getMaxScore()) != 0) {
                throw new IOException("score sheet denominator does not match configured assessment max score");
            }
        }
    }

    private List<Path> findArchives() throws IOException {
        if (!Files.isDirectory(config.getInputDirectory())) {
            throw new IOException("submission input directory does not exist: "
                    + config.getInputDirectory());
        }
        List<Path> archives = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(config.getInputDirectory())) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry)
                        && config.getArchiveFilePattern().matcher(
                        entry.getFileName().toString()).matches()) {
                    archives.add(entry);
                }
            }
        }
        archives.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return archives;
    }

    private Path createRunDirectory(Path parent, String prefix) throws IOException {
        Files.createDirectories(parent);
        Path runDirectory = parent.resolve(prefix + UUID.randomUUID());
        Files.createDirectory(runDirectory);
        return runDirectory;
    }

    private void markDuplicateStudents(List<SubmissionResult> results) {
        Map<String, List<Integer>> submissionsByStudent = new HashMap<>();
        for (int index = 0; index < results.size(); index++) {
            StudentRecord student = results.get(index).getIdentity().getStudentRecord();
            if (student != null) {
                submissionsByStudent.computeIfAbsent(student.getId(), key -> new ArrayList<>())
                        .add(index);
            }
        }
        for (Map.Entry<String, List<Integer>> entry : submissionsByStudent.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (int index : entry.getValue()) {
                    SubmissionResult result = results.get(index).withReviewAnomaly(
                            "duplicate submission found for the same score sheet student");
                    results.set(index, result);
                    anomalyLog.record(result.getArchivePath(),
                            "duplicate submission found for the same score sheet student");
                }
            }
        }
    }

    private void addAnomaly(Path archive, List<String> anomalies, String anomaly) {
        anomalies.add(anomaly);
        anomalyLog.record(archive, anomaly);
    }

    private void notifyProgress(String stage, String item, int completed, int total) {
        if (progressListener != null) {
            try {
                progressListener.onProgress(stage, item, completed, total);
            } catch (RuntimeException exception) {
                System.err.println("progress listener failed; continuing without progress updates: "
                        + message(exception));
                progressListener = null;
            }
        }
    }

    private String message(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
