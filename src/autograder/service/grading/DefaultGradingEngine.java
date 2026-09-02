package autograder.service.grading;

import autograder.config.AppConfig;
import autograder.model.Anomaly;
import autograder.model.NormalizedSubmission;
import autograder.model.ProcessResult;
import autograder.model.QuestionResult;
import autograder.model.QuestionSource;
import autograder.model.QuestionSpec;
import autograder.model.Severity;
import autograder.model.StudentGrade;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.service.api.GradingEngine;
import autograder.service.api.ProgressListener;
import autograder.service.api.TestExecutionStrategy;
import autograder.service.api.TestSuiteProvider;
import autograder.service.process.JavaCompilerService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Compiles and grades each configured question independently. */
public final class DefaultGradingEngine implements GradingEngine {
    private final AppConfig config;
    private final Path testersDirectory;
    private final Path batchWorkspace;
    private final JavaCompilerService compiler;
    private final TestSuiteProvider testSuiteProvider;
    private final TestExecutionStrategy executionStrategy;
    private final Duration compilationTimeout;
    private final Duration testTimeout;

    public DefaultGradingEngine(AppConfig config, Path batchWorkspace,
            JavaCompilerService compiler, TestSuiteProvider testSuiteProvider,
            TestExecutionStrategy executionStrategy) {
        this.config = config;
        this.testersDirectory = config.getPath("testers.directory");
        this.batchWorkspace = batchWorkspace;
        this.compiler = compiler;
        this.testSuiteProvider = testSuiteProvider;
        this.executionStrategy = executionStrategy;
        this.compilationTimeout = Duration.ofSeconds(
                config.getPositiveInt("compile.timeout.seconds"));
        this.testTimeout = Duration.ofSeconds(config.getPositiveInt("test.timeout.seconds"));
    }

    @Override
    public StudentGrade grade(NormalizedSubmission submission, ProgressListener listener)
            throws IOException, InterruptedException {
        Map<String, QuestionResult> results = new LinkedHashMap<>();
        Path studentWorkspace = batchWorkspace.resolve("grading")
                .resolve(safeName(submission.getStudent().getUsername()));
        for (QuestionSpec question : config.getQuestions()) {
            listener.onProgress("  " + question.getId() + ": preparing");
            QuestionSource source = submission.getQuestionSources().get(question.getId());
            if (source == null) {
                results.put(question.getId(), zeroResult(question));
                listener.onProgress("  " + question.getId() + ": 0.0/"
                        + format(question.getMaximumScore()) + " (source missing)");
                continue;
            }
            try {
                QuestionResult result = gradeQuestion(submission, source,
                        studentWorkspace.resolve(question.getId()), listener);
                results.put(question.getId(), result);
            } catch (IOException exception) {
                submission.addAnomaly(new Anomaly("QUESTION_SYSTEM_ERROR", Severity.ERROR,
                        question.getId() + " could not be graded: " + exception.getMessage()));
                results.put(question.getId(), zeroResult(question));
                listener.onProgress("  " + question.getId() + ": 0.0/"
                        + format(question.getMaximumScore()) + " (grading error)");
            }
        }
        return new StudentGrade(submission.getStudent(), results, submission.getAnomalies());
    }

    private QuestionResult gradeQuestion(NormalizedSubmission submission, QuestionSource source,
            Path questionWorkspace, ProgressListener listener)
            throws IOException, InterruptedException {
        QuestionSpec question = source.getSpecification();
        Path sourceDirectory = questionWorkspace.resolve("src");
        Path classesDirectory = questionWorkspace.resolve("classes");
        Files.createDirectories(sourceDirectory);
        Files.createDirectories(classesDirectory);

        stageStudentSources(source, sourceDirectory, submission);
        stageFixtures(question, sourceDirectory);
        stageTrustedSupport(question, classesDirectory);
        Path tester = testersDirectory.resolve(question.getTesterFileName());
        if (!Files.isRegularFile(tester)) {
            throw new IOException("Trusted tester is missing: " + tester);
        }
        List<TestCaseDefinition> testCases = testSuiteProvider.createCases(
                question, tester, sourceDirectory);
        if (Math.abs(testCases.size() - question.getMaximumScore()) > 0.0001) {
            submission.addAnomaly(new Anomaly("TEST_COUNT_MISMATCH", Severity.WARNING,
                    question.getId() + " has " + testCases.size()
                            + " cases but maximum score " + question.getMaximumScore()));
        }

        ProcessResult compilation = compiler.compile(sourceDirectory, classesDirectory,
                compilationTimeout);
        if (compilation.isTimedOut() || compilation.getExitCode() != 0) {
            String reason = compilation.isTimedOut() ? "compilation timed out"
                    : summarize(compilation.getStandardError(), "javac exited "
                            + compilation.getExitCode());
            submission.addAnomaly(new Anomaly("COMPILATION_FAILED", Severity.ERROR,
                    question.getId() + ": " + reason));
            List<TestCaseResult> failedCases = new ArrayList<>();
            for (TestCaseDefinition testCase : testCases) {
                failedCases.add(new TestCaseResult(testCase.getNumber(), 0.0,
                        TestCaseResult.Status.EXECUTION_ERROR, "Compilation failed"));
            }
            listener.onProgress("  " + question.getId() + ": 0.0/"
                    + format(question.getMaximumScore()) + " (compilation failed)");
            return new QuestionResult(question.getId(), 0.0,
                    question.getMaximumScore(), failedCases);
        }

        List<TestCaseResult> caseResults = new ArrayList<>();
        double score = 0.0;
        for (TestCaseDefinition testCase : testCases) {
            TestCaseResult result = executionStrategy.execute(testCase, classesDirectory,
                    sourceDirectory, testTimeout);
            caseResults.add(result);
            score += result.getScore();
            if (result.getStatus() == TestCaseResult.Status.TIMED_OUT) {
                submission.addAnomaly(new Anomaly("TEST_TIMEOUT", Severity.WARNING,
                        question.getId() + " case " + testCase.getNumber()
                                + " exceeded " + testTimeout.toSeconds() + " seconds"));
            } else if (result.getStatus() == TestCaseResult.Status.EXECUTION_ERROR
                    || result.getStatus() == TestCaseResult.Status.INVALID_OUTPUT) {
                submission.addAnomaly(new Anomaly("TEST_EXECUTION_ERROR", Severity.WARNING,
                        question.getId() + " case " + testCase.getNumber() + ": "
                                + result.getDetail()));
            }
        }
        score = Math.min(score, question.getMaximumScore());
        listener.onProgress("  " + question.getId() + ": " + format(score) + "/"
                + format(question.getMaximumScore()));
        return new QuestionResult(question.getId(), score,
                question.getMaximumScore(), caseResults);
    }

    private void stageStudentSources(QuestionSource source, Path destination,
            NormalizedSubmission submission) throws IOException {
        QuestionSpec question = source.getSpecification();
        String primaryDestinationName = source.isFallback()
                ? question.getFallbackSourceFileName() : question.getSourceFileName();
        Files.copy(source.getPrimarySource(), destination.resolve(primaryDestinationName),
                StandardCopyOption.REPLACE_EXISTING);
        for (String companionName : question.getCompanionSources()) {
            Optional<Path> companion = findSibling(source.getSourceDirectory(), companionName);
            if (companion.isPresent()) {
                Files.copy(companion.get(), destination.resolve(companionName),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                submission.addAnomaly(new Anomaly("MISSING_COMPANION_SOURCE", Severity.WARNING,
                        question.getId() + " companion source is missing: " + companionName));
            }
        }
        if (source.isFallback()) {
            String expectedClass = removeJavaSuffix(question.getSourceFileName());
            String parentClass = question.getFallbackParentClass();
            if (parentClass.isEmpty()) {
                throw new IOException("Fallback parent class is not configured for "
                        + question.getId());
            }
            String adapter = "public class " + expectedClass + " extends "
                    + parentClass + " { }" + System.lineSeparator();
            Files.writeString(destination.resolve(question.getSourceFileName()), adapter,
                    StandardCharsets.UTF_8);
        }
    }

    private void stageFixtures(QuestionSpec question, Path destination) throws IOException {
        for (String fixture : question.getFixtures()) {
            Path trustedFixture = testersDirectory.resolve(fixture).normalize();
            if (!trustedFixture.startsWith(testersDirectory.toAbsolutePath().normalize())
                    && !trustedFixture.startsWith(testersDirectory.normalize())) {
                throw new IOException("Fixture escapes the trusted tester directory: " + fixture);
            }
            if (!Files.isRegularFile(trustedFixture)) {
                throw new IOException("Trusted fixture is missing: " + trustedFixture);
            }
            Files.copy(trustedFixture, destination.resolve(Path.of(fixture).getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void stageTrustedSupport(QuestionSpec question, Path classesDirectory)
            throws IOException {
        for (String configuredPath : question.getSupportFiles()) {
            Path support = Path.of(configuredPath);
            if (!support.isAbsolute()) {
                support = config.getProjectRoot().resolve(support).normalize();
            }
            if (!Files.isRegularFile(support)) {
                throw new IOException("Trusted support file is missing: " + support);
            }
            Files.copy(support, classesDirectory.resolve(support.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Optional<Path> findSibling(Path directory, String fileName) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                    .findFirst();
        }
    }

    private QuestionResult zeroResult(QuestionSpec question) {
        return new QuestionResult(question.getId(), 0.0, question.getMaximumScore(), List.of());
    }

    private String removeJavaSuffix(String fileName) {
        return fileName.endsWith(".java")
                ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private String safeName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String summarize(String text, String fallback) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        int limit = Math.min(trimmed.length(), 500);
        return trimmed.substring(0, limit).replace('\n', ' ');
    }

    private String format(double score) {
        return String.format(Locale.ROOT, "%.1f", score);
    }
}
