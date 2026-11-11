package autograder.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/** Records that group the same-typed arguments of the AppConfig constructor by concern. */
final class AppConfigGroups {
    private AppConfigGroups() {
    }

    record RunDirectories(Path inputDirectory, Path extractionDirectory, Path workingDirectory) {
    }

    record DirectoryPrefixes(String runDirectoryPrefix, String workingRunDirectoryPrefix,
                             String workingSubmissionDirectoryPrefix) {
    }

    record LogPaths(Path anomalyLog, Path correctionLog) {
    }

    record FileNamePatterns(Pattern archiveFilePattern, Pattern javaSourcePattern,
                            Pattern questionDirectoryPattern, Pattern metadataDirectoryPattern) {
    }

    record IdentityPatterns(Pattern namePattern, Pattern emailPattern, Pattern usernamePattern) {
    }

    record IdentityLabels(String nameLabel, String emailLabel, String usernameLabel) {
    }

    record ArchiveIdentityPatterns(Pattern archiveUsernamePattern, Pattern wrapperUsernamePattern,
                                   Pattern wrapperStudentIdPattern, Pattern wrapperPlaceholderPattern) {
    }

    record RosterSettings(String rosterPrefix, String rosterLastNamePlaceholder, int identityHeaderMaxLines) {
    }

    record ArchiveLimits(int ioBufferBytes, int maxArchiveEntries, long maxEntryBytes, long maxArchiveBytes) {
    }

    record ScoreSheetSettings(Path scoreSheetPath, Map<String, String> scoreSheetColumns) {
    }
}
