package autograder.grading;

import autograder.api.PlagiarismDetector;
import autograder.api.ProgressListener;
import autograder.api.TestCaseProvider;
import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.execution.CaseExecutor;
import autograder.execution.ProcessResult;
import autograder.execution.ProcessRunner;
import autograder.execution.StudentCompiler;
import autograder.model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class GradingPipeline {
    public static final String SIMILARITY_REPORT = "similarity.csv";

    private final AppConfig app;
    private final ExecutionConfig execution;
    private final List<TestCaseDefinition> cases;
    private final PlagiarismDetector plagiarismDetector;
    private final ProgressListener progressListener;
    private boolean progressEnabled = true;

    public GradingPipeline(AppConfig app, ExecutionConfig execution) throws IOException {
        this(app, execution, (submissions, assessment) -> List.of());
    }

    public GradingPipeline(AppConfig app, ExecutionConfig execution,
                           PlagiarismDetector plagiarismDetector) throws IOException {
        this(app, execution, plagiarismDetector, null);
    }

    // a null listener keeps the console-only behaviour of the other constructors
    public GradingPipeline(AppConfig app, ExecutionConfig execution,
                           PlagiarismDetector plagiarismDetector,
                           ProgressListener progressListener) throws IOException {
        this.app = app;
        this.execution = execution;
        this.plagiarismDetector = plagiarismDetector;
        this.progressListener = progressListener;
        TestCaseProvider fixedProvider = new InstructorTestCaseProvider();
        TestCaseProvider dynamicProvider = new DynamicTestCaseProvider();
        TestCaseProvider combinedProvider = new CombinedTestCaseProvider(fixedProvider, dynamicProvider);
        this.cases = combinedProvider.load(app.getAssessment());
    }

    public void validateEnvironment() throws IOException, InterruptedException {
        for (String executable : List.of(execution.javaExecutable(), execution.javacExecutable())) {
            ProcessResult result = ProcessRunner.run(List.of(executable, "-version"),
                    Path.of(".").toAbsolutePath(), execution.compileTimeoutMillis(),
                    execution.maxOutputBytes());
            if (result.timedOut() || result.exitCode() != 0) {
                throw new IOException("could not start configured Java tool: " + executable
                        + " " + result.stderr());
            }
        }
        for (QuestionDefinition question : app.getAssessment().getQuestions()) {
            List<Path> required = new ArrayList<>(question.getSupportFiles());
            required.addAll(question.getFixtures());
            required.add(question.getTesterFile());
            for (Path path : required) {
                if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                    throw new IOException("missing/unreadable instructor resource: " + path);
                }
            }
        }
    }

    public GradingRun grade(List<SubmissionResult> submissions) throws IOException, InterruptedException {
        checkCancelled();
        Files.createDirectories(execution.directory());
        Path run = Files.createTempDirectory(execution.directory(), "run-");
        Map<String, Integer> identities = identityCounts(submissions);
        Map<SubmissionResult, String> withheld = new LinkedHashMap<>();
        List<StudentGrade> grades = new ArrayList<>();
        int total = submissions.size();
        AtomicInteger finished = new AtomicInteger();
        List<GradedSubmission> graded = new SubmissionScheduler(execution.workers()).run(total, index -> {
            SubmissionResult submission = submissions.get(index);
            String item = submission.getArchivePath().getFileName().toString();
            notifyCount(item, finished, 0, total);
            checkCancelled();
            Map<SubmissionResult, String> local = new LinkedHashMap<>();
            StudentGrade grade = gradeSubmission(submission, identities, run, index + 1, total, local);
            checkCancelled();
            notifyCount(item, finished, 1, total);
            checkCancelled();
            return new GradedSubmission(grade, local);
        });
        checkCancelled();
        for (GradedSubmission item : graded) {
            grades.add(item.grade());
            withheld.putAll(item.withheld());
        }
        BatchResult batch = new BatchResult(app.getAssessment(), grades, List.of(),
                compareSimilarity(submissions));
        GradingRun result = new GradingRun(batch, run, withheld);
        checkCancelled();
        notifyProgress("reporting", "Writing score and diagnostic CSV files", 0, 2);
        checkCancelled();
        new GradingDiagnosticsWriter(app, execution).write(result);
        notifyProgress("reporting", "Score and diagnostic CSV files ready", 1, 2);
        return result;
    }

    private static void checkCancelled() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("grading cancelled");
        }
    }

    private record GradedSubmission(StudentGrade grade, Map<SubmissionResult, String> withheld) {
    }

    // the count is read under the listener lock so parallel events never go backwards
    private synchronized void notifyCount(String item, AtomicInteger finished, int step, int total) {
        notifyProgress("grading", item, finished.addAndGet(step), total);
    }

    // a failing listener must not stop grading and is disabled for the rest of the run
    private synchronized void notifyProgress(String stage, String item, int completed, int total) {
        if (progressListener == null || !progressEnabled) {
            return;
        }
        try {
            progressListener.onProgress(stage, item, completed, total);
        } catch (RuntimeException exception) {
            System.err.println("progress update failed: " + exception.getMessage());
            progressEnabled = false;
        }
    }

    // similarity is advisory, so a detector failure must not block score export
    private List<SimilarityFinding> compareSimilarity(List<SubmissionResult> submissions) {
        try {
            return plagiarismDetector.compare(submissions, app.getAssessment());
        } catch (IOException | RuntimeException exception) {
            System.err.println("similarity check skipped: " + exception.getMessage());
            return List.of();
        }
    }

    private Map<String, Integer> identityCounts(List<SubmissionResult> submissions) {
        Map<String, Integer> identities = new HashMap<>();
        for (SubmissionResult submission : submissions) {
            StudentRecord student = submission.getIdentity().getStudentRecord();
            if (student != null) {
                identities.merge(student.getId(), 1, Integer::sum);
            }
        }
        return identities;
    }

    private StudentGrade gradeSubmission(SubmissionResult submission, Map<String, Integer> identities,
                                         Path run, int index, int total,
                                         Map<SubmissionResult, String> withheld)
            throws IOException, InterruptedException {
        String blocker = blocker(submission, identities);
        List<TestCaseResult> results = new ArrayList<>();
        System.out.println("Grading " + index + "/" + total + ": "
                + submission.getArchivePath().getFileName());
        if (blocker != null) {
            withheld.put(submission, blocker);
            for (TestCaseDefinition test : cases) {
                results.add(new TestCaseResult(test, TestStatus.NOT_RUN, 0, "", blocker));
            }
        } else {
            Path studentDirectory = Files.createDirectory(run.resolve("submission-" + index));
            for (QuestionDefinition question : app.getAssessment().getQuestions()) {
                checkCancelled();
                gradeQuestion(submission, question, studentDirectory, results, withheld);
            }
        }
        if (results.size() != cases.size()) {
            throw new IOException("incomplete grading result; score export stopped");
        }
        return new StudentGrade(submission, results);
    }

    private void gradeQuestion(SubmissionResult submission, QuestionDefinition question,
                               Path studentDirectory, List<TestCaseResult> results,
                               Map<SubmissionResult, String> withheld) throws InterruptedException {
        List<TestCaseDefinition> questionCases = casesFor(question.getId());
        try {
            Path part = studentDirectory.resolve(question.getId());
            ProcessResult compilation = new StudentCompiler().compile(question,
                    submission.getWorkingRoot(), part, execution.javacExecutable(),
                    execution.compileTimeoutMillis(), execution.maxOutputBytes());
            if (compilation.timedOut() || compilation.exitCode() != 0) {
                recordCompilationFailure(results, questionCases, compilation);
            } else {
                for (TestCaseDefinition test : questionCases) {
                    checkCancelled();
                    results.add(new CaseExecutor().execute(test, question, part,
                            execution.javaExecutable(), execution.caseTimeoutMillis(),
                            execution.maxOutputBytes()));
                }
            }
        } catch (IOException exception) {
            String reason = "grading infrastructure error: " + exception.getMessage();
            withheld.put(submission, reason);
            for (TestCaseDefinition test : questionCases) {
                if (!hasResult(results, test.getId())) {
                    results.add(new TestCaseResult(test, TestStatus.NOT_RUN, 0, "", reason));
                }
            }
        }
    }

    private void recordCompilationFailure(List<TestCaseResult> results,
                                          List<TestCaseDefinition> questionCases,
                                          ProcessResult compilation) {
        String diagnostic = compilation.stderr();
        if (compilation.timedOut()) {
            diagnostic = "compilation deadline exceeded\n" + diagnostic;
        }
        for (TestCaseDefinition test : questionCases) {
            results.add(new TestCaseResult(test, TestStatus.COMPILE_ERROR,
                    compilation.durationMillis(), compilation.stdout(), diagnostic));
        }
    }

    private String blocker(SubmissionResult submission, Map<String, Integer> identities) {
        StudentRecord student = submission.getIdentity().getStudentRecord();
        if (student == null) {
            return "unresolved student identity";
        }
        if (identities.get(student.getId()) > 1) {
            return "duplicate submissions for the same student";
        }
        if (submission.getStatus() == SubmissionStatus.FAILED || submission.getWorkingRoot() == null) {
            return "submission preparation did not produce a usable working copy";
        }
        return null;
    }

    private List<TestCaseDefinition> casesFor(String questionId) {
        List<TestCaseDefinition> selected = new ArrayList<>();
        for (TestCaseDefinition test : cases) {
            if (test.getQuestionId().equals(questionId)) {
                selected.add(test);
            }
        }
        return selected;
    }

    private boolean hasResult(List<TestCaseResult> results, String id) {
        for (TestCaseResult result : results) {
            if (result.getTestCase().getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
