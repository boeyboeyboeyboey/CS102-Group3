package autograder.grading;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionSchedulerTest {
    @Test
    void preservesIndexOrderWhenTasksFinishOutOfOrder() throws Exception {
        List<Integer> results = new SubmissionScheduler(4).run(8, index -> {
            Thread.sleep((8 - index) * 15L);
            return index;
        });
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), results);
    }

    @Test
    void sequentialPathRunsOnCallingThreadAndStopsAtFirstFailure() throws Exception {
        Thread caller = Thread.currentThread();
        List<Thread> threads = new java.util.ArrayList<>();
        new SubmissionScheduler(1).run(3, index -> {
            threads.add(Thread.currentThread());
            return index;
        });
        assertEquals(List.of(caller, caller, caller), threads);
        List<Integer> started = new java.util.ArrayList<>();
        IOException failure = new IOException("first");
        IOException thrown = assertThrows(IOException.class, () -> new SubmissionScheduler(1).run(3, index -> {
            started.add(index);
            throw failure;
        }));
        assertSame(failure, thrown);
        assertEquals(List.of(0), started);
    }

    @Test
    void rethrowsLowestIndexFailure() {
        IOException low = new IOException("low");
        assertSame(low, assertThrows(IOException.class, () -> new SubmissionScheduler(4).run(4, index -> {
            if (index == 1) {
                Thread.sleep(80);
                throw low;
            }
            if (index == 3) {
                throw new IllegalStateException("high");
            }
            return index;
        })));
        IllegalStateException runtime = new IllegalStateException("low runtime");
        assertSame(runtime, assertThrows(IllegalStateException.class,
                () -> new SubmissionScheduler(4).run(4, index -> {
                    if (index == 0) {
                        Thread.sleep(80);
                        throw runtime;
                    }
                    throw new IOException("high");
                })));
    }

    @Test
    void interruptingCallerCancelsRunningTasksAndStopsPoolThreads() throws Exception {
        CountDownLatch running = new CountDownLatch(3);
        CountDownLatch cancelled = new CountDownLatch(3);
        Set<Thread> poolThreads = ConcurrentHashMap.newKeySet();
        Throwable[] thrown = new Throwable[1];
        Thread caller = new Thread(() -> {
            try {
                new SubmissionScheduler(3).run(3, index -> {
                    poolThreads.add(Thread.currentThread());
                    running.countDown();
                    try {
                        Thread.sleep(60_000);
                    } catch (InterruptedException exception) {
                        cancelled.countDown();
                        throw exception;
                    }
                    return index;
                });
            } catch (Throwable exception) {
                thrown[0] = exception;
            }
        });
        caller.start();
        assertTrue(running.await(5, TimeUnit.SECONDS));
        long start = System.nanoTime();
        caller.interrupt();
        caller.join(15_000);
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 12);
        assertTrue(thrown[0] instanceof InterruptedException);
        assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        for (Thread thread : poolThreads) {
            thread.join(5_000);
            assertTrue(!thread.isAlive());
        }
    }
}
