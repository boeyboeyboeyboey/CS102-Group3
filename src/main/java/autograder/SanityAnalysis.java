package autograder;

import autograder.analysis.sanity.SanityChecker;
import autograder.analysis.sanity.SanityCsvWriter;
import autograder.analysis.sanity.SanityFinding;
import autograder.config.SanityConfig;
import autograder.grading.GradingRun;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

final class SanityAnalysis {
    static final String REPORT = "sanity-checks.csv";

    private SanityAnalysis() {
    }

    record Result(List<SanityFinding> findings, int exitCode) {
    }

    static Result analyze(GradingRun run, SanityConfig config) {
        return analyze(run, config, System.err::println);
    }

    static Result analyze(GradingRun run, SanityConfig config, Consumer<String> problems) {
        if (!config.enabled()) {
            return new Result(List.of(), 0);
        }
        List<SanityFinding> findings;
        try {
            findings = new SanityChecker().check(run, config);
        } catch (RuntimeException exception) {
            // no csv or "none" line, so a crashed check never reads as a clean pass
            problems.accept("could not run sanity checks: " + exception.getMessage());
            return new Result(List.of(), 1);
        }
        Path report = run.directory().resolve(REPORT);
        int exitCode = write(report, findings, problems);
        print(findings, exitCode == 0 ? report.toString() : "not saved");
        return new Result(findings, exitCode);
    }

    private static int write(Path report, List<SanityFinding> findings, Consumer<String> problems) {
        try {
            Files.writeString(report, SanityCsvWriter.format(findings),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException | RuntimeException exception) {
            problems.accept("could not write sanity checks " + report + ": " + exception.getMessage());
            return 1;
        }
        return 0;
    }

    // findings are still shown when the csv could not be saved
    private static void print(List<SanityFinding> findings, String report) {
        if (findings.isEmpty()) {
            System.out.println("Sanity checks: none");
            return;
        }
        System.out.println("Sanity checks: " + findings.size() + " possible assessment problems (" + report + ")");
        for (SanityFinding finding : findings) {
            String where = finding.getCaseId().isEmpty() ? finding.getQuestionId() : finding.getCaseId();
            System.out.println("  " + finding.getCheck() + " | " + where + " | " + finding.getMessage());
        }
    }
}
