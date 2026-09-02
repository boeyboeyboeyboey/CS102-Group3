package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Configurable file and scoring rules for one assessment question. */
public final class QuestionSpec {
    private final String id;
    private final String folderName;
    private final String sourceFileName;
    private final String fallbackSourceFileName;
    private final String fallbackParentClass;
    private final String testerFileName;
    private final double maximumScore;
    private final List<String> fixtures;
    private final List<String> companionSources;
    private final List<String> supportFiles;

    public QuestionSpec(String id, String folderName, String sourceFileName,
            String fallbackSourceFileName, String fallbackParentClass,
            String testerFileName, double maximumScore, List<String> fixtures,
            List<String> companionSources, List<String> supportFiles) {
        this.id = id;
        this.folderName = folderName;
        this.sourceFileName = sourceFileName;
        this.fallbackSourceFileName = fallbackSourceFileName;
        this.fallbackParentClass = fallbackParentClass;
        this.testerFileName = testerFileName;
        this.maximumScore = maximumScore;
        this.fixtures = Collections.unmodifiableList(new ArrayList<>(fixtures));
        this.companionSources = Collections.unmodifiableList(new ArrayList<>(companionSources));
        this.supportFiles = Collections.unmodifiableList(new ArrayList<>(supportFiles));
    }

    public String getId() {
        return id;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getFallbackSourceFileName() {
        return fallbackSourceFileName;
    }

    public String getFallbackParentClass() {
        return fallbackParentClass;
    }

    public String getTesterFileName() {
        return testerFileName;
    }

    public double getMaximumScore() {
        return maximumScore;
    }

    public List<String> getFixtures() {
        return fixtures;
    }

    public List<String> getCompanionSources() {
        return companionSources;
    }

    public List<String> getSupportFiles() {
        return supportFiles;
    }
}
