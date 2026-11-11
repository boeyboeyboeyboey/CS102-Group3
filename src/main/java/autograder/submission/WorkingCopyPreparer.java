package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds the working copy that is graded, applying case, header and wrapper layout corrections without touching the extracted files. */
final class WorkingCopyPreparer {
    private final AppConfig config;
    private final IdentityResolver identityResolver;
    private final CaseVariants caseVariants;

    WorkingCopyPreparer(AppConfig config, IdentityResolver identityResolver) {
        this.config = config;
        this.identityResolver = identityResolver;
        this.caseVariants = new CaseVariants(config);
    }

    Preparation prepare(Path extractionPath, Path studentRoot, Path workingRun,
                        StudentRecord student, List<SubmissionCorrection> headerCorrections)
            throws IOException {
        String username = safeUsername(student);
        Path submissionDirectory = workingRun.resolve(
                config.getWorkingSubmissionDirectoryPrefix() + UUID.randomUUID());
        Path workingRoot = submissionDirectory.resolve(username);
        List<CaseVariants.Variant> variants = caseVariants.find(studentRoot);
        copyTree(studentRoot, workingRoot, variants);

        List<SubmissionCorrection> corrections = new ArrayList<>();
        for (CaseVariants.Variant variant : variants) {
            corrections.add(new SubmissionCorrection("case", variant.actual(), variant.kind(),
                    variant.actualName(), variant.canonicalName(), false));
        }
        corrections.addAll(identityResolver.applyCorrections(workingRoot, headerCorrections));
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

    // required entries with the wrong case are written under their expected names, so the working
    // copy is the same on case-sensitive and case-insensitive filesystems without any rename
    private void copyTree(Path sourceRoot, Path destinationRoot,
                          List<CaseVariants.Variant> variants) throws IOException {
        Map<Path, String> expectedNames = new HashMap<>();
        for (CaseVariants.Variant variant : variants) {
            expectedNames.put(variant.actual(), variant.canonicalName());
        }
        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.toList()) {
                Path relative = sourceRoot.relativize(source);
                Path destination = withExpectedNames(destinationRoot, relative, expectedNames).normalize();
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

    private Path withExpectedNames(Path destinationRoot, Path relative,
                                   Map<Path, String> expectedNames) {
        Path destination = destinationRoot;
        Path original = null;
        for (Path segment : relative) {
            original = original == null ? segment : original.resolve(segment);
            destination = destination.resolve(expectedNames.getOrDefault(original, segment.toString()));
        }
        return destination;
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
