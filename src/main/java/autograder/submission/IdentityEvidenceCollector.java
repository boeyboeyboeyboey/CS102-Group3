package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IdentityEvidenceCollector {
    private final AppConfig config;

    IdentityEvidenceCollector(AppConfig config) {
        this.config = config;
    }

    IdentityEvidence collect(Path archivePath, Path extractionPath, Path studentRoot,
                             AssessmentDefinition assessment) throws IOException {
        List<String> anomalies = new ArrayList<>();
        List<IdentityEvidence.ParsedSource> parsedSources = new ArrayList<>();
        List<IdentityEvidence.StrongEvidence> strongEvidence = new ArrayList<>();
        Set<String> names = new TreeSet<>();
        Set<String> emailIds = new TreeSet<>();
        Set<String> usernames = new TreeSet<>();

        addArchiveEvidence(archivePath, strongEvidence, anomalies);
        addWrapperEvidence(extractionPath, studentRoot, strongEvidence);
        validateAndReadSources(studentRoot, assessment, parsedSources, strongEvidence,
                names, emailIds, usernames, anomalies);

        return new IdentityEvidence(anomalies, strongEvidence, parsedSources,
                new ArrayList<>(names), new ArrayList<>(emailIds), new ArrayList<>(usernames));
    }

    private void addArchiveEvidence(Path archivePath,
                                    List<IdentityEvidence.StrongEvidence> evidence,
                                    List<String> anomalies) {
        String fileName = archivePath.getFileName().toString();
        Matcher matcher = config.getArchiveUsernamePattern().matcher(fileName);
        if (!matcher.matches()) {
            anomalies.add("archive name does not match the configured student username pattern");
            return;
        }
        String username = matcher.group(1).trim();
        if (username.isEmpty()) {
            anomalies.add("archive name contains an empty student username");
            return;
        }
        evidence.add(new IdentityEvidence.StrongEvidence(
                IdentityEvidence.EvidenceType.USERNAME, username, "archive"));
    }

    private void addWrapperEvidence(Path extractionPath, Path studentRoot,
                                    List<IdentityEvidence.StrongEvidence> evidence) {
        if (studentRoot == null || extractionPath.equals(studentRoot)) {
            return;
        }
        Path relativePath = extractionPath.relativize(studentRoot);
        for (Path segment : relativePath) {
            String value = segment.toString();
            if (config.getWrapperPlaceholderPattern().matcher(value).matches()) {
                continue;
            }
            if (config.getWrapperUsernamePattern().matcher(value).matches()) {
                evidence.add(new IdentityEvidence.StrongEvidence(
                        IdentityEvidence.EvidenceType.USERNAME, value, "wrapper username"));
            } else if (config.getWrapperStudentIdPattern().matcher(value).matches()) {
                evidence.add(new IdentityEvidence.StrongEvidence(
                        IdentityEvidence.EvidenceType.STUDENT_ID, value, "wrapper student id"));
            }
        }
    }

    private void validateAndReadSources(Path studentRoot, AssessmentDefinition assessment,
                                        List<IdentityEvidence.ParsedSource> parsedSources,
                                        List<IdentityEvidence.StrongEvidence> evidence,
                                        Set<String> names, Set<String> emailIds,
                                        Set<String> usernames, List<String> anomalies)
            throws IOException {
        for (QuestionDefinition question : assessment.getQuestions()) {
            if (studentRoot == null) {
                anomalies.add("required source cannot be checked because the student root is unresolved: "
                        + question.getId());
                continue;
            }
            Path questionDirectory = studentRoot.resolve(question.getFolder()).normalize();
            Path sourcePath = questionDirectory.resolve(question.getSourceFile()).normalize();
            if (!sourcePath.startsWith(studentRoot)
                    || !ExactEntries.hasDirectory(studentRoot, question.getFolder())
                    || !ExactEntries.hasFile(questionDirectory, question.getSourceFile())) {
                anomalies.add("required source is missing: " + question.getId() + " ("
                        + question.getFolder() + "/" + question.getSourceFile() + ")");
            } else {
                readSource(question.getId(), sourcePath, studentRoot, parsedSources,
                        evidence, names, emailIds, usernames);
            }
            for (String companion : question.getCompanionSourceFiles()) {
                Path companionPath = questionDirectory.resolve(companion).normalize();
                if (!companionPath.startsWith(studentRoot)
                        || !ExactEntries.hasDirectory(studentRoot, question.getFolder())
                        || !ExactEntries.hasFile(questionDirectory, companion)) {
                    anomalies.add("required companion source is missing: " + question.getId()
                            + " (" + question.getFolder() + "/" + companion + ")");
                } else {
                    readSource(question.getId(), companionPath, studentRoot, parsedSources,
                            evidence, names, emailIds, usernames);
                }
            }
        }
    }

    private void readSource(String questionId, Path sourcePath, Path studentRoot,
                            List<IdentityEvidence.ParsedSource> parsedSources,
                            List<IdentityEvidence.StrongEvidence> evidence,
                            Set<String> names, Set<String> emailIds, Set<String> usernames)
            throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            String line;
            while (lines.size() < config.getIdentityHeaderMaxLines()
                    && (line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        IdentityEvidence.HeaderField name = readField(lines, config.getIdentityNamePattern());
        IdentityEvidence.HeaderField emailId = readField(lines, config.getIdentityEmailPattern());
        IdentityEvidence.HeaderField username = readField(lines, config.getIdentityUsernamePattern());
        IdentityEvidence.ParsedSource parsedSource = new IdentityEvidence.ParsedSource(
                questionId, sourcePath, studentRoot.relativize(sourcePath), name, emailId, username);
        parsedSources.add(parsedSource);

        for (String value : name.nonemptyValues()) {
            names.add(value);
        }
        for (String value : emailId.nonemptyValues()) {
            emailIds.add(value);
            evidence.add(new IdentityEvidence.StrongEvidence(
                    IdentityEvidence.EvidenceType.EMAIL_OR_USERNAME, value,
                    "header email id in " + sourcePath.getFileName()));
        }
        for (String value : username.nonemptyValues()) {
            usernames.add(value);
            evidence.add(new IdentityEvidence.StrongEvidence(
                    IdentityEvidence.EvidenceType.USERNAME, value,
                    "header username in " + sourcePath.getFileName()));
        }
    }

    private IdentityEvidence.HeaderField readField(List<String> lines, Pattern pattern) {
        List<IdentityEvidence.HeaderLine> fieldLines = new ArrayList<>();
        int lineLimit = Math.min(lines.size(), config.getIdentityHeaderMaxLines());
        for (int lineIndex = 0; lineIndex < lineLimit; lineIndex++) {
            Matcher matcher = pattern.matcher(lines.get(lineIndex));
            if (matcher.matches()) {
                fieldLines.add(new IdentityEvidence.HeaderLine(matcher.group(1).trim()));
            }
        }
        return new IdentityEvidence.HeaderField(fieldLines);
    }
}
