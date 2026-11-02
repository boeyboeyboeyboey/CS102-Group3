package autograder.submission;

import autograder.config.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SafeZipExtractor {
    private static final String WINDOWS_INVALID_CHARACTERS = "<>:\"|?*";
    private static final Pattern WINDOWS_RESERVED_NAME = Pattern.compile(
            "(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])");

    private final AppConfig config;

    public SafeZipExtractor(AppConfig config) {
        this.config = config;
    }

    public void extract(Path archivePath, Path destination) throws IOException {
        Path outputRoot = destination.toAbsolutePath().normalize();
        boolean createdOutputRoot = false;
        try {
            Files.createDirectories(outputRoot.getParent());
            Files.createDirectory(outputRoot);
            createdOutputRoot = true;
            extractEntries(archivePath, outputRoot);
        } catch (IOException | RuntimeException exception) {
            if (createdOutputRoot) {
                try {
                    deleteTree(outputRoot);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw exception;
        }
    }

    private void extractEntries(Path archivePath, Path outputRoot) throws IOException {
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            long entryCount = 0;
            long totalBytes = 0;
            byte[] buffer = new byte[config.getIoBufferBytes()];

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entryCount++;
                if (entryCount > config.getMaxArchiveEntries()) {
                    throw new IOException("archive entry count exceeds configured limit");
                }

                Path outputPath = resolveEntry(outputRoot, entry.getName());
                long declaredSize = entry.getSize();
                if (declaredSize > config.getMaxEntryBytes()) {
                    throw new IOException("archive entry exceeds configured size limit: " + entry.getName());
                }
                if (declaredSize >= 0 && declaredSize > config.getMaxArchiveBytes() - totalBytes) {
                    throw new IOException("archive exceeds configured uncompressed size limit");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    totalBytes += copyEntry(zipFile, entry, outputPath, buffer, totalBytes);
                }
            }
        }
    }

    private long copyEntry(ZipFile zipFile, ZipEntry entry, Path outputPath,
                           byte[] buffer, long totalBytes) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        long entryBytes = 0;
        CRC32 checksum = new CRC32();
        try (InputStream input = zipFile.getInputStream(entry);
             OutputStream output = Files.newOutputStream(outputPath,
                     StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                if (bytesRead > config.getMaxEntryBytes() - entryBytes) {
                    throw new IOException("archive entry exceeds configured size limit: " + entry.getName());
                }
                if (bytesRead > config.getMaxArchiveBytes() - totalBytes - entryBytes) {
                    throw new IOException("archive exceeds configured uncompressed size limit");
                }
                output.write(buffer, 0, bytesRead);
                checksum.update(buffer, 0, bytesRead);
                entryBytes += bytesRead;
            }
        }
        if (entry.getSize() >= 0 && entryBytes != entry.getSize()) {
            throw new IOException("archive entry size does not match its directory record: "
                    + entry.getName());
        }
        if (entry.getCrc() >= 0 && checksum.getValue() != entry.getCrc()) {
            throw new IOException("archive entry checksum does not match its directory record: "
                    + entry.getName());
        }
        return entryBytes;
    }

    private Path resolveEntry(Path outputRoot, String entryName) throws IOException {
        if (entryName.isBlank()) {
            throw new IOException("archive contains an empty entry path");
        }
        String portableEntryName = entryName.replace('\\', '/');
        rejectWindowsInvalidNames(portableEntryName, entryName);
        Path relativePath = Path.of(portableEntryName);
        if (relativePath.isAbsolute()) {
            throw new IOException("archive contains an absolute entry path: " + entryName);
        }
        for (Path segment : relativePath) {
            if (segment.toString().equals("..")) {
                throw new IOException("archive contains a parent traversal path: " + entryName);
            }
        }
        Path outputPath = outputRoot.resolve(relativePath).normalize();
        if (!outputPath.startsWith(outputRoot)) {
            throw new IOException("archive entry escapes extraction directory: " + entryName);
        }
        return outputPath;
    }

    private void rejectWindowsInvalidNames(String portableName, String entryName) throws IOException {
        for (String segment : portableName.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                continue;
            }
            if (hasWindowsInvalidName(segment)) {
                throw new IOException("archive entry has a name that is invalid on Windows: "
                        + sanitize(entryName));
            }
        }
    }

    private static boolean hasWindowsInvalidName(String segment) {
        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (character < 0x20 || WINDOWS_INVALID_CHARACTERS.indexOf(character) >= 0) {
                return true;
            }
        }
        char last = segment.charAt(segment.length() - 1);
        if (last == '.' || last == ' ') {
            return true;
        }
        int dot = segment.indexOf('.');
        String baseName = dot < 0 ? segment : segment.substring(0, dot);
        return WINDOWS_RESERVED_NAME.matcher(baseName).matches();
    }

    private static String sanitize(String name) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            text.append(character < 0x20 ? '?' : character);
        }
        return text.toString();
    }

    private void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            Path[] tree = paths.sorted((first, second) -> second.compareTo(first))
                    .toArray(Path[]::new);
            for (Path path : tree) {
                Files.deleteIfExists(path);
            }
        }
    }
}
