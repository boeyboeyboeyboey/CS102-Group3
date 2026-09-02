package autograder.service.process;

import autograder.model.ProcessResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Runs an external process without allowing output buffers or timeouts to block the batch. */
public final class ExternalProcessRunner {
    private final int maximumOutputBytes;

    public ExternalProcessRunner(int maximumOutputBytes) {
        this.maximumOutputBytes = maximumOutputBytes;
    }

    public ProcessResult run(List<String> command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        Instant started = Instant.now();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        ExecutorService capturePool = Executors.newFixedThreadPool(2);
        Future<CapturedOutput> stdout = capturePool.submit(
                new StreamCapture(process.getInputStream(), maximumOutputBytes));
        Future<CapturedOutput> stderr = capturePool.submit(
                new StreamCapture(process.getErrorStream(), maximumOutputBytes));
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                terminateProcessTree(process);
                process.waitFor(2, TimeUnit.SECONDS);
            }
            CapturedOutput capturedStdout = await(stdout);
            CapturedOutput capturedStderr = await(stderr);
            int exitCode = finished ? process.exitValue() : -1;
            return new ProcessResult(command, exitCode, !finished,
                    capturedStdout.truncated || capturedStderr.truncated,
                    capturedStdout.text, capturedStderr.text,
                    Duration.between(started, Instant.now()));
        } finally {
            capturePool.shutdownNow();
            if (process.isAlive()) {
                terminateProcessTree(process);
            }
        }
    }

    private CapturedOutput await(Future<CapturedOutput> future) throws IOException,
            InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Could not capture process output", cause);
        }
    }

    private void terminateProcessTree(Process process) {
        List<ProcessHandle> descendants = new ArrayList<>();
        try {
            process.descendants().forEach(descendants::add);
        } catch (RuntimeException exception) {
            // Some restricted hosts deny operating-system process enumeration.
            // The direct JVM is still terminated below so the batch can continue.
        }
        for (ProcessHandle descendant : descendants) {
            descendant.destroy();
        }
        process.destroy();
        for (ProcessHandle descendant : descendants) {
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static final class StreamCapture implements Callable<CapturedOutput> {
        private static final int BUFFER_SIZE = 8192;

        private final InputStream input;
        private final int maximumBytes;

        private StreamCapture(InputStream input, int maximumBytes) {
            this.input = input;
            this.maximumBytes = maximumBytes;
        }

        @Override
        public CapturedOutput call() throws IOException {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            boolean truncated = false;
            int read;
            while ((read = input.read(buffer)) != -1) {
                int remaining = maximumBytes - captured.size();
                if (remaining > 0) {
                    captured.write(buffer, 0, Math.min(remaining, read));
                }
                if (read > remaining) {
                    truncated = true;
                }
            }
            return new CapturedOutput(captured.toString(StandardCharsets.UTF_8), truncated);
        }
    }

    private static final class CapturedOutput {
        private final String text;
        private final boolean truncated;

        private CapturedOutput(String text, boolean truncated) {
            this.text = text;
            this.truncated = truncated;
        }
    }
}
