package autograder.submission;

import autograder.model.SubmissionCorrection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Appends one tab-separated line to the correction log for each correction made to a submission. */
public final class CorrectionLog {
    private final Path logPath;

    public CorrectionLog(Path logPath) {
        this.logPath = logPath;
    }

    public synchronized boolean record(Path archivePath, SubmissionCorrection correction) {
        String line = Instant.now() + "\t" + clean(archivePath.getFileName().toString())
                + "\t" + clean(correction.getType())
                + "\t" + clean(String.valueOf(correction.getRelativePath()))
                + "\t" + clean(correction.getField())
                + "\t" + clean(correction.getOriginalValue())
                + "\t" + clean(correction.getCorrectedValue())
                + System.lineSeparator();
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(logPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException exception) {
            System.err.println("could not write correction log " + logPath + ": "
                    + exception.getMessage());
            return false;
        }
    }

    private String clean(String value) {
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
