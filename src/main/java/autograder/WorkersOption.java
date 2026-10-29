package autograder;

import autograder.config.ExecutionConfig;
import autograder.config.WorkerLimits;

/**
 * Parses the value of the --workers command line flag.
 */
final class WorkersOption {
    static final String FLAG = "--workers";
    private static final String AUTO = "auto";

    private WorkersOption() {
    }

    static int parse(String value, WorkerLimits limits) {
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase(AUTO)) {
            return limits.autoWorkers();
        }
        try {
            int workers = Integer.parseInt(trimmed);
            if (workers >= 1 && workers <= ExecutionConfig.MAX_WORKERS) {
                return workers;
            }
        } catch (NumberFormatException exception) {
            // reported below with the same message
        }
        throw new IllegalArgumentException(FLAG + " must be auto or a whole number from 1 to "
                + ExecutionConfig.MAX_WORKERS + ": " + value);
    }
}
