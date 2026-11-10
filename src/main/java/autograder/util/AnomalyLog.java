package autograder.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Appends unexpected conditions found while grading to a log file. */
public final class AnomalyLog {
    private final Path logPath;

    public AnomalyLog(Path logPath) {
        this.logPath = logPath;
    }

    public synchronized void record(Path archivePath, String message) {
        String safeArchiveName = archivePath.getFileName().toString().replace('\t', ' ');
        String safeMessage = message.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        String line = Instant.now() + "\t" + safeArchiveName + "\t" + safeMessage
                + System.lineSeparator();
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(logPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("could not write anomaly log " + logPath + ": " + exception.getMessage());
        }
        System.err.println(safeArchiveName + ": " + safeMessage);
    }
}
