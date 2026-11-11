package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.SubmissionCorrection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies planned header corrections to the source files of a working copy. */
final class HeaderCorrectionApplier {
    private final AppConfig config;

    HeaderCorrectionApplier(AppConfig config) {
        this.config = config;
    }

    List<SubmissionCorrection> apply(Path workingRoot, List<SubmissionCorrection> corrections)
            throws IOException {
        Map<Path, List<SubmissionCorrection>> correctionsByFile = new LinkedHashMap<>();
        for (SubmissionCorrection correction : corrections) {
            if (correction.getType().equals("header")) {
                correctionsByFile.computeIfAbsent(correction.getRelativePath(),
                        key -> new ArrayList<>()).add(correction);
            }
        }

        List<SubmissionCorrection> applied = new ArrayList<>();
        for (Map.Entry<Path, List<SubmissionCorrection>> entry : correctionsByFile.entrySet()) {
            applyFile(workingRoot, entry.getKey(), entry.getValue(), applied);
        }
        return applied;
    }

    private void applyFile(Path workingRoot, Path relativePath,
                           List<SubmissionCorrection> corrections,
                           List<SubmissionCorrection> applied) throws IOException {
        Path sourcePath = workingRoot.resolve(relativePath).normalize();
        if (!sourcePath.startsWith(workingRoot)) {
            throw new IOException("header correction path escapes the working directory");
        }
        List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
        List<String> addedLines = new ArrayList<>();
        for (SubmissionCorrection correction : corrections) {
            applyCorrection(lines, correction, addedLines, applied);
        }
        if (!addedLines.isEmpty()) {
            List<String> headerBlock = new ArrayList<>();
            headerBlock.add("/*");
            headerBlock.addAll(addedLines);
            headerBlock.add(" */");
            lines.addAll(0, headerBlock);
        }
        Files.write(sourcePath, lines, StandardCharsets.UTF_8);
    }

    private void applyCorrection(List<String> lines, SubmissionCorrection correction,
                                 List<String> addedLines,
                                 List<SubmissionCorrection> applied) throws IOException {
        Pattern pattern = patternForField(correction.getField());
        boolean found = false;
        boolean filled = false;
        int lineLimit = Math.min(lines.size(), config.getIdentityHeaderMaxLines());
        for (int lineIndex = 0; lineIndex < lineLimit; lineIndex++) {
            Matcher matcher = pattern.matcher(lines.get(lineIndex));
            if (matcher.matches()) {
                found = true;
                if (matcher.group(1).trim().isEmpty()) {
                    String line = lines.get(lineIndex);
                    String beforeValue = line.substring(0, matcher.start(1));
                    if (!beforeValue.endsWith(" ")) {
                        beforeValue = beforeValue + " ";
                    }
                    lines.set(lineIndex, beforeValue + correction.getCorrectedValue()
                            + line.substring(matcher.end(1)));
                    filled = true;
                }
            }
        }
        if (!found && correction.isHeaderAdded()) {
            addedLines.add(" * " + headerLabelForField(correction.getField()) + ": "
                    + correction.getCorrectedValue());
            applied.add(correction);
        } else if (filled) {
            applied.add(correction);
        }
    }

    private Pattern patternForField(String field) throws IOException {
        if (field.equals("name")) {
            return config.getIdentityNamePattern();
        }
        if (field.equals("email id")) {
            return config.getIdentityEmailPattern();
        }
        if (field.equals("username")) {
            return config.getIdentityUsernamePattern();
        }
        throw new IOException("unknown header correction field: " + field);
    }

    private String headerLabelForField(String field) throws IOException {
        if (field.equals("name")) {
            return config.getIdentityNameLabel();
        }
        if (field.equals("email id")) {
            return config.getIdentityEmailLabel();
        }
        if (field.equals("username")) {
            return config.getIdentityUsernameLabel();
        }
        throw new IOException("unknown header correction field: " + field);
    }
}
