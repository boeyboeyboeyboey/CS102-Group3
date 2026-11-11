package autograder.submission;

import java.nio.file.Path;
import java.util.List;

/** Immutable result of identity evidence collection: anomalies, strong evidence, parsed source headers and the names, email ids and usernames found. */
final class IdentityEvidence {
    private final List<String> anomalies;
    private final List<StrongEvidence> strongEvidence;
    private final List<ParsedSource> parsedSources;
    private final List<String> names;
    private final List<String> emailIds;
    private final List<String> usernames;

    IdentityEvidence(List<String> anomalies, List<StrongEvidence> strongEvidence,
                     List<ParsedSource> parsedSources, List<String> names,
                     List<String> emailIds, List<String> usernames) {
        this.anomalies = List.copyOf(anomalies);
        this.strongEvidence = List.copyOf(strongEvidence);
        this.parsedSources = List.copyOf(parsedSources);
        this.names = List.copyOf(names);
        this.emailIds = List.copyOf(emailIds);
        this.usernames = List.copyOf(usernames);
    }

    List<String> anomalies() {
        return anomalies;
    }

    List<StrongEvidence> strongEvidence() {
        return strongEvidence;
    }

    List<ParsedSource> parsedSources() {
        return parsedSources;
    }

    List<String> names() {
        return names;
    }

    List<String> emailIds() {
        return emailIds;
    }

    List<String> usernames() {
        return usernames;
    }

    enum EvidenceType {
        USERNAME,
        STUDENT_ID,
        EMAIL_OR_USERNAME
    }

    record StrongEvidence(EvidenceType type, String value, String source) {
        StrongEvidence {
            value = value.trim();
        }
    }

    record HeaderLine(String value) {
    }

    record HeaderField(List<HeaderLine> lines) {
        HeaderField {
            lines = List.copyOf(lines);
        }

        List<String> nonemptyValues() {
            return lines.stream().map(HeaderLine::value)
                    .filter(value -> !value.isBlank()).toList();
        }
    }

    record ParsedSource(String questionId, Path sourcePath, Path relativePath,
                        HeaderField name, HeaderField emailId, HeaderField username) {
    }
}
