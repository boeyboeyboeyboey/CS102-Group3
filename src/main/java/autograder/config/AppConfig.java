package autograder.config;

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

    AppConfig(Path inputDirectory, Path extractionDirectory, Path workingDirectory,
              String runDirectoryPrefix, String workingRunDirectoryPrefix,
              String workingSubmissionDirectoryPrefix, Path anomalyLog,
              Path correctionLog, Pattern archiveFilePattern, Pattern javaSourcePattern,
              Pattern questionDirectoryPattern, List<String> requiredQuestionDirectories,
              Pattern metadataDirectoryPattern, Pattern identityNamePattern,
              Pattern identityEmailPattern, Pattern identityUsernamePattern,
              String identityNameLabel, String identityEmailLabel,
              String identityUsernameLabel, Pattern archiveUsernamePattern,
              Pattern wrapperUsernamePattern, Pattern wrapperStudentIdPattern,
              Pattern wrapperPlaceholderPattern, String rosterPrefix,
              String rosterLastNamePlaceholder, int identityHeaderMaxLines,
              int ioBufferBytes, int maxArchiveEntries, long maxEntryBytes,
              long maxArchiveBytes, Path scoreSheetPath,
              Map<String, String> scoreSheetColumns, AssessmentDefinition assessment) {
        this.inputDirectory = inputDirectory;
        this.extractionDirectory = extractionDirectory;
        this.workingDirectory = workingDirectory;
        this.runDirectoryPrefix = runDirectoryPrefix;
        this.workingRunDirectoryPrefix = workingRunDirectoryPrefix;
        this.workingSubmissionDirectoryPrefix = workingSubmissionDirectoryPrefix;
        this.anomalyLog = anomalyLog;
        this.correctionLog = correctionLog;
        this.archiveFilePattern = archiveFilePattern;
        this.javaSourcePattern = javaSourcePattern;
        this.questionDirectoryPattern = questionDirectoryPattern;
        this.requiredQuestionDirectories = Collections.unmodifiableList(
                new ArrayList<>(requiredQuestionDirectories));
        this.metadataDirectoryPattern = metadataDirectoryPattern;
        this.identityNamePattern = identityNamePattern;
        this.identityEmailPattern = identityEmailPattern;
        this.identityUsernamePattern = identityUsernamePattern;
        this.identityNameLabel = identityNameLabel;
        this.identityEmailLabel = identityEmailLabel;
        this.identityUsernameLabel = identityUsernameLabel;
        this.archiveUsernamePattern = archiveUsernamePattern;
        this.wrapperUsernamePattern = wrapperUsernamePattern;
        this.wrapperStudentIdPattern = wrapperStudentIdPattern;
        this.wrapperPlaceholderPattern = wrapperPlaceholderPattern;
        this.rosterPrefix = rosterPrefix;
        this.rosterLastNamePlaceholder = rosterLastNamePlaceholder;
        this.identityHeaderMaxLines = identityHeaderMaxLines;
        this.ioBufferBytes = ioBufferBytes;
        this.maxArchiveEntries = maxArchiveEntries;
        this.maxEntryBytes = maxEntryBytes;
        this.maxArchiveBytes = maxArchiveBytes;
        this.scoreSheetPath = scoreSheetPath;
        this.scoreSheetColumns = Collections.unmodifiableMap(
                new LinkedHashMap<>(scoreSheetColumns));
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
