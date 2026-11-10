package autograder.gui;

import autograder.RunOutcome;
import autograder.util.ElapsedTime;

import java.io.UncheckedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.List;

/** Wording for run progress and results, kept free of Swing so it can be tested headless. */
public final class OutcomeText {
    public static final String CHECKING = "Checking setup…";
    public static final String WRITING_REPORTS = "Writing reports…";
    public static final String STOPPING = "Stopping…";
    public static final String FIX_FIELDS = "Fix the highlighted fields";
    private static final String ENVIRONMENT_HINT =
            " Check that a JDK is installed and the tester files are present.";

    private OutcomeText() {
    }

    public static String intake(String item, int completed, int total) {
        return step("Preparing", item, completed, total);
    }

    public static String grading(String item, int completed, int total) {
        return step("Grading", item, completed, total);
    }

    public static String headline(RunOutcome outcome) {
        return switch (outcome.status()) {
            case COMPLETED -> "Grading complete";
            case REPORT_FAILURES -> "Grading complete. Some reports could not be written.";
            case NO_SUBMISSIONS -> "No submissions matched";
            case INTERRUPTED -> stopped(outcome);
            default -> failed(outcome);
        };
    }

    /** Headline followed by how long the run took, e.g. "Grading complete\nRun time: 1 min 23 s". */
    public static String headline(RunOutcome outcome, long elapsedMillis) {
        return headline(outcome) + "\nRun time: " + ElapsedTime.describe(elapsedMillis);
    }

    /** Progress text with the live elapsed clock appended, e.g. "Grading 3 of 6: chee · 0:42". */
    public static String withClock(String text, long elapsedMillis) {
        String clock = ElapsedTime.clock(elapsedMillis);
        return text == null || text.isBlank() ? clock : text + " · " + clock;
    }

    /** One line per reported problem, or empty when there are none. */
    public static String reportProblems(List<String> messages) {
        StringBuilder text = new StringBuilder();
        for (String message : messages) {
            if (message != null && !message.isBlank()) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(message.strip());
            }
        }
        return text.toString();
    }

    private static String stopped(RunOutcome outcome) {
        RunOutcome.Stage stage = outcome.failedStage();
        if (outcome.run() != null || stage == RunOutcome.Stage.REPORTS) {
            return "Grading stopped while " + stageLabel(stage)
                    + ". Scores may have been written and reports may be incomplete.";
        }
        if (stage == RunOutcome.Stage.GRADING) {
            return "Grading stopped while grading. Some scores may have been written.";
        }
        return "Grading stopped. No scores were written.";
    }

    private static String stageLabel(RunOutcome.Stage stage) {
        if (stage == null) {
            return "running";
        }
        return switch (stage) {
            case ENVIRONMENT -> "checking the setup";
            case PREPARATION -> "preparing submissions";
            case GRADING -> "grading";
            default -> "writing reports";
        };
    }

    private static String failed(RunOutcome outcome) {
        String text = outcome.failedStage() == null
                ? "Grading stopped: " + reason(outcome.failure())
                : "Grading stopped while " + stageLabel(outcome.failedStage()) + ": " + reason(outcome.failure());
        if (outcome.failedStage() == RunOutcome.Stage.ENVIRONMENT) {
            return text + ENVIRONMENT_HINT;
        }
        return text;
    }

    private static String reason(Exception failure) {
        if (failure == null) {
            return "unknown error";
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ClosedByInterruptException) {
                return "the run was stopped";
            }
        }
        Throwable cause = failure;
        while (cause instanceof UncheckedIOException wrapper && wrapper.getCause() != null) {
            cause = wrapper.getCause();
        }
        if (cause instanceof NoSuchFileException missing) {
            return withPath("The file or folder was not found", missing.getFile());
        }
        if (cause instanceof AccessDeniedException denied) {
            return withPath("Permission was denied", denied.getFile());
        }
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static String withPath(String sentence, String path) {
        return path == null || path.isBlank() ? sentence : sentence + ": " + path;
    }

    private static String step(String verb, String item, int completed, int total) {
        String text = verb + " " + completed + " of " + total;
        return item == null || item.isBlank() ? text : text + ": " + item;
    }
}
