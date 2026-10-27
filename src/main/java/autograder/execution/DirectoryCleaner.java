package autograder.execution;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/**
 * Deletes a directory tree, retrying briefly when the file system reports a transient failure
 * such as a Windows file handle that a finished child process has not released yet.
 */
public final class DirectoryCleaner {
    private static final long[] RETRY_DELAYS_MILLIS = {50, 150};

    /** Deletes a single path and may throw an IOException. */
    @FunctionalInterface
    interface Deleter {
        void delete(Path path) throws IOException;
    }

    /** Pauses the current thread. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final Deleter deleter;
    private final Sleeper sleeper;

    public DirectoryCleaner() {
        this(Files::deleteIfExists, Thread::sleep);
    }

    DirectoryCleaner(Deleter deleter, Sleeper sleeper) {
        this.deleter = deleter;
        this.sleeper = sleeper;
    }

    public void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        List<Path> paths;
        try (var walk = Files.walk(directory)) {
            paths = walk.sorted((left, right) -> right.compareTo(left)).toList();
        }
        for (Path path : paths) {
            deleteWithRetry(path);
        }
    }

    private void deleteWithRetry(Path path) throws IOException {
        int attempt = 0;
        while (true) {
            try {
                deleter.delete(path);
                return;
            } catch (NoSuchFileException exception) {
                return;
            } catch (FileSystemException exception) {
                if (attempt >= RETRY_DELAYS_MILLIS.length) {
                    throw exception;
                }
                try {
                    sleeper.sleep(RETRY_DELAYS_MILLIS[attempt]);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
                attempt++;
            }
        }
    }
}
