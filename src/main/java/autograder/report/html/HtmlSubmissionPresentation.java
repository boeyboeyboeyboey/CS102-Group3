package autograder.report.html;

import autograder.model.StudentIdentity;

import java.nio.file.Path;

/** Chooses the names and path text shown for a submission, falling back when data is missing. */
final class HtmlSubmissionPresentation {
    static final String NOT_RECORDED = "Not recorded";

    private HtmlSubmissionPresentation() { }

    static String displayName(StudentIdentity identity, int ordinal) {
        if (identity != null) {
            String matchedName = rosterName(identity);
            if (hasText(matchedName)) {
                return matchedName;
            }
            if (hasText(identity.getName())) {
                return identity.getName();
            }
            String username = preferredUsername(identity);
            if (hasText(username)) {
                return username;
            }
        }
        return "Unidentified submission #" + ordinal;
    }

    static String pathText(Path path) {
        return path == null ? NOT_RECORDED : path.toString();
    }

    static String archiveName(Path path) {
        if (path == null || path.getFileName() == null) {
            return NOT_RECORDED;
        }
        return path.getFileName().toString();
    }

    static String preferredUsername(StudentIdentity identity) {
        if (identity == null) {
            return "";
        }
        if (identity.getStudentRecord() != null
                && hasText(identity.getStudentRecord().getUsername())) {
            return identity.getStudentRecord().getUsername();
        }
        return nullToEmpty(identity.getUsername());
    }

    static String rosterName(StudentIdentity identity) {
        if (identity == null || identity.getStudentRecord() == null) {
            return "";
        }
        String firstName = nullToEmpty(identity.getStudentRecord().getFirstName()).trim();
        String lastName = nullToEmpty(identity.getStudentRecord().getLastName()).trim();
        return (firstName + " " + lastName).trim();
    }

    static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
