package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class HeaderCorrectionPlanner {
    private final AppConfig config;

    HeaderCorrectionPlanner(AppConfig config) {
        this.config = config;
    }

    Plan plan(List<IdentityEvidence.ParsedSource> sources, StudentRecord student) {
        List<String> anomalies = new ArrayList<>();
        List<SubmissionCorrection> corrections = new ArrayList<>();
        if (student == null) {
            addUnresolvedHeaderAnomalies(sources, anomalies);
        } else {
            prepareHeaderCorrections(sources, student, corrections, anomalies);
        }
        return new Plan(corrections, anomalies);
    }

    private void prepareHeaderCorrections(List<IdentityEvidence.ParsedSource> sources,
                                          StudentRecord student,
                                          List<SubmissionCorrection> corrections,
                                          List<String> anomalies) {
        String expectedName = rosterName(student);
        String expectedEmail = student.getEmail().trim();
        String expectedUsername = RosterPrefix.strip(config, student.getUsername());
        for (IdentityEvidence.ParsedSource source : sources) {
            if (expectedName.isEmpty()) {
                anomalies.add(source.questionId() + ": roster name is empty");
            } else {
                prepareFieldCorrection(source, "name", source.name(), expectedName,
                        true, anomalies, corrections);
            }
            prepareFieldCorrection(source, "email id", source.emailId(), expectedEmail,
                    true, anomalies, corrections);
            prepareFieldCorrection(source, "username", source.username(), expectedUsername,
                    false, anomalies, corrections);
        }
    }

    private void prepareFieldCorrection(IdentityEvidence.ParsedSource source, String fieldName,
                                        IdentityEvidence.HeaderField field, String expectedValue,
                                        boolean addMissingHeader, List<String> anomalies,
                                        List<SubmissionCorrection> corrections) {
        boolean hasNonemptyValue = false;
        for (IdentityEvidence.HeaderLine line : field.lines()) {
            if (!line.value().isBlank()) {
                hasNonemptyValue = true;
                if (fieldName.equals("name")
                        && !canonicalName(line.value()).equals(canonicalName(expectedValue))) {
                    anomalies.add(source.questionId() + ": name header does not corroborate the roster");
                }
            }
        }

        if (field.lines().isEmpty()) {
            if (addMissingHeader) {
                corrections.add(new SubmissionCorrection("header",
                        source.relativePath(), fieldName, "", expectedValue, true));
            }
            return;
        }
        if (hasNonemptyValue) {
            for (IdentityEvidence.HeaderLine line : field.lines()) {
                if (line.value().isBlank()) {
                    corrections.add(new SubmissionCorrection("header",
                            source.relativePath(), fieldName, "",
                            expectedValue, false));
                    break;
                }
            }
            return;
        }
        corrections.add(new SubmissionCorrection("header",
                source.relativePath(), fieldName, "", expectedValue, false));
    }

    private void addUnresolvedHeaderAnomalies(List<IdentityEvidence.ParsedSource> sources,
                                              List<String> anomalies) {
        for (IdentityEvidence.ParsedSource source : sources) {
            if (source.name().lines().isEmpty()) {
                anomalies.add(source.questionId() + ": name header is missing");
            } else if (source.name().nonemptyValues().isEmpty()) {
                anomalies.add(source.questionId() + ": name header is blank");
            }
            if (source.emailId().lines().isEmpty()) {
                anomalies.add(source.questionId() + ": email id header is missing");
            } else if (source.emailId().nonemptyValues().isEmpty()) {
                anomalies.add(source.questionId() + ": email id header is blank");
            }
        }
    }

    private String rosterName(StudentRecord student) {
        String firstName = student.getFirstName().trim();
        String lastName = student.getLastName().trim();
        if (lastName.isEmpty() || lastName.equals(config.getRosterLastNamePlaceholder())) {
            return firstName;
        }
        if (firstName.isEmpty()) {
            return lastName;
        }
        return firstName + " " + lastName;
    }

    private String canonicalName(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        for (int index = 0; index < lower.length(); index++) {
            char character = lower.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                normalized.append(character);
            }
        }
        return normalized.toString();
    }

    record Plan(List<SubmissionCorrection> corrections, List<String> anomalies) {
        Plan {
            corrections = List.copyOf(corrections);
            anomalies = List.copyOf(anomalies);
        }
    }
}
