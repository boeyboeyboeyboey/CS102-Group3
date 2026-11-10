package autograder.model;

import java.nio.file.Path;

/** A correction applied to a submission file while preparing it, with the original and corrected value. */
public final class SubmissionCorrection {
    private final String type;
    private final Path relativePath;
    private final String field;
    private final String originalValue;
    private final String correctedValue;
    private final boolean headerAdded;

    public SubmissionCorrection(String type, Path relativePath, String field,
                                String originalValue, String correctedValue,
                                boolean headerAdded) {
        this.type = type;
        this.relativePath = relativePath;
        this.field = field;
        this.originalValue = originalValue;
        this.correctedValue = correctedValue;
        this.headerAdded = headerAdded;
    }

    public String getType() {
        return type;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public String getField() {
        return field;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public String getCorrectedValue() {
        return correctedValue;
    }

    public boolean isHeaderAdded() {
        return headerAdded;
    }

    @Override
    public String toString() {
        return type + " | " + relativePath + " | " + field + " | " + originalValue
                + " -> " + correctedValue;
    }
}
