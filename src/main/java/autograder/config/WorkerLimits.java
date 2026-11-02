package autograder.config;

import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * Bounds worker counts by the processors available to this machine.
 */
public final class WorkerLimits {
    // highest worker count exercised by the parallel grading checks
    private static final int AUTO_WORKER_CEILING = 4;

    private final IntSupplier processors;

    public WorkerLimits(IntSupplier processors) {
        this.processors = processors;
    }

    public static WorkerLimits system() {
        return new WorkerLimits(Runtime.getRuntime()::availableProcessors);
    }

    public int processors() {
        int available = processors.getAsInt();
        return Math.max(1, Math.min(ExecutionConfig.MAX_WORKERS, available));
    }

    public int autoWorkers() {
        return Math.min(AUTO_WORKER_CEILING, processors());
    }

    public Capped cap(int requested) {
        int available = processors();
        if (requested > available) {
            return new Capped(available, Optional.of("execution.workers="
                    + requested + " exceeds " + available
                    + " available processors; using " + available));
        }
        return new Capped(requested, Optional.empty());
    }

    public record Capped(int workers, Optional<String> warning) {
    }
}
