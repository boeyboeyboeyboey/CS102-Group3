package autograder.gui;

import autograder.config.RunSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** Checks the four inputs without creating or writing anything. */
public final class ReadinessChecker {
    static final String DEFAULT_ARCHIVE_REGEX = "(?i).*\\.zip";
    private static final String ARCHIVE_KEY = "archive.file.regex";

    public Readiness check(GuiInputs inputs) {
        FieldStatus config = checkConfig(inputs.config());
        if (!config.ok()) {
            return new Readiness(List.of(config), null);
        }
        List<FieldStatus> statuses = new ArrayList<>();
        statuses.add(config);
        statuses.add(checkSubmissions(inputs.submissions(), archivePattern(inputs.config())));
        statuses.add(checkScoreSheet(inputs.scoreSheet()));
        statuses.add(checkOutput(inputs.output()));
        if (statuses.stream().anyMatch(status -> !status.ok())) {
            return new Readiness(statuses, null);
        }
        FieldStatus overlap = checkOverlap(inputs);
        if (overlap != null) {
            return new Readiness(replace(statuses, overlap), null);
        }
        return load(inputs, statuses);
    }

    private Readiness load(GuiInputs inputs, List<FieldStatus> statuses) {
        try {
            RunSettings settings = RunSettings.load(inputs.config(), inputs.toOverrides());
            return new Readiness(statuses, settings);
        } catch (IllegalArgumentException | IOException exception) {
            return new Readiness(replace(statuses, mapFailure(exception)), null);
        }
    }

    private static List<FieldStatus> replace(List<FieldStatus> statuses, FieldStatus error) {
        List<FieldStatus> result = new ArrayList<>();
        for (FieldStatus status : statuses) {
            result.add(status.field() == error.field() ? error : status);
        }
        return result;
    }

    static FieldStatus mapFailure(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return FieldStatus.error(InputField.CONFIG, exception.getClass().getSimpleName());
        }
        if (message.contains("input.directory")) {
            return FieldStatus.error(InputField.SUBMISSIONS, "Submissions folder not usable");
        }
        if (message.contains("score.sheet.path")) {
            return FieldStatus.error(InputField.SCORE_SHEET, "Score sheet not usable");
        }
        if (message.contains("grading.directory") || message.contains("grading directory")
                || message.contains("output directory")) {
            return FieldStatus.error(InputField.OUTPUT, "Output overlaps a folder or file the grader uses");
        }
        return FieldStatus.error(InputField.CONFIG, "Config problem: " + message);
    }

    private static FieldStatus checkConfig(Path config) {
        if (config == null) {
            return FieldStatus.error(InputField.CONFIG, "Choose a config file");
        }
        if (!Files.exists(config)) {
            return FieldStatus.error(InputField.CONFIG, "File not found");
        }
        if (!Files.isRegularFile(config)) {
            return FieldStatus.error(InputField.CONFIG, "Not a file");
        }
        if (!Files.isReadable(config)) {
            return FieldStatus.error(InputField.CONFIG, "Can't read this file");
        }
        return FieldStatus.ok(InputField.CONFIG, "");
    }

    private static FieldStatus checkSubmissions(Path folder, Pattern pattern) {
        if (folder == null) {
            return FieldStatus.error(InputField.SUBMISSIONS, "Choose a folder");
        }
        if (!Files.exists(folder)) {
            return FieldStatus.error(InputField.SUBMISSIONS, "Folder not found");
        }
        if (!Files.isDirectory(folder)) {
            return FieldStatus.error(InputField.SUBMISSIONS, "Not a folder");
        }
        if (!Files.isReadable(folder)) {
            return FieldStatus.error(InputField.SUBMISSIONS, "Can't read this folder");
        }
        int count = countArchives(folder, pattern);
        if (count < 0) {
            return FieldStatus.error(InputField.SUBMISSIONS, "Can't read this folder");
        }
        if (count == 0) {
            return FieldStatus.error(InputField.SUBMISSIONS, "No student .zip files found");
        }
        return FieldStatus.ok(InputField.SUBMISSIONS, count + " submissions found");
    }

    // mirrors SubmissionProcessor: regular files directly inside, full-name match
    private static int countArchives(Path folder, Pattern pattern) {
        int count = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder)) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry)
                        && pattern.matcher(entry.getFileName().toString()).matches()) {
                    count++;
                }
            }
        } catch (IOException | RuntimeException exception) {
            return -1;
        }
        return count;
    }

    private static Pattern archivePattern(Path config) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(config)) {
            properties.load(input);
            return Pattern.compile(properties.getProperty(ARCHIVE_KEY, DEFAULT_ARCHIVE_REGEX).trim());
        } catch (IOException | RuntimeException exception) {
            return Pattern.compile(DEFAULT_ARCHIVE_REGEX);
        }
    }

    private static FieldStatus checkScoreSheet(Path file) {
        if (file == null) {
            return FieldStatus.error(InputField.SCORE_SHEET, "Choose a file");
        }
        if (!Files.exists(file)) {
            return FieldStatus.error(InputField.SCORE_SHEET, "File not found");
        }
        if (!Files.isRegularFile(file)) {
            return FieldStatus.error(InputField.SCORE_SHEET, "Not a file");
        }
        if (!Files.isReadable(file)) {
            return FieldStatus.error(InputField.SCORE_SHEET, "Can't read this file");
        }
        return FieldStatus.ok(InputField.SCORE_SHEET, "");
    }

    private static FieldStatus checkOutput(Path output) {
        if (output == null) {
            return FieldStatus.error(InputField.OUTPUT, "Choose an output folder");
        }
        if (Files.exists(output)) {
            if (!Files.isDirectory(output)) {
                return FieldStatus.error(InputField.OUTPUT, "Not a folder");
            }
            return writable(output, "");
        }
        Path ancestor = output.getParent();
        while (ancestor != null && !Files.exists(ancestor)) {
            ancestor = ancestor.getParent();
        }
        if (ancestor == null || !Files.isDirectory(ancestor)) {
            return FieldStatus.error(InputField.OUTPUT, "Can't write here");
        }
        return writable(ancestor, "Will be created");
    }

    private static FieldStatus writable(Path directory, String okMessage) {
        if (!Files.isWritable(directory)) {
            return FieldStatus.error(InputField.OUTPUT, "Can't write here");
        }
        return FieldStatus.ok(InputField.OUTPUT, okMessage);
    }

    private static FieldStatus checkOverlap(GuiInputs inputs) {
        Path output = inputs.output();
        Path submissions = inputs.submissions();
        if (output.startsWith(submissions)) {
            return FieldStatus.error(InputField.OUTPUT,
                    "Output can't be inside the submissions folder");
        }
        if (submissions.startsWith(output)) {
            return FieldStatus.error(InputField.OUTPUT,
                    "Output can't contain the submissions folder");
        }
        if (inputs.scoreSheet().startsWith(output)) {
            return FieldStatus.error(InputField.OUTPUT, "Output can't contain the score sheet");
        }
        return null;
    }
}
