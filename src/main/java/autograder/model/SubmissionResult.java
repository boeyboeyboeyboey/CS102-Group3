package autograder.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A prepared submission: its archive, extracted files, identity and any warnings. */
public final class SubmissionResult {
    private final Path archivePath;
    private final Path extractionPath;
    private final Path studentRoot;
    private final Path workingRoot;
    private final StudentIdentity identity;
    private final List<String> questionDirectories;
    private final List<String> anomalies;
    private final List<SubmissionCorrection> corrections;
    private final SubmissionStatus status;
    private final boolean extracted;

    public SubmissionResult(Path archivePath, Path extractionPath, Path studentRoot,
                            Path workingRoot, StudentIdentity identity,
                            List<String> questionDirectories, List<String> anomalies,
                            List<SubmissionCorrection> corrections, SubmissionStatus status,
                            boolean extracted) {
        this.archivePath = archivePath;
        this.extractionPath = extractionPath;
        this.studentRoot = studentRoot;
        this.workingRoot = workingRoot;
        this.identity = identity;
        this.questionDirectories = Collections.unmodifiableList(
                new ArrayList<>(questionDirectories));
        this.anomalies = Collections.unmodifiableList(new ArrayList<>(anomalies));
        this.corrections = Collections.unmodifiableList(new ArrayList<>(corrections));
        this.status = status;
        this.extracted = extracted;
    }

    public Path getArchivePath() {
        return archivePath;
    }

    public Path getExtractionPath() {
        return extractionPath;
    }

    public Path getStudentRoot() {
        return studentRoot;
    }

    public Path getWorkingRoot() {
        return workingRoot;
    }

    public StudentIdentity getIdentity() {
        return identity;
    }

    public List<String> getQuestionDirectories() {
        return questionDirectories;
    }

    public List<String> getAnomalies() {
        return anomalies;
    }

    public List<SubmissionCorrection> getCorrections() {
        return corrections;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public boolean isExtracted() {
        return extracted;
    }

    public SubmissionResult withReviewAnomaly(String anomaly) {
        List<String> updatedAnomalies = new ArrayList<>(anomalies);
        updatedAnomalies.add(anomaly);
        SubmissionStatus updatedStatus = SubmissionStatus.REVIEW_REQUIRED;
        if (status == SubmissionStatus.FAILED) {
            updatedStatus = SubmissionStatus.FAILED;
        }
        return new SubmissionResult(archivePath, extractionPath, studentRoot, workingRoot,
                identity, questionDirectories, updatedAnomalies, corrections,
                updatedStatus, extracted);
    }
}
