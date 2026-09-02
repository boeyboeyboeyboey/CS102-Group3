package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A student row loaded from the institutional scoresheet. */
public final class StudentRecord {
    private final String organizationId;
    private final String username;
    private final String displayName;
    private final String email;
    private final List<String> originalRow;

    public StudentRecord(String organizationId, String username, String displayName,
            String email, List<String> originalRow) {
        this.organizationId = stripMarker(organizationId);
        this.username = stripMarker(username);
        this.displayName = displayName;
        this.email = email;
        this.originalRow = Collections.unmodifiableList(new ArrayList<>(originalRow));
    }

    private static String stripMarker(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getOriginalRow() {
        return originalRow;
    }
}
