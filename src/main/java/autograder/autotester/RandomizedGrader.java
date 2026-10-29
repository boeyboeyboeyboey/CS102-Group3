package autograder.autotester;

import autograder.execution.ProcessResult;
import autograder.execution.ProcessRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RandomizedGrader {
    private static final long MAX_TOTAL_TRIALS = 10_000;
    private static final long MAX_ESTIMATED_INPUT_BYTES = 16L * 1024 * 1024;
    private static final long COMPILATION_TIMEOUT_MILLIS = 60_000;

    public RandomizedResult grade(Question question, Path submissionDirectory,
                                  RandomizedConfig config, long seed)
            throws IOException, InterruptedException {
        question.validate();
        Path submission = submissionDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(submission)) {
            throw new IOException("student submission folder does not exist: " + submission);
        }
        Path resultsDirectory = config.resultsDirectory().toAbsolutePath().normalize();
        if (submission.startsWith(resultsDirectory) || resultsDirectory.startsWith(submission)) {
            throw new IOException("randomized results directory must not overlap the student submission folder");
        }
        Path source = submission.resolve(question.getClassName() + ".java");
        if (!Files.isRegularFile(source)) {
            throw new IOException("could not find " + source);
        }
        for (String companion : question.getCompanionSourceNames()) {
            if (!Files.isRegularFile(submission.resolve(companion))) {
                throw new IOException("could not find required companion source " + companion);
            }
        }
        Files.createDirectories(config.resultsDirectory());
        List<Trial> trials = generateTrials(question, config.runsPerCase(), seed);
        Path workspace = Files.createTempDirectory(config.resultsDirectory(), "randomized-");
        try {
            return gradeInWorkspace(question, source, config, seed, trials, workspace);
        } finally {
            deleteTree(workspace);
        }
    }

    public Path writeResult(Question question, Path submissionDirectory,
                            RandomizedConfig config, RandomizedResult result) throws IOException {
        String student = safeName(submissionDirectory.getFileName() == null
                ? "submission" : submissionDirectory.getFileName().toString());
        Path output = config.resultsDirectory().resolve(student + "-" + question.getClassName() + ".txt");
        Files.createDirectories(output.getParent());
        StringBuilder text = new StringBuilder(result.passed() ? "PASS\n" : "FAIL\n")
                .append("passed ").append(result.passedCount()).append(" of ")
                .append(result.totalCount()).append(" tests\nseed: ").append(result.seed()).append('\n');
        if (!result.errorReason().isBlank()) {
            text.append(result.errorReason().strip()).append('\n');
        }
        for (RandomizedResult.Failure failure : result.failures()) {
            text.append("test case ").append(failure.testCaseId()).append(" run ")
                    .append(failure.runNumber()).append(": input (").append(failure.input())
                    .append(") expected ").append(failure.expected()).append(", got ")
                    .append(failure.actual()).append('\n');
        }
        int failures = result.totalCount() - result.passedCount();
        int omitted = Math.max(0, failures - result.failures().size());
        if (omitted > 0) text.append("... and ").append(omitted).append(" more failure(s)\n");
        Files.writeString(output, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return output;
    }

    private RandomizedResult gradeInWorkspace(Question question, Path source, RandomizedConfig config,
                                              long seed, List<Trial> trials, Path workspace)
            throws IOException, InterruptedException {
        Path sourceDirectory = Files.createDirectory(workspace.resolve("src"));
        Path classesDirectory = Files.createDirectory(workspace.resolve("classes"));
        Path studentSource = sourceDirectory.resolve(source.getFileName());
        Files.copy(source, studentSource, StandardCopyOption.REPLACE_EXISTING);
        List<Path> studentSources = new ArrayList<>(List.of(studentSource));
        for (String companion : question.getCompanionSourceNames()) {
            Path copy = sourceDirectory.resolve(companion);
            Files.copy(source.getParent().resolve(companion), copy, StandardCopyOption.REPLACE_EXISTING);
            studentSources.add(copy);
        }
        studentSources.addAll(copyArtifacts(question, classesDirectory, workspace, sourceDirectory));
        Path runnerSource = sourceDirectory.resolve("RandomizedStudentRunner.java");
        copyRunner(runnerSource);
        List<String> compileCommand = new ArrayList<>(List.of(config.javacCommand(), "-proc:none", "-encoding",
                "UTF-8", "--release", "17", "-classpath", classesDirectory.toString(),
                "-d", classesDirectory.toString()));
        for (Path studentSourceFile : studentSources) compileCommand.add(studentSourceFile.toString());
        compileCommand.add(runnerSource.toString());
        ProcessResult compilation = ProcessRunner.run(compileCommand, workspace,
                Math.max(config.timeoutMillis(), COMPILATION_TIMEOUT_MILLIS), config.maxOutputBytes());
        if (compilation.timedOut() || compilation.exitCode() != 0) {
            String reason = compilation.timedOut() ? "COMPILE TIMEOUT\n" : "COMPILE ERROR\n";
            return failedResult(seed, trials.size(), reason + compilation.stderr());
        }

        return executeTrials(question, config, seed, trials, workspace, classesDirectory);
    }

    private List<Path> copyArtifacts(Question question, Path classesDirectory, Path workspace,
                                    Path sourceDirectory) throws IOException {
        List<Path> supportSources = new ArrayList<>();
        for (Question.Artifact artifact : question.getArtifacts()) {
            if (artifact.kind() == Question.ArtifactKind.STARTER_SOURCE) continue;
            Path source = Path.of("").toAbsolutePath().resolve(artifact.source()).normalize();
            if (!Files.isRegularFile(source)) {
                throw new IOException("question resource is missing: " + source);
            }
            Path destinationDirectory = switch (artifact.kind()) {
                case SUPPORT_CLASS -> classesDirectory;
                case SUPPORT_SOURCE -> sourceDirectory;
                case FIXTURE -> workspace;
                case STARTER_SOURCE -> throw new IllegalStateException("starter source is not a runtime artifact");
            };
            Path destination = destinationDirectory.resolve(source.getFileName());
            Files.copy(source, destination,
                    StandardCopyOption.REPLACE_EXISTING);
            if (artifact.kind() == Question.ArtifactKind.SUPPORT_SOURCE) supportSources.add(destination);
        }
        return supportSources;
    }

    static List<String> childCommand(String javaCommand, Path classesDirectory, Question question,
                                     Path inputFile, Path outputFile) {
        // child output is decoded as utf-8 by the parent whatever the platform default is
        List<String> command = new ArrayList<>(List.of(javaCommand, "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-classpath",
                classesDirectory.toString(), "RandomizedStudentRunner", question.getClassName(),
                question.getMethodName(), inputFile.toString(), outputFile.toString()));
        for (Question.Parameter parameter : question.getParameters()) {
            command.add(typeName(parameter.type()));
        }
        return command;
    }

    private RandomizedResult executeTrials(Question question, RandomizedConfig config, long seed,
                                           List<Trial> trials, Path workspace, Path classesDirectory)
            throws IOException, InterruptedException {
        Path inputFile = workspace.resolve("inputs.bin");
        Path outputFile = workspace.resolve("outputs.bin");
        List<Object[]> inputArguments = trials.stream().map(trial -> trial.arguments().toArray()).toList();
        RandomizedTrialProtocol.writeInputs(inputFile, inputArguments);
        List<String> command = childCommand(config.javaCommand(), classesDirectory, question,
                inputFile, outputFile);
        ProcessResult execution = ProcessRunner.run(command, workspace,
                config.timeoutMillis(), config.maxOutputBytes());
        if (execution.timedOut()) {
            return failedResult(seed, trials.size(), "TIMEOUT: the submission ran longer than "
                    + config.timeoutMillis() / 1_000 + " seconds (possible infinite loop)");
        }
        if (execution.exitCode() != 0 || !Files.isRegularFile(outputFile)) {
            return failedResult(seed, trials.size(), "The submission crashed or called System.exit before finishing.\n"
                    + execution.stderr());
        }
        List<?> outcomes = RandomizedTrialProtocol.readOutcomes(outputFile);
        if (outcomes.size() != trials.size()) {
            return failedResult(seed, trials.size(), "student runner returned an invalid number of results");
        }
        return compareOutcomes(trials, outcomes, config, seed);
    }

    private RandomizedResult compareOutcomes(List<Trial> trials, List<?> outcomes,
                                             RandomizedConfig config, long seed) throws IOException {
        int passed = 0;
        List<RandomizedResult.Failure> failures = new ArrayList<>();
        for (int index = 0; index < trials.size(); index++) {
            Trial trial = trials.get(index);
            Object[] outcome = RandomizedTrialProtocol.requireOutcome(outcomes.get(index));
            boolean expectedException = trial.expected() instanceof Question.ExpectedException expected
                    && !Boolean.TRUE.equals(outcome[0])
                    && exceptionMatches(expected.className(), String.valueOf(outcome[1]));
            if (expectedException || Boolean.TRUE.equals(outcome[0])
                    && java.util.Objects.deepEquals(trial.expected(), outcome[1])) {
                passed++;
            } else if (failures.size() < config.maxFailuresShown()) {
                String actual = Boolean.TRUE.equals(outcome[0])
                        ? Literals.show(outcome[1]) : "EXCEPTION: " + outcome[1];
                failures.add(new RandomizedResult.Failure(trial.caseId(), trial.runNumber(),
                        showArguments(trial.arguments()), Literals.show(trial.expected()), actual));
            }
        }
        return new RandomizedResult(passed == trials.size(), passed, trials.size(), seed, failures, "");
    }

    private List<Trial> generateTrials(Question question, int runsPerCase, long seed) {
        long total = (long) question.getTestCases().size() * runsPerCase;
        if (total > MAX_TOTAL_TRIALS) {
            throw new IllegalArgumentException("question exceeds the maximum of "
                    + MAX_TOTAL_TRIALS + " trials per grading run");
        }
        Random random = new Random(seed);
        List<Trial> trials = new ArrayList<>();
        long estimatedInputBytes = 0;
        for (Question.RandomTestCase testCase : question.getTestCases()) {
            for (int run = 1; run <= runsPerCase; run++) {
                List<Object> arguments = ResourceGenerator.generate(testCase, random);
                question.validateArguments(arguments);
                estimatedInputBytes += estimateInputBytes(arguments);
                if (estimatedInputBytes > MAX_ESTIMATED_INPUT_BYTES) {
                    throw new IllegalArgumentException("generated inputs exceed the 16 MiB per-run limit");
                }
                try {
                    Object expected = question.answerKey(ResourceGenerator.copyArguments(arguments));
                    question.validateExpected(expected);
                    trials.add(new Trial(testCase.id(), run, arguments, expected));
                } catch (Exception exception) {
                    throw new IllegalArgumentException("answer key failed for test case " + testCase.id()
                            + " run " + run, exception);
                }
            }
        }
        return List.copyOf(trials);
    }

    private long estimateInputBytes(List<Object> arguments) {
        long bytes = 64;
        for (Object argument : arguments) bytes += estimateValueBytes(argument);
        return bytes;
    }

    private long estimateValueBytes(Object value) {
        if (value == null) return 8;
        if (value instanceof String text) return 16L + text.length() * 2L;
        if (value instanceof int[] values) return 16L + values.length * Integer.BYTES;
        if (value instanceof String[] values) {
            long bytes = 16;
            for (String item : values) bytes += estimateValueBytes(item);
            return bytes;
        }
        if (value instanceof Object[] values) {
            long bytes = 16;
            for (Object item : values) bytes += estimateValueBytes(item);
            return bytes;
        }
        if (value instanceof java.util.Collection<?> values) {
            long bytes = 24;
            for (Object item : values) bytes += estimateValueBytes(item);
            return bytes;
        }
        return 16;
    }

    private String showArguments(List<Object> arguments) {
        return arguments.stream().map(Literals::show)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String typeName(Class<?> type) {
        return type.isArray() ? type.getCanonicalName() : type.getName();
    }

    private RandomizedResult failedResult(long seed, int total, String reason) {
        return new RandomizedResult(false, 0, total, seed, List.of(), reason.strip());
    }

    private boolean exceptionMatches(String expectedClass, String actualDescription) {
        String simpleName = expectedClass.substring(expectedClass.lastIndexOf('.') + 1);
        return actualDescription.equals(simpleName) || actualDescription.startsWith(simpleName + ":");
    }

    private String safeName(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "submission" : sanitized;
    }

    private void copyRunner(Path target) throws IOException {
        try (InputStream runner = RandomizedGrader.class.getResourceAsStream(
                "/autograder/autotester/RandomizedStudentRunner.java")) {
            if (runner == null) throw new IOException("randomized student runner resource is missing");
            Files.copy(runner, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.delete(path);
            }
        }
    }

    private record Trial(String caseId, int runNumber, List<Object> arguments, Object expected) { }
}
