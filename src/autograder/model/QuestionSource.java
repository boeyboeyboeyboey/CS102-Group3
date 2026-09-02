package autograder.model;

import java.nio.file.Path;

/** The selected student source location for one question. */
public final class QuestionSource {
    private final QuestionSpec specification;
    private final Path primarySource;
    private final boolean fallback;

    public QuestionSource(QuestionSpec specification, Path primarySource, boolean fallback) {
        this.specification = specification;
        this.primarySource = primarySource;
        this.fallback = fallback;
    }

    public QuestionSpec getSpecification() {
        return specification;
    }

    public Path getPrimarySource() {
        return primarySource;
    }

    public Path getSourceDirectory() {
        return primarySource.getParent();
    }

    public boolean isFallback() {
        return fallback;
    }
}
