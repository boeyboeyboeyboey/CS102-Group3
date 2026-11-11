package autograder.config;

import autograder.config.AppConfigGroups.ArchiveIdentityPatterns;
import autograder.config.AppConfigGroups.ArchiveLimits;
import autograder.config.AppConfigGroups.DirectoryPrefixes;
import autograder.config.AppConfigGroups.FileNamePatterns;
import autograder.config.AppConfigGroups.IdentityLabels;
import autograder.config.AppConfigGroups.IdentityPatterns;
import autograder.config.AppConfigGroups.LogPaths;
import autograder.config.AppConfigGroups.RosterSettings;
import autograder.config.AppConfigGroups.RunDirectories;
import autograder.config.AppConfigGroups.ScoreSheetSettings;
import autograder.model.AssessmentDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Directories, file names and name patterns that locate the input, working and output files of a run. */
public final class AppConfig {
    private final Path inputDirectory;
    private final Path extractionDirectory;
    private final Path workingDirectory;
    private final String runDirectoryPrefix;
    private final String workingRunDirectoryPrefix;
    private final String workingSubmissionDirectoryPrefix;
    private final Path anomalyLog;
    private final Path correctionLog;
    private final Pattern archiveFilePattern;
    private final Pattern javaSourcePattern;
    private final Pattern questionDirectoryPattern;
    private final List<String> requiredQuestionDirectories;
    private final Pattern metadataDirectoryPattern;
    private final Pattern identityNamePattern;
    private final Pattern identityEmailPattern;
    private final Pattern identityUsernamePattern;
    private final String identityNameLabel;
    private final String identityEmailLabel;
    private final String identityUsernameLabel;
    private final Pattern archiveUsernamePattern;
    private final Pattern wrapperUsernamePattern;
    private final Pattern wrapperStudentIdPattern;
    private final Pattern wrapperPlaceholderPattern;
    private final String rosterPrefix;
    private final String rosterLastNamePlaceholder;
    private final int identityHeaderMaxLines;
    private final int ioBufferBytes;
    private final int maxArchiveEntries;
    private final long maxEntryBytes;
    private final long maxArchiveBytes;
    private final Path scoreSheetPath;
    private final Map<String, String> scoreSheetColumns;
    private final AssessmentDefinition assessment;

    AppConfig(RunDirectories directories, DirectoryPrefixes prefixes, LogPaths logs,
              FileNamePatterns filePatterns, List<String> requiredQuestionDirectories,
              IdentityPatterns identityPatterns, IdentityLabels identityLabels,
              ArchiveIdentityPatterns archiveIdentityPatterns, RosterSettings roster,
              ArchiveLimits limits, ScoreSheetSettings scoreSheet, AssessmentDefinition assessment) {
        this.inputDirectory = directories.inputDirectory();
        this.extractionDirectory = directories.extractionDirectory();
        this.workingDirectory = directories.workingDirectory();
        this.runDirectoryPrefix = prefixes.runDirectoryPrefix();
        this.workingRunDirectoryPrefix = prefixes.workingRunDirectoryPrefix();
        this.workingSubmissionDirectoryPrefix = prefixes.workingSubmissionDirectoryPrefix();
        this.anomalyLog = logs.anomalyLog();
        this.correctionLog = logs.correctionLog();
        this.archiveFilePattern = filePatterns.archiveFilePattern();
        this.javaSourcePattern = filePatterns.javaSourcePattern();
        this.questionDirectoryPattern = filePatterns.questionDirectoryPattern();
        this.requiredQuestionDirectories = Collections.unmodifiableList(
                new ArrayList<>(requiredQuestionDirectories));
        this.metadataDirectoryPattern = filePatterns.metadataDirectoryPattern();
        this.identityNamePattern = identityPatterns.namePattern();
        this.identityEmailPattern = identityPatterns.emailPattern();
        this.identityUsernamePattern = identityPatterns.usernamePattern();
        this.identityNameLabel = identityLabels.nameLabel();
        this.identityEmailLabel = identityLabels.emailLabel();
        this.identityUsernameLabel = identityLabels.usernameLabel();
        this.archiveUsernamePattern = archiveIdentityPatterns.archiveUsernamePattern();
        this.wrapperUsernamePattern = archiveIdentityPatterns.wrapperUsernamePattern();
        this.wrapperStudentIdPattern = archiveIdentityPatterns.wrapperStudentIdPattern();
        this.wrapperPlaceholderPattern = archiveIdentityPatterns.wrapperPlaceholderPattern();
        this.rosterPrefix = roster.rosterPrefix();
        this.rosterLastNamePlaceholder = roster.rosterLastNamePlaceholder();
        this.identityHeaderMaxLines = roster.identityHeaderMaxLines();
        this.ioBufferBytes = limits.ioBufferBytes();
        this.maxArchiveEntries = limits.maxArchiveEntries();
        this.maxEntryBytes = limits.maxEntryBytes();
        this.maxArchiveBytes = limits.maxArchiveBytes();
        this.scoreSheetPath = scoreSheet.scoreSheetPath();
        this.scoreSheetColumns = Collections.unmodifiableMap(
                new LinkedHashMap<>(scoreSheet.scoreSheetColumns()));
        this.assessment = assessment;
    }

    public static AppConfig load(Path configPath) throws IOException {
        return AppConfigLoader.load(configPath);
    }

    static AppConfig load(Properties properties, Path baseDirectory)
            throws IOException {
        return AppConfigLoader.load(properties, baseDirectory);
    }

    static Path canonicalize(Path path) throws IOException {
        return ConfigPathPolicy.canonicalize(path);
    }

    public Path getInputDirectory() {
        return inputDirectory;
    }

    public Path getExtractionDirectory() {
        return extractionDirectory;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public String getRunDirectoryPrefix() {
        return runDirectoryPrefix;
    }

    public String getWorkingRunDirectoryPrefix() {
        return workingRunDirectoryPrefix;
    }

    public String getWorkingSubmissionDirectoryPrefix() {
        return workingSubmissionDirectoryPrefix;
    }

    public Path getAnomalyLog() {
        return anomalyLog;
    }

    public Path getCorrectionLog() {
        return correctionLog;
    }

    public Pattern getArchiveFilePattern() {
        return archiveFilePattern;
    }

    public Pattern getJavaSourcePattern() {
        return javaSourcePattern;
    }

    public Pattern getQuestionDirectoryPattern() {
        return questionDirectoryPattern;
    }

    public List<String> getRequiredQuestionDirectories() {
        return requiredQuestionDirectories;
    }

    public Pattern getMetadataDirectoryPattern() {
        return metadataDirectoryPattern;
    }

    public Pattern getIdentityNamePattern() {
        return identityNamePattern;
    }

    public Pattern getIdentityEmailPattern() {
        return identityEmailPattern;
    }

    public Pattern getIdentityUsernamePattern() {
        return identityUsernamePattern;
    }

    public String getIdentityNameLabel() {
        return identityNameLabel;
    }

    public String getIdentityEmailLabel() {
        return identityEmailLabel;
    }

    public String getIdentityUsernameLabel() {
        return identityUsernameLabel;
    }

    public Pattern getArchiveUsernamePattern() {
        return archiveUsernamePattern;
    }

    public Pattern getWrapperUsernamePattern() {
        return wrapperUsernamePattern;
    }

    public Pattern getWrapperStudentIdPattern() {
        return wrapperStudentIdPattern;
    }

    public Pattern getWrapperPlaceholderPattern() {
        return wrapperPlaceholderPattern;
    }

    public String getRosterPrefix() {
        return rosterPrefix;
    }

    public String getRosterLastNamePlaceholder() {
        return rosterLastNamePlaceholder;
    }

    public int getIdentityHeaderMaxLines() {
        return identityHeaderMaxLines;
    }

    public int getIoBufferBytes() {
        return ioBufferBytes;
    }

    public int getMaxArchiveEntries() {
        return maxArchiveEntries;
    }

    public long getMaxEntryBytes() {
        return maxEntryBytes;
    }

    public long getMaxArchiveBytes() {
        return maxArchiveBytes;
    }

    public Path getScoreSheetPath() {
        return scoreSheetPath;
    }

    public Map<String, String> getScoreSheetColumns() {
        return scoreSheetColumns;
    }

    public AssessmentDefinition getAssessment() {
        return assessment;
    }
}
