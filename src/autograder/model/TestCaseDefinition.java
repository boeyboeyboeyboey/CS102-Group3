package autograder.model;

import java.nio.file.Path;

/** A generated, independently executable test case. */
public final class TestCaseDefinition {
    private final int number;
    private final String className;
    private final Path sourceFile;

    public TestCaseDefinition(int number, String className, Path sourceFile) {
        this.number = number;
        this.className = className;
        this.sourceFile = sourceFile;
    }

    public int getNumber() {
        return number;
    }

    public String getClassName() {
        return className;
    }

    public Path getSourceFile() {
        return sourceFile;
    }
}
