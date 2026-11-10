package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class WorkingCopyPreparer {
    private final AppConfig config;
    private final IdentityResolver identityResolver;

    WorkingCopyPreparer(AppConfig config, IdentityResolver identityResolver) {
        this.config = config;
        this.identityResolver = identityResolver;
    }

    Preparation prepare(Path extractionPath, Path studentRoot, Path workingRun,
                        StudentRecord student, List<SubmissionCorrection> headerCorrections)
            throws IOException {
        String username = safeUsername(student);
        Path submissionDirectory = workingRun.resolve(
                config.getWorkingSubmissionDirectoryPrefix() + UUID.randomUUID());
        Path workingRoot = submissionDirectory.resolve(username);
        copyTree(studentRoot, workingRoot);

        List<SubmissionCorrection> corrections = new ArrayList<>(
                identityResolver.applyCorrections(workingRoot, headerCorrections));
        String originalWrapper = extractionPath.relativize(studentRoot).toString();
        if (needsLayoutCorrection(extractionPath, studentRoot, username)) {
            Path relativePath = Path.of(originalWrapper);
            if (originalWrapper.isEmpty()) {
                originalWrapper = "missing wrapper folder";
                relativePath = Path.of(".");
            }
            corrections.add(new SubmissionCorrection("layout", relativePath, "wrapper root",
                    originalWrapper, username, false));
        }
        return new Preparation(workingRoot, corrections);
    }

    private boolean needsLayoutCorrection(Path extractionPath, Path studentRoot, String username) {
        if (extractionPath.equals(studentRoot)) {
            return true;
        }
        Path relativePath = extractionPath.relativize(studentRoot);
        return relativePath.getNameCount() != 1
                || !relativePath.getFileName().toString().equals(username);
    }

    private String safeUsername(StudentRecord student) throws IOException {
        String username = RosterPrefix.strip(config, student.getUsername());
        Path usernamePath;
        try {
            usernamePath = Path.of(username);
        } catch (RuntimeException exception) {
            throw new IOException("roster username is not a safe directory name", exception);
        }
        if (username.isBlank() || usernamePath.isAbsolute()
                || usernamePath.getNameCount() != 1
                || username.equals(".") || username.equals("..")) {
            throw new IOException("roster username is not a safe directory name");
        }
        return username;
    }

    private void copyTree(Path sourceRoot, Path destinationRoot) throws IOException {
        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.toList()) {
                Path relative = sourceRoot.relativize(source);
                Path destination = destinationRoot.resolve(relative).normalize();
                if (!destination.startsWith(destinationRoot)) {
                    throw new IOException("working copy path escapes its destination");
                }
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Path parent = destination.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    static final class Preparation {
        private final Path workingRoot;
        private final List<SubmissionCorrection> corrections;

        private Preparation(Path workingRoot, List<SubmissionCorrection> corrections) {
            this.workingRoot = workingRoot;
            this.corrections = List.copyOf(corrections);
        }

        Path getWorkingRoot() {
            return workingRoot;
        }

        List<SubmissionCorrection> getCorrections() {
            return corrections;
        }
    }
}
