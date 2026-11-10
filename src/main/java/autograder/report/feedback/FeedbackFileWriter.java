package autograder.report.feedback;

import autograder.model.StudentRecord;
import autograder.model.SubmissionResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/** Writes a feedback file under a safe, unique name derived from the student or archive. */
final class FeedbackFileWriter {
    private static final String FILE_EXTENSION = ".md";
    private static final String FALLBACK_FILE_NAME = "submission";
    private static final String ARCHIVE_EXTENSION = ".zip";
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final int MAX_FILE_NAME_ATTEMPTS = 1000;

    private FeedbackFileWriter() { }

    static Path write(Path outputDirectory, String baseName, String content) throws IOException {
        for (int attempt = 1; attempt <= MAX_FILE_NAME_ATTEMPTS; attempt++) {
            String suffix = attempt == 1 ? "" : "-" + attempt;
            Path file = outputDirectory.resolve(baseName + suffix + FILE_EXTENSION);
            try {
                Files.writeString(file, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return file;
            } catch (FileAlreadyExistsException exception) {
                // another submission already uses this name, so try the next suffix
            }
        }
        throw new IOException("could not choose a unique feedback file name for " + baseName);
    }

    static String baseFileName(SubmissionResult submission, String archiveName) {
        StudentRecord student = submission.getIdentity().getStudentRecord();
        String name = student != null ? student.getUsername() : archiveName;
        if (student == null && name.toLowerCase(Locale.ROOT).endsWith(ARCHIVE_EXTENSION)) {
            name = name.substring(0, name.length() - ARCHIVE_EXTENSION.length());
        }
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^[._-]+", "");
        if (safe.length() > MAX_FILE_NAME_LENGTH) {
            safe = safe.substring(0, MAX_FILE_NAME_LENGTH);
        }
        return safe.isEmpty() ? FALLBACK_FILE_NAME : safe;
    }
}
