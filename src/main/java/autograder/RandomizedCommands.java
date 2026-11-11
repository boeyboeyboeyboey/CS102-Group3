package autograder;

import autograder.autotester.execution.RandomizedBatchGrader;
import autograder.autotester.execution.RandomizedBatchResult;
import autograder.autotester.execution.RandomizedConfig;
import autograder.autotester.execution.RandomizedGrader;
import autograder.autotester.execution.RandomizedResult;
import autograder.autotester.execution.RandomizedSubmissionResult;
import autograder.autotester.execution.ResourceGenerator;
import autograder.autotester.question.Question;
import autograder.autotester.question.QuestionRegistry;
import autograder.config.AppConfig;
import autograder.util.ExceptionMessages;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.OptionalLong;

import static autograder.App.printUsage;

/** The randomized command-line commands: resource generation and single or batch grading. */
final class RandomizedCommands {
    private RandomizedCommands() {
    }

    static int runRandomized(String[] args) {
        try {
            return switch (args[0]) {
                case "--randomized-resources" -> generateRandomizedResources(args);
                case "--randomized-grade" -> gradeRandomizedSubmission(args);
                case "--randomized-grade-batch" -> gradeRandomizedBatch(args);
                default -> {
                    printUsage();
                    yield 1;
                }
            };
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("randomized autograder error: " + ExceptionMessages.describe(exception));
            return 1;
        }
    }

    private static int generateRandomizedResources(String[] args) throws Exception {
        if (args.length != 3) {
            printUsage();
            return 1;
        }
        Question question = QuestionRegistry.find(args[1]);
        RandomizedConfig config = RandomizedConfig.load(Path.of(args[2]));
        new ResourceGenerator().generate(question, config.resourcesDirectory(),
                config.examplesPerCase(), new SecureRandom().nextLong());
        System.out.println("Student stub: " + config.resourcesDirectory().resolve(question.getClassName() + ".java"));
        System.out.println("Student tester: " + config.resourcesDirectory()
                .resolve(question.getClassName() + "Tester.java"));
        return 0;
    }

    // a bad seed prints a plain sentence and the usage, and the command exits with 1
    private static OptionalLong parseSeed(String[] args) {
        if (args.length != 5) {
            return OptionalLong.of(new SecureRandom().nextLong());
        }
        try {
            return OptionalLong.of(Long.parseLong(args[4]));
        } catch (NumberFormatException exception) {
            System.err.println("The seed must be a whole number, for example 42, but was: " + args[4]);
            printUsage();
            return OptionalLong.empty();
        }
    }

    private static int gradeRandomizedSubmission(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            printUsage();
            return 1;
        }
        Question question = QuestionRegistry.find(args[1]);
        RandomizedConfig config = RandomizedConfig.load(Path.of(args[3]));
        OptionalLong parsedSeed = parseSeed(args);
        if (parsedSeed.isEmpty()) {
            return 1;
        }
        long seed = parsedSeed.getAsLong();
        RandomizedGrader grader = new RandomizedGrader();
        RandomizedResult result = grader.grade(question, Path.of(args[2]), config, seed);
        Path report = grader.writeResult(question, Path.of(args[2]), config, result);
        System.out.println((result.passed() ? "PASS" : "FAIL") + " | " + result.passedCount()
                + "/" + result.totalCount() + " tests | seed=" + result.seed());
        System.out.println("Result: " + report);
        return 0;
    }

    private static int gradeRandomizedBatch(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            printUsage();
            return 1;
        }
        Question question = QuestionRegistry.find(args[1]);
        Path zipDirectory = Path.of(args[2]);
        Path configFile = Path.of(args[3]);
        AppConfig appConfig = AppConfig.load(configFile);
        RandomizedConfig config = RandomizedConfig.load(configFile);
        OptionalLong parsedSeed = parseSeed(args);
        if (parsedSeed.isEmpty()) {
            return 1;
        }
        long seed = parsedSeed.getAsLong();
        RandomizedBatchResult batch = new RandomizedBatchGrader().grade(question, zipDirectory,
                appConfig, config, seed);
        for (RandomizedSubmissionResult submission : batch.submissions()) {
            RandomizedResult result = submission.result();
            System.out.println(submission.archive() + " | " + (result.passed() ? "PASS" : "FAIL")
                    + " | " + result.passedCount() + "/" + result.totalCount()
                    + " tests | seed=" + result.seed()
                    + (result.errorReason().isBlank() ? "" : " | " + result.errorReason()));
        }
        System.out.println("Randomized batch: " + batch.passedCount() + " passed; "
                + batch.failedCount() + " failed of " + batch.submissions().size());
        System.out.println("Summary: " + batch.summaryFile());
        System.out.println("Per-submission reports: " + batch.outputDirectory());
        return 0;
    }
}
