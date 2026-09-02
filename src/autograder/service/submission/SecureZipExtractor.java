package autograder.service.submission;

import autograder.model.Anomaly;
import autograder.model.Severity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts ZIP files while enforcing traversal, entry-count, and size limits. */
public final class SecureZipExtractor {
    private static final int BUFFER_SIZE = 8192;

    private final int maximumEntries;
    private final long maximumUncompressedBytes;

    public SecureZipExtractor(int maximumEntries, long maximumUncompressedBytes) {
        this.maximumEntries = maximumEntries;
        this.maximumUncompressedBytes = maximumUncompressedBytes;
    }

    public void extract(Path archive, Path destination, List<Anomaly> anomalies)
            throws IOException {
        Files.createDirectories(destination);
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        int entryCount = 0;
        long totalBytes = 0;
        Set<Path> extractedFiles = new HashSet<>();
        try (InputStream fileInput = Files.newInputStream(archive);
                ZipInputStream zipInput = new ZipInputStream(fileInput)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > maximumEntries) {
                    throw new IOException("ZIP exceeds the configured entry limit: " + archive);
                }
                Path output = safeDestination(normalizedDestination, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    if (!extractedFiles.add(output) || Files.exists(output)) {
                        anomalies.add(new Anomaly("DUPLICATE_ZIP_ENTRY", Severity.WARNING,
                                "Ignored duplicate archive entry " + entry.getName()));
                        totalBytes += drain(zipInput, maximumUncompressedBytes - totalBytes);
                    } else {
                        try (OutputStream fileOutput = Files.newOutputStream(output,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int read;
                            while ((read = zipInput.read(buffer)) != -1) {
                                totalBytes += read;
                                if (totalBytes > maximumUncompressedBytes) {
                                    throw new IOException(
                                            "ZIP exceeds the configured uncompressed-size limit: "
                                                    + archive);
                                }
                                fileOutput.write(buffer, 0, read);
                            }
                        }
                    }
                }
                zipInput.closeEntry();
            }
        }
    }

    private Path safeDestination(Path destination, String entryName) throws IOException {
        if (entryName == null || entryName.indexOf('\0') >= 0) {
            throw new IOException("ZIP contains an invalid entry name");
        }
        String portableName = entryName.replace('\\', '/');
        Path output = destination.resolve(portableName).normalize();
        if (!output.startsWith(destination) || Path.of(portableName).isAbsolute()) {
            throw new IOException("ZIP entry escapes the extraction directory: " + entryName);
        }
        return output;
    }

    private long drain(InputStream input, long remainingLimit) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long consumed = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            consumed += read;
            if (consumed > remainingLimit) {
                throw new IOException("ZIP exceeds the configured uncompressed-size limit");
            }
        }
        return consumed;
    }
}
