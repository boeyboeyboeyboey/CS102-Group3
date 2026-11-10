package autograder.model;

/** One student row from the score sheet. */
public final class StudentRecord {
    private final String id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;

    public StudentRecord(String id, String username, String firstName, String lastName, String email) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
