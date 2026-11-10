package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Name, email and username details recovered for one submission. */
public final class StudentIdentity {
    private final String name;
    private final String emailId;
    private final String username;
    private final List<String> headerNames;
    private final List<String> headerEmailIds;
    private final List<String> headerUsernames;
    private final StudentRecord studentRecord;
    private final String source;
    private final List<String> strongEvidence;

    public StudentIdentity(String name, String emailId, String username,
                           List<String> headerNames, List<String> headerEmailIds,
                           List<String> headerUsernames,
                           StudentRecord studentRecord, String source,
                           List<String> strongEvidence) {
        this.name = name;
        this.emailId = emailId;
        this.username = username;
        this.headerNames = Collections.unmodifiableList(new ArrayList<>(headerNames));
        this.headerEmailIds = Collections.unmodifiableList(new ArrayList<>(headerEmailIds));
        this.headerUsernames = Collections.unmodifiableList(new ArrayList<>(headerUsernames));
        this.studentRecord = studentRecord;
        this.source = source;
        this.strongEvidence = Collections.unmodifiableList(new ArrayList<>(strongEvidence));
    }

    public String getName() {
        return name;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getHeaderNames() {
        return headerNames;
    }

    public List<String> getHeaderEmailIds() {
        return headerEmailIds;
    }

    public List<String> getHeaderUsernames() {
        return headerUsernames;
    }

    public StudentRecord getStudentRecord() {
        return studentRecord;
    }

    public String getSource() {
        return source;
    }

    public List<String> getStrongEvidence() {
        return strongEvidence;
    }

    public String getLabel() {
        if (studentRecord != null) {
            return studentRecord.getUsername() + " [" + source + "]";
        }
        if (!username.isBlank()) {
            return username + " [unresolved: " + source + "]";
        }
        if (!emailId.isBlank()) {
            return emailId + " [unresolved: " + source + "]";
        }
        if (!name.isBlank()) {
            return name + " [unresolved: " + source + "]";
        }
        return "archive-derived fallback [" + source + "]";
    }
}
