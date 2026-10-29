package autograder.grading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class SubmissionScheduler {
    private static final long TERMINATION_WAIT_SECONDS = 10;

    @FunctionalInterface
    interface IndexedTask<T> {
        T grade(int index) throws IOException, InterruptedException;
    }

    private final int workers;

    SubmissionScheduler(int workers) {
        this.workers = workers;
    }

    <T> List<T> run(int total, IndexedTask<T> task) throws IOException, InterruptedException {
        if (workers <= 1 || total <= 1) {
            List<T> results = new ArrayList<>();
            for (int index = 0; index < total; index++) {
                results.add(task.grade(index));
            }
            return results;
        }
        return runParallel(total, task);
    }

    private <T> List<T> runParallel(int total, IndexedTask<T> task)
            throws IOException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(workers, total), threadFactory());
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < total; index++) {
                int taskIndex = index;
                futures.add(pool.submit(() -> task.grade(taskIndex)));
            }
            return collect(futures);
        } finally {
            for (Future<T> future : futures) {
                future.cancel(true);
            }
            pool.shutdownNow();
            awaitTermination(pool);
        }
    }

    // futures are read in index order, so the first failure seen is the lowest failing index
    private <T> List<T> collect(List<Future<T>> futures) throws IOException, InterruptedException {
        List<T> results = new ArrayList<>();
        Throwable failure = null;
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof InterruptedException interrupted) {
                    throw interrupted;
                }
                failure = cause;
                break;
            }
        }
        if (failure == null) {
            return results;
        }
        if (failure instanceof IOException ioException) {
            throw ioException;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IOException(failure);
    }

    private static void awaitTermination(ExecutorService pool) {
        boolean interrupted = false;
        try {
            pool.awaitTermination(TERMINATION_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            interrupted = true;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory threadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "grading-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
