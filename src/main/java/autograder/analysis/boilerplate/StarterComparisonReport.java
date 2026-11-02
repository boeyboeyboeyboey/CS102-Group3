package autograder.analysis.boilerplate;

import autograder.config.BoilerplateConfig;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StarterComparisonReport {
    private StarterComparisonReport() {
    }

    public static Path generate(BatchResult batch, BoilerplateConfig config,
                                Path directory) throws IOException {
        Path reportDirectory = directory.resolve("starter-comparison");
        Files.createDirectory(reportDirectory);
        Path report = reportDirectory.resolve("index.html");
        Path details = Files.createTempFile(reportDirectory, "details-", ".tmp");
        try (Writer output = Files.newBufferedWriter(
                report, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            output.write(resource("comparison-head.html"));
            output.write("<p>Assessment: " + MethodDiffHtml.escape(
                    batch.getAssessment().getName()) + "</p>");
            StringBuilder rows = new StringBuilder();
            Set<Path> files = files(batch);
            int ordinal = 0;
            try (Writer detailOutput = Files.newBufferedWriter(details, StandardCharsets.UTF_8)) {
                for (var grade : batch.getGrades()) {
                    SubmissionResult submission = grade.getSubmission();
                    for (Path file : files) {
                        writeFile(detailOutput, submission, file, config,
                                "comparison-" + ++ordinal, rows);
                    }
                }
            }
            ComparisonOverview.write(output, rows);
            output.write("<main>");
            try (var input = Files.newBufferedReader(details, StandardCharsets.UTF_8)) {
                input.transferTo(output);
            }
            output.write("</main><script>" + resource("comparison.js")
                    + "</script></body></html>");
        } finally {
            Files.deleteIfExists(details);
        }
        return report;
    }

    private static Set<Path> files(BatchResult batch) {
        Set<Path> files = new LinkedHashSet<>();
        for (QuestionDefinition question
                : batch.getAssessment().getQuestions()) {
            files.add(Path.of(question.getFolder(),
                    question.getSourceFile()));
            for (String companion : question.getCompanionSourceFiles()) {
                files.add(Path.of(question.getFolder(), companion));
            }
        }
        return files;
    }

    private static void writeFile(Writer output, SubmissionResult submission,
            Path file, BoilerplateConfig config, String id, StringBuilder rows) throws IOException {
        String label = submission.getArchivePath().getFileName() + " / " + file;
        output.write("<section class=file id=\"" + id + "\"><h2 tabindex=\"-1\">"
                + MethodDiffHtml.escape(label) + "</h2>");
        List<MethodComparison.Difference> differences;
        try {
            if (submission.getWorkingRoot() == null) {
                throw new IOException("No working copy is available");
            }
            String starter = ComparisonSourceReader.read(
                    config.starterDirectory(), file, config);
            String answer = ComparisonSourceReader.read(
                    submission.getWorkingRoot(), file, config);
            differences = MethodComparison.compare(
                    starter, answer, config.maxTokens());
        } catch (IOException | RuntimeException exception) {
            ComparisonOverview.row(rows, id, label, List.of(),
                    "Unavailable: " + String.valueOf(exception.getMessage()));
            output.write("<article class=method data-status=UNAVAILABLE><h3>"
                    + "COMPARISON UNAVAILABLE</h3><p>"
                    + MethodDiffHtml.escape(
                            String.valueOf(exception.getMessage()))
                    + "</p></article></section>");
            return;
        }
        ComparisonOverview.row(rows, id, label, differences,
                differences.isEmpty() ? "No explicit methods" : "Compared");
        if (differences.isEmpty()) {
            output.write("<article class=method data-status=NO_METHODS>"
                    + "No explicit methods or constructors found in either "
                    + "file. This is not evidence that the whole files match."
                    + "</article>");
        }
        for (var difference : differences) {
            MethodDiffHtml.write(output, difference);
        }
        output.write("</section>");
    }

    private static String resource(String name) throws IOException {
        try (var stream = StarterComparisonReport.class.getResourceAsStream(
                "/autograder/boilerplate/" + name)) {
            if (stream == null) {
                throw new IOException("missing comparison report resource: "
                        + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
