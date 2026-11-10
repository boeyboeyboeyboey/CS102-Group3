package autograder.model;

import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One question of the assessment: where its files are, how it is compiled and its maximum score. */
public final class QuestionDefinition {
    private final String id;
    private final String folder;
    private final String sourceFile;
    private final Path testerFile;
    private final int caseCount;
    private final double pointsPerCase;
    private final List<Path> supportFiles;
    private final List<Path> fixtures;
    private final List<String> companionSourceFiles;

    public QuestionDefinition(String id, String folder, String sourceFile, Path testerFile,
                              int caseCount, double pointsPerCase, List<Path> supportFiles,
                              List<Path> fixtures, List<String> companionSourceFiles) {
        this.id = id;
        this.folder = folder;
        this.sourceFile = sourceFile;
        this.testerFile = testerFile;
        this.caseCount = caseCount;
        this.pointsPerCase = pointsPerCase;
        this.supportFiles = Collections.unmodifiableList(new ArrayList<>(supportFiles));
        this.fixtures = Collections.unmodifiableList(new ArrayList<>(fixtures));
        this.companionSourceFiles = Collections.unmodifiableList(
                new ArrayList<>(companionSourceFiles));
    }

    public String getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public Path getTesterFile() {
        return testerFile;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public double getPointsPerCase() {
        return pointsPerCase;
    }

    public List<Path> getSupportFiles() {
        return supportFiles;
    }

    public List<Path> getFixtures() {
        return fixtures;
    }

    public List<String> getCompanionSourceFiles() {
        return companionSourceFiles;
    }

    public double getMaxScore() {
        return getMaxScoreDecimal().doubleValue();
    }

    public BigDecimal getMaxScoreDecimal() {
        return BigDecimal.valueOf(pointsPerCase).multiply(BigDecimal.valueOf(caseCount));
    }
}
