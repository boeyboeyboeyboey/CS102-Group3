package autograder.execution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ProcessRunner {
    public ProcessRunner() {
    }

    public static ProcessResult run(List<String> command, Path cwd, long timeoutMillis,
                                    int maxOutputBytes) throws IOException, InterruptedException {
        if (command == null || command.isEmpty() || timeoutMillis <= 0 || maxOutputBytes < 0) {
            throw new IllegalArgumentException("command, positive timeout, and output limit are required");
        }
        Path workingDirectory = cwd.toAbsolutePath().normalize();
        if (!Files.isDirectory(workingDirectory)) {
            throw new IOException("process working directory does not exist: " + workingDirectory);
        }

        long started = System.nanoTime();
        Process process = new ProcessBuilder(command).directory(workingDirectory.toFile()).start();
        process.getOutputStream().close();
        List<ProcessHandle> knownDescendants = new ArrayList<>();
        collectDescendants(process, knownDescendants);
        BoundedCapture stdout = new BoundedCapture(process.getInputStream(), maxOutputBytes);
        BoundedCapture stderr = new BoundedCapture(process.getErrorStream(), maxOutputBytes);
        Thread stdoutThread = new Thread(stdout, "process-stdout-capture");
        Thread stderrThread = new Thread(stderr, "process-stderr-capture");
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        boolean timedOut;
        try {
            timedOut = !process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            collectDescendants(process, knownDescendants);
            if (timedOut) {
                terminate(process, knownDescendants);
            }
        } catch (InterruptedException interrupted) {
            collectDescendants(process, knownDescendants);
            terminateUninterruptibly(process, knownDescendants);
            closeCaptureInputs(process, stdout, stderr);
            joinUninterruptibly(stdoutThread);
            joinUninterruptibly(stderrThread);
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        try {
            stdoutThread.join(2000);
            stderrThread.join(2000);
        } catch (InterruptedException interrupted) {
            terminateUninterruptibly(process, knownDescendants);
            closeCaptureInputs(process, stdout, stderr);
            joinUninterruptibly(stdoutThread);
            joinUninterruptibly(stderrThread);
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        if (stdoutThread.isAlive() || stderrThread.isAlive()) {
            collectDescendants(process, knownDescendants);
            boolean interrupted = terminateUninterruptibly(process, knownDescendants);
            closeCaptureInputs(process, stdout, stderr);
            interrupted |= joinUninterruptibly(stdoutThread);
            interrupted |= joinUninterruptibly(stderrThread);
            if (interrupted) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("interrupted while cleaning up process output");
            }
        }
        int exitCode = process.isAlive() ? -1 : process.exitValue();
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (stdout.failure != null) {
            throw stdout.failure;
        }
        if (stderr.failure != null) {
            throw stderr.failure;
        }
        return new ProcessResult(exitCode, timedOut, durationMillis,
                stdout.text(), stderr.text());
    }

    private static void terminate(Process process, List<ProcessHandle> knownDescendants)
            throws InterruptedException {
        collectDescendants(process, knownDescendants);
        destroyDescendants(knownDescendants, false);
        process.destroy();
        process.waitFor(200, TimeUnit.MILLISECONDS);
        collectDescendants(process, knownDescendants);
        destroyDescendants(knownDescendants, true);
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        process.waitFor(2, TimeUnit.SECONDS);
    }

    private static void collectDescendants(Process process, List<ProcessHandle> knownDescendants) {
        try {
            process.descendants().forEach(child -> {
                boolean known = false;
                for (ProcessHandle existing : knownDescendants) {
                    if (existing.pid() == child.pid()) {
                        known = true;
                        break;
                    }
                }
                if (!known) {
                    knownDescendants.add(child);
                }
            });
        } catch (RuntimeException ignored) {
        }
    }

    private static void destroyDescendants(List<ProcessHandle> descendants, boolean forcibly) {
        for (ProcessHandle child : descendants) {
            try {
                if (child.isAlive()) {
                    if (forcibly) {
                        child.destroyForcibly();
                    } else {
                        child.destroy();
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static boolean terminateUninterruptibly(Process process, List<ProcessHandle> descendants) {
        boolean interrupted = false;
        while (true) {
            try {
                terminate(process, descendants);
                break;
            } catch (InterruptedException exception) {
                interrupted = true;
                collectDescendants(process, descendants);
            }
        }
        return interrupted;
    }

    private static void closeCaptureInputs(Process process, BoundedCapture stdout,
                                           BoundedCapture stderr) {
        stdout.closeInput();
        stderr.closeInput();
        try {
            process.getInputStream().close();
        } catch (IOException ignored) {
        }
        try {
            process.getErrorStream().close();
        } catch (IOException ignored) {
        }
    }

    private static boolean joinUninterruptibly(Thread thread) {
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
        while (thread.isAlive() && System.nanoTime() < deadline) {
            try {
                thread.join(50);
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        return interrupted;
    }

    private static final class BoundedCapture implements Runnable {
        private final InputStream input;
        private final int limit;
        private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        private volatile boolean closing;
        private volatile IOException failure;

        private BoundedCapture(InputStream input, int limit) {
            this.input = input;
            this.limit = limit;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            try (InputStream stream = input) {
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    synchronized (captured) {
                        int remaining = limit - captured.size();
                        if (remaining > 0) {
                            captured.write(buffer, 0, Math.min(count, remaining));
                        }
                    }
                }
            } catch (IOException exception) {
                if (!closing) {
                    failure = exception;
                }
            }
        }

        private String text() {
            synchronized (captured) {
                return captured.toString(StandardCharsets.UTF_8);
            }
        }

        private void closeInput() {
            closing = true;
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }
}
