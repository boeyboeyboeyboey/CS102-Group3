package autograder.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryCleanerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void succeedsAfterTransientFailures() throws Exception {
        Path tree = createTree();
        AtomicInteger failures = new AtomicInteger(2);
        List<Long> sleeps = new ArrayList<>();
        DirectoryCleaner cleaner = new DirectoryCleaner(path -> {
            if (failures.getAndDecrement() > 0) {
                throw new AccessDeniedException(path.toString());
            }
            Files.deleteIfExists(path);
        }, sleeps::add);

        cleaner.deleteRecursively(tree);

        assertFalse(Files.exists(tree));
        assertEquals(List.of(50L, 150L), sleeps);
    }

    @Test
    void rethrowsLastFailureAfterThreeAttempts() throws Exception {
        Path tree = createTree();
        AtomicInteger attempts = new AtomicInteger();
        List<AccessDeniedException> thrown = new ArrayList<>();
        DirectoryCleaner cleaner = new DirectoryCleaner(path -> {
            attempts.incrementAndGet();
            AccessDeniedException exception = new AccessDeniedException(path.toString());
            thrown.add(exception);
            throw exception;
        }, millis -> { });

        AccessDeniedException failure = assertThrows(AccessDeniedException.class,
                () -> cleaner.deleteRecursively(tree));

        assertEquals(3, attempts.get());
        assertSame(thrown.get(2), failure);
        assertTrue(Files.exists(tree));
    }

    @Test
    void doesNotRetryOtherIoExceptions() throws Exception {
        Path tree = createTree();
        AtomicInteger attempts = new AtomicInteger();
        DirectoryCleaner cleaner = new DirectoryCleaner(path -> {
            attempts.incrementAndGet();
            throw new IOException("disk failure");
        }, millis -> { });

        assertThrows(IOException.class, () -> cleaner.deleteRecursively(tree));

        assertEquals(1, attempts.get());
    }

    @Test
    void restoresInterruptFlagAndRethrowsPendingException() throws Exception {
        Path tree = createTree();
        DirectoryCleaner cleaner = new DirectoryCleaner(path -> {
            throw new AccessDeniedException(path.toString());
        }, millis -> {
            throw new InterruptedException();
        });

        try {
            assertThrows(AccessDeniedException.class, () -> cleaner.deleteRecursively(tree));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void deletesRealTreeAndIgnoresMissingDirectory() throws Exception {
        Path tree = createTree();
        DirectoryCleaner cleaner = new DirectoryCleaner();

        cleaner.deleteRecursively(tree);
        cleaner.deleteRecursively(tree);

        assertFalse(Files.exists(tree));
    }

    private Path createTree() throws IOException {
        Path tree = Files.createDirectories(temporaryDirectory.resolve("tree/inner"));
        Files.writeString(tree.resolve("file.txt"), "x");
        return temporaryDirectory.resolve("tree");
    }
}
