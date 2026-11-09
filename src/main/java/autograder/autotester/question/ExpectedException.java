package autograder.autotester.question;

import java.io.Serializable;

public record ExpectedException(String className) implements Serializable {
    public ExpectedException {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("expected exception class name is required");
        }
    }
}
