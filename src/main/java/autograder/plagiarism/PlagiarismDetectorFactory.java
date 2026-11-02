package autograder.plagiarism;

import autograder.api.PlagiarismDetector;
import autograder.config.PlagiarismConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// builds the detector chosen by plagiarism.enabled and plagiarism.algorithms
public final class PlagiarismDetectorFactory {
    private PlagiarismDetectorFactory() {
    }

    public static PlagiarismDetector create(PlagiarismConfig config) {
        if (!config.enabled()) {
            return (submissions, assessment) -> List.of();
        }
        Map<String, PlagiarismDetector> detectors = new LinkedHashMap<>();
        if (config.uses(PlagiarismConfig.NGRAM)) {
            detectors.put(NGramPlagiarismDetector.ALGORITHM_NAME, new NGramPlagiarismDetector(config));
        }
        if (config.uses(PlagiarismConfig.JPLAG)) {
            detectors.put(JPlagPlagiarismDetector.ALGORITHM_NAME, new JPlagPlagiarismDetector(config));
        }
        return new CombinedPlagiarismDetector(detectors);
    }
}
