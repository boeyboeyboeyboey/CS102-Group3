package autograder;

import autograder.config.AppConfig;
import autograder.config.RunSettings;
import autograder.config.RuntimePathOverrides;
import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void emptySubmissionDirectoryReportsNoSubmissionsAndCreatesNoRunDirectory() throws Exception {
        RecordingListener listener = new RecordingListener();
        RunSettings settings = settings(Files.createDirectories(temporaryDirectory.resolve("empty")));

        RunOutcome outcome = new GradingService().run(settings, listener);

        assertEquals(RunOutcome.Status.NO_SUBMISSIONS, outcome.status());
        assertEquals(1, outcome.exitCode());
        assertNull(outcome.run());
        assertFalse(outcome.graded());
        assertEquals(List.of("preparing"), listener.events);
        Path grading = settings.execution().directory();
        if (Files.exists(grading)) {
            try (Stream<Path> children = Files.list(grading)) {
                assertEquals(0, children.count());
            }
        }
    }

    @Test
    void missingInstructorResourceFailsInTheEnvironmentStage() throws Exception {
        RunSettings settings = settings(Files.createDirectories(temporaryDirectory.resolve("empty")));
        Path tester = settings.app().getAssessment().getQuestions().get(0).getTesterFile();
        Path hidden = tester.resolveSibling(tester.getFileName() + ".hidden");
        Files.move(tester, hidden);
        RecordingListener listener = new RecordingListener();
        try {
            RunOutcome outcome = new GradingService().run(settings, listener);

            assertEquals(RunOutcome.Status.FAILED, outcome.status());
            assertEquals(RunOutcome.Stage.ENVIRONMENT, outcome.failedStage());
            assertTrue(outcome.failure().getMessage().contains("instructor tester is missing"));
            assertEquals(1, outcome.exitCode());
            assertTrue(listener.events.isEmpty());
        } finally {
            Files.move(hidden, tester);
        }
    }

    @Test
    void interruptedThreadStopsBeforeIntakeAndKeepsTheInterruptFlag() throws Exception {
        RunSettings settings = settings(Files.createDirectories(temporaryDirectory.resolve("empty")));
        RecordingListener listener = new RecordingListener();
        Thread.currentThread().interrupt();
        try {
            RunOutcome outcome = new GradingService().run(settings, listener);

            assertEquals(RunOutcome.Status.INTERRUPTED, outcome.status());
            assertInstanceOf(InterruptedException.class, outcome.failure());
            assertEquals("grading stopped", outcome.failure().getMessage());
            assertEquals(1, outcome.exitCode());
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(listener.events.isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptDuringIntakeReturnsInterruptedInPreparation() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("subs"));
        RunSettings settings = settings(submissions);
        Path sample = Path.of("").toAbsolutePath().resolve("Project/student-submission");
        try (Stream<Path> zips = Files.list(sample)) {
            for (Path zip : (Iterable<Path>) zips.limit(2)::iterator) {
                Files.copy(zip, submissions.resolve(zip.getFileName()));
            }
        }
        RecordingListener listener = new RecordingListener() {
            @Override
            public void onProgress(String stage, String item, int completed, int total) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            RunOutcome outcome = new GradingService().run(settings, listener);

            assertEquals(RunOutcome.Status.INTERRUPTED, outcome.status());
            assertEquals(RunOutcome.Stage.PREPARATION, outcome.failedStage());
            assertFalse(listener.events.contains("prepared"));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void listenerFailureWhilePreparingDoesNotFailTheRun() throws Exception {
        RunSettings settings = settings(Files.createDirectories(temporaryDirectory.resolve("empty")));
        RecordingListener listener = new RecordingListener() {
            @Override
            public void preparing(String assessmentName) {
                throw new IllegalStateException("x", new java.nio.channels.ClosedByInterruptException());
            }
        };
        try {
            RunOutcome outcome = new GradingService().run(settings, listener);
            assertEquals(RunOutcome.Status.NO_SUBMISSIONS, outcome.status());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void listenerFailureDoesNotDiscardAGradedRun() throws Exception {
        Path submissions = Files.createDirectories(temporaryDirectory.resolve("subs"));
        RunSettings settings = settings(submissions);
        List<String> reportingProgress = new ArrayList<>();
        Path sample = Path.of("").toAbsolutePath().resolve("Project/student-submission");
        try (Stream<Path> zips = Files.list(sample)) {
            Path zip = zips.sorted().findFirst().orElseThrow();
            Files.copy(zip, submissions.resolve(zip.getFileName()));
        }
        RecordingListener listener = new RecordingListener() {
            @Override
            public void graded(GradingRun run, RunSettings runSettings) {
                throw new IllegalStateException("display broke");
            }

            @Override
            public void reportingProgress(String item, int completed, int total) {
                reportingProgress.add(item + ":" + completed + "/" + total);
            }
        };

        RunOutcome outcome = new GradingService().run(settings, listener);

        assertTrue(outcome.graded());
        assertTrue(outcome.run() != null);
        assertTrue(outcome.status() != RunOutcome.Status.FAILED);
        assertEquals(List.of("Writing score and diagnostic CSV files:0/2",
                "Score and diagnostic CSV files ready:1/2",
                "Generating optional reports:1/2",
                outcome.status() == RunOutcome.Status.COMPLETED
                        ? "Reports ready:2/2" : "Reports completed with errors:2/2"),
                reportingProgress);
    }

    private RunSettings settings(Path submissions) throws IOException {
        Path root = Path.of("").toAbsolutePath();
        Path copiedProject = temporaryDirectory.resolve("project");
        Files.createDirectories(copiedProject);
        copyTree(root.resolve("Project"), copiedProject.resolve("Project"));
        Path config = copiedProject.resolve("config.properties");
        Files.copy(root.resolve("config.properties"), config);
        return RunSettings.load(config, new RuntimePathOverrides(submissions, null, null));
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination);
                }
            }
        }
    }

    private static class RecordingListener implements RunListener {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onProgress(String stage, String item, int completed, int total) {
            events.add("progress");
        }

        @Override
        public void preparing(String assessmentName) {
            events.add("preparing");
        }

        @Override
        public void prepared(List<SubmissionResult> results, AppConfig app) {
            events.add("prepared");
        }

        @Override
        public void graded(GradingRun run, autograder.config.RunSettings settings) {
            events.add("graded");
        }
    }
}
