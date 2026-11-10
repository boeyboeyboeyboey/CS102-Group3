package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.StudentIdentity;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Works out which roster student an archive belongs to and applies the header corrections that follow. */
public final class IdentityResolver {
    private final IdentityEvidenceCollector evidenceCollector;
    private final RosterIdentityMatcher rosterMatcher;
    private final HeaderCorrectionPlanner correctionPlanner;
    private final HeaderCorrectionApplier correctionApplier;

    public IdentityResolver(AppConfig config) {
        this.evidenceCollector = new IdentityEvidenceCollector(config);
        this.rosterMatcher = new RosterIdentityMatcher(config);
        this.correctionPlanner = new HeaderCorrectionPlanner(config);
        this.correctionApplier = new HeaderCorrectionApplier(config);
    }

    public Resolution resolve(Path archivePath, Path extractionPath, Path studentRoot,
                              AssessmentDefinition assessment, List<StudentRecord> roster)
            throws IOException {
        IdentityEvidence evidence = evidenceCollector.collect(archivePath, extractionPath,
                studentRoot, assessment);
        RosterIdentityMatcher.Result match = rosterMatcher.match(evidence.strongEvidence(), roster);
        List<String> anomalies = new ArrayList<>(evidence.anomalies());
        anomalies.addAll(match.anomalies());

        HeaderCorrectionPlanner.Plan correctionPlan = correctionPlanner.plan(
                evidence.parsedSources(), match.studentRecord());
        anomalies.addAll(correctionPlan.anomalies());

        String name = onlyValue(evidence.names());
        String emailId = onlyValue(evidence.emailIds());
        String username = onlyValue(evidence.usernames());
        if (match.studentRecord() == null && name.isEmpty() && emailId.isEmpty()
                && username.isEmpty()) {
            name = "archive-derived fallback: " + archiveStem(archivePath);
        }
        Set<String> evidenceSources = new LinkedHashSet<>();
        List<String> evidenceLabels = new ArrayList<>();
        for (IdentityEvidence.StrongEvidence item : evidence.strongEvidence()) {
            evidenceSources.add(item.source());
            evidenceLabels.add(item.source() + ": " + item.value());
        }
        String source = match.studentRecord() == null
                ? "unresolved strong evidence" : String.join("+", evidenceSources);
        StudentIdentity identity = new StudentIdentity(name, emailId, username,
                evidence.names(), evidence.emailIds(), evidence.usernames(),
                match.studentRecord(), source, evidenceLabels);
        return new Resolution(identity, anomalies, correctionPlan.corrections(),
                evidence.parsedSources().size());
    }

    public List<SubmissionCorrection> applyCorrections(Path workingRoot,
                                                        List<SubmissionCorrection> corrections)
            throws IOException {
        return correctionApplier.apply(workingRoot, corrections);
    }

    private String onlyValue(List<String> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        return "";
    }

    private String archiveStem(Path archivePath) {
        String fileName = archivePath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            return fileName.substring(0, extensionIndex);
        }
        return fileName;
    }

    /** The resolved identity of one archive with the anomalies and corrections found on the way. */
    public static final class Resolution {
        private final StudentIdentity identity;
        private final List<String> anomalies;
        private final List<SubmissionCorrection> corrections;
        private final int validatedSourceCount;

        private Resolution(StudentIdentity identity, List<String> anomalies,
                           List<SubmissionCorrection> corrections, int validatedSourceCount) {
            this.identity = identity;
            this.anomalies = List.copyOf(anomalies);
            this.corrections = List.copyOf(corrections);
            this.validatedSourceCount = validatedSourceCount;
        }

        public StudentIdentity getIdentity() {
            return identity;
        }

        public List<String> getAnomalies() {
            return anomalies;
        }

        public List<SubmissionCorrection> getCorrections() {
            return corrections;
        }

        public int getValidatedSourceCount() {
            return validatedSourceCount;
        }
    }
}
