package autograder.service.submission;

import autograder.model.Anomaly;
import autograder.model.Severity;
import autograder.model.StudentRecord;
import autograder.service.api.IdentityResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Implements ZIP name, student-number folder, header, then folder-name precedence. */
public final class PrecedenceIdentityResolver implements IdentityResolver {
    private static final int HEADER_LINE_LIMIT = 40;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)email(?:\\s+id)?\\s*:\\s*([a-z][a-z0-9._-]+)(?:@[^\\s*]+)?");

    private final int searchDepth;

    public PrecedenceIdentityResolver(int searchDepth) {
        this.searchDepth = searchDepth;
    }

    @Override
    public Optional<StudentRecord> resolve(Path archive, Path extractedRoot,
            List<StudentRecord> roster, List<Anomaly> anomalies) throws IOException {
        Set<StudentRecord> zipMatches = matchArchive(archive, roster);
        Set<StudentRecord> idFolderMatches = matchFolders(extractedRoot, roster, true);
        Set<StudentRecord> headerMatches = matchHeaders(extractedRoot, roster);
        Set<StudentRecord> usernameFolderMatches = matchFolders(extractedRoot, roster, false);

        StudentRecord selected = firstUnique(zipMatches);
        String evidence = "ZIP filename";
        if (selected == null) {
            selected = firstUnique(idFolderMatches);
            evidence = "student ID folder";
        }
        if (selected == null) {
            selected = firstUnique(headerMatches);
            evidence = "Java header";
        }
        if (selected == null) {
            selected = firstUnique(usernameFolderMatches);
            evidence = "folder name";
        }
        if (selected == null) {
            anomalies.add(new Anomaly("UNRESOLVED_IDENTITY", Severity.ERROR,
                    "No unique roster identity could be resolved for " + archive.getFileName()));
            return Optional.empty();
        }

        anomalies.add(new Anomaly("IDENTITY_RESOLVED", Severity.INFO,
                "Resolved " + selected.getUsername() + " from " + evidence));
        reportConflicts(selected, zipMatches, idFolderMatches, headerMatches,
                usernameFolderMatches, anomalies);
        return Optional.of(selected);
    }

    private Set<StudentRecord> matchArchive(Path archive, List<StudentRecord> roster) {
        String name = archive.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        Set<StudentRecord> matches = new LinkedHashSet<>();
        for (StudentRecord student : roster) {
            String username = student.getUsername().toLowerCase(Locale.ROOT);
            if (name.equals(username) || name.endsWith("-" + username)) {
                matches.add(student);
            }
        }
        return matches;
    }

    private Set<StudentRecord> matchFolders(Path root, List<StudentRecord> roster,
            boolean useOrganizationId) throws IOException {
        Set<StudentRecord> matches = new LinkedHashSet<>();
        try (Stream<Path> paths = Files.walk(root, searchDepth)) {
            paths.filter(Files::isDirectory).forEach(path -> {
                Path fileName = path.getFileName();
                if (fileName == null) {
                    return;
                }
                String candidate = fileName.toString();
                for (StudentRecord student : roster) {
                    String expected = useOrganizationId
                            ? student.getOrganizationId() : student.getUsername();
                    if (!expected.isEmpty() && candidate.equalsIgnoreCase(expected)) {
                        matches.add(student);
                    }
                }
            });
        }
        return matches;
    }

    private Set<StudentRecord> matchHeaders(Path root, List<StudentRecord> roster)
            throws IOException {
        Set<String> tokens = new LinkedHashSet<>();
        List<Path> javaFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root, searchDepth)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(javaFiles::add);
        }
        for (Path javaFile : javaFiles) {
            try (BufferedReader reader = Files.newBufferedReader(javaFile, StandardCharsets.UTF_8)) {
                for (int lineNumber = 0; lineNumber < HEADER_LINE_LIMIT; lineNumber++) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.toLowerCase(Locale.ROOT).contains("email")) {
                        Matcher matcher = EMAIL_PATTERN.matcher(line);
                        while (matcher.find()) {
                            tokens.add(matcher.group(1).toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
        }
        Set<StudentRecord> matches = new LinkedHashSet<>();
        for (StudentRecord student : roster) {
            String username = student.getUsername().toLowerCase(Locale.ROOT);
            String email = student.getEmail().toLowerCase(Locale.ROOT);
            String emailLocalPart = email.contains("@")
                    ? email.substring(0, email.indexOf('@')) : email;
            if (tokens.contains(username) || tokens.contains(emailLocalPart)) {
                matches.add(student);
            }
        }
        return matches;
    }

    private StudentRecord firstUnique(Set<StudentRecord> matches) {
        return matches.size() == 1 ? matches.iterator().next() : null;
    }

    private void reportConflicts(StudentRecord selected, Set<StudentRecord> zipMatches,
            Set<StudentRecord> idMatches, Set<StudentRecord> headerMatches,
            Set<StudentRecord> folderMatches, List<Anomaly> anomalies) {
        List<Set<StudentRecord>> allEvidence = List.of(
                zipMatches, idMatches, headerMatches, folderMatches);
        for (Set<StudentRecord> evidence : allEvidence) {
            for (StudentRecord candidate : evidence) {
                if (!candidate.getUsername().equalsIgnoreCase(selected.getUsername())) {
                    anomalies.add(new Anomaly("IDENTITY_CONFLICT", Severity.WARNING,
                            "Evidence for " + candidate.getUsername()
                                    + " conflicts with selected identity "
                                    + selected.getUsername()));
                }
            }
        }
    }
}
