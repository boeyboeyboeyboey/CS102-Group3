package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.StudentRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Matches the strong identity evidence found in a submission against the score sheet roster. */
final class RosterIdentityMatcher {
    private static final Set<String> WRAPPER_SOURCES = Set.of("wrapper username", "wrapper student id");

    private final AppConfig config;

    RosterIdentityMatcher(AppConfig config) {
        this.config = config;
    }

    Result match(List<IdentityEvidence.StrongEvidence> evidence, List<StudentRecord> roster) {
        Map<String, StudentRecord> matchedStudents = new LinkedHashMap<>();
        List<String> anomalies = new ArrayList<>();
        Set<String> otherEvidenceStudents = new HashSet<>();
        boolean unresolvedEvidence = false;
        boolean unmatchedWrapper = false;
        for (IdentityEvidence.StrongEvidence item : evidence) {
            List<StudentRecord> matches = findMatches(item, roster);
            if (matches.isEmpty()) {
                anomalies.add(unmatchedAnomaly(item));
                // a mistyped wrapper folder is only a warning when other evidence names one student
                if (isWrapper(item)) {
                    unmatchedWrapper = true;
                } else {
                    unresolvedEvidence = true;
                }
            } else if (matches.size() > 1) {
                anomalies.add(item.source() + " matches multiple score sheet students");
                unresolvedEvidence = true;
            } else {
                StudentRecord student = matches.get(0);
                matchedStudents.put(student.getId(), student);
                if (!isWrapper(item)) {
                    otherEvidenceStudents.add(student.getId());
                }
            }
        }
        if (unmatchedWrapper && otherEvidenceStudents.isEmpty()) {
            unresolvedEvidence = true;
        }
        return decide(matchedStudents, anomalies, unresolvedEvidence);
    }

    private Result decide(Map<String, StudentRecord> matchedStudents, List<String> anomalies,
                          boolean unresolvedEvidence) {
        if (matchedStudents.size() > 1) {
            anomalies.add("conflicting strong identity evidence points to different score sheet students: "
                    + conflictingUsernames(matchedStudents.values()));
            unresolvedEvidence = true;
        }
        if (matchedStudents.isEmpty()) {
            anomalies.add("no strong identity evidence matched the score sheet");
            return new Result(null, anomalies);
        }
        if (unresolvedEvidence) {
            return new Result(null, anomalies);
        }
        return new Result(matchedStudents.values().iterator().next(), anomalies);
    }

    private static boolean isWrapper(IdentityEvidence.StrongEvidence evidence) {
        return WRAPPER_SOURCES.contains(evidence.source());
    }

    // the wrapper folder name comes from the student's archive, so control characters are stripped before echoing it
    private static String unmatchedAnomaly(IdentityEvidence.StrongEvidence evidence) {
        String text = evidence.source() + " does not match a score sheet student";
        if (!isWrapper(evidence)) {
            return text;
        }
        return text + ": " + evidence.value().replaceAll("[\\p{Cc}\\p{Cf}\\u2028\\u2029]", " ").strip();
    }

    private String conflictingUsernames(Collection<StudentRecord> students) {
        List<String> usernames = new ArrayList<>();
        for (StudentRecord student : students) {
            usernames.add(student.getUsername());
        }
        return String.join(", ", usernames);
    }

    private List<StudentRecord> findMatches(IdentityEvidence.StrongEvidence evidence,
                                            List<StudentRecord> roster) {
        List<StudentRecord> matches = new ArrayList<>();
        for (StudentRecord student : roster) {
            boolean matchesStudent = false;
            if (evidence.type() == IdentityEvidence.EvidenceType.STUDENT_ID) {
                matchesStudent = RosterPrefix.strip(config, student.getId())
                        .equalsIgnoreCase(evidence.value());
            } else if (evidence.type() == IdentityEvidence.EvidenceType.EMAIL_OR_USERNAME
                    && evidence.value().contains("@")) {
                matchesStudent = student.getEmail().trim().equalsIgnoreCase(evidence.value());
            } else {
                matchesStudent = RosterPrefix.strip(config, student.getUsername())
                        .equalsIgnoreCase(evidence.value());
            }
            if (matchesStudent) {
                matches.add(student);
            }
        }
        return matches;
    }

    record Result(StudentRecord studentRecord, List<String> anomalies) {
        Result {
            anomalies = List.copyOf(anomalies);
        }
    }
}
