package autograder.execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Parent side of one {@link CaseHost} jvm: starts it, sends cases and enforces each case deadline. */
final class CaseHostProcess implements AutoCloseable {
    private static final String END_OF_STREAM = "\u0000end";
    private static final long STREAM_DRAIN_MILLIS = 500;
    private static final String REPLY_THREAD_NAME = "case-host-replies";
    private static final String ERROR_THREAD_NAME = "case-host-errors";

    private final Process process;
    private final String token;
    private final BufferedWriter commands;
    private final BlockingQueue<String> replies = new LinkedBlockingQueue<>();
    private final BoundedText hostErrors;
    private final Thread errorReader;

    private CaseHostProcess(Process process, String token, int maxOutputBytes) {
        this.process = process;
        this.token = token;
        this.commands = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),
                StandardCharsets.UTF_8));
        this.hostErrors = new BoundedText(maxOutputBytes);
        startDaemon(this::readReplies, REPLY_THREAD_NAME);
        this.errorReader = startDaemon(this::readErrors, ERROR_THREAD_NAME);
    }

    static CaseHostProcess start(String javaExecutable, Path classesDirectory, Path workingDirectory,
                                 int maxOutputBytes, long startupTimeoutMillis)
            throws IOException, InterruptedException {
        String token = UUID.randomUUID().toString();
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-classpath");
        command.add(hostClasspath().toString());
        command.add(CaseHost.class.getName());
        command.add(token);
        command.add(classesDirectory.toAbsolutePath().normalize().toString());
        command.add(Integer.toString(maxOutputBytes));
        Process process = new ProcessBuilder(command).directory(workingDirectory.toFile()).start();
        CaseHostProcess host = new CaseHostProcess(process, token, maxOutputBytes);
        try {
            host.awaitReady(startupTimeoutMillis);
        } catch (IOException | InterruptedException | RuntimeException exception) {
            host.close();
            throw exception;
        }
        return host;
    }

    HostCaseOutcome run(String runnerClass, Path caseDirectory, List<String> arguments,
                        long timeoutMillis) throws IOException, InterruptedException {
        hostErrors.clear();
        long started = System.nanoTime();
        send(runnerClass, caseDirectory, arguments);
        long deadline = started + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        String reply = nextReply(deadline);
        if (reply != null && reply.equals(END_OF_STREAM)) {
            throw new HostFailureException("case host exited before accepting the case");
        }
        if (reply != null && !reply.equals(CaseHost.STARTED)) {
            close();
            throw new HostFailureException("unexpected case host reply: " + reply);
        }
        if (reply != null) {
            reply = nextReply(deadline);
        }
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (reply == null) {
            close();
            return new HostCaseOutcome(-1, true, false, true, duration, drainedErrors());
        }
        if (reply.equals(END_OF_STREAM)) {
            // student code ended the jvm, for example through System.exit
            int exitCode = exitCodeAfterEnd();
            return new HostCaseOutcome(exitCode, false, false, true, duration, drainedErrors());
        }
        String[] fields = reply.split(CaseHost.REPLY_SEPARATOR);
        if (fields.length != 3 || !fields[0].equals(CaseHost.DONE)) {
            close();
            throw new HostFailureException("unexpected case host reply: " + reply);
        }
        try {
            int exitCode = Integer.parseInt(fields[1]);
            boolean threadsLeft = Boolean.parseBoolean(fields[2]);
            return new HostCaseOutcome(exitCode, false, threadsLeft, false, duration,
                    hostErrors.text());
        } catch (NumberFormatException exception) {
            close();
            throw new HostFailureException("unexpected case host reply: " + reply, exception);
        }
    }

    private int exitCodeAfterEnd() throws InterruptedException {
        if (process.waitFor(STREAM_DRAIN_MILLIS, TimeUnit.MILLISECONDS)) {
            close();
            return process.exitValue();
        }
        close();
        return -1;
    }

    @Override
    public void close() {
        try {
            commands.close();
        } catch (IOException ignored) {
            // the host may already have exited
        }
        if (process.isAlive()) {
            ProcessRunner.stopTree(process);
        }
    }

    private void awaitReady(long startupTimeoutMillis) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(startupTimeoutMillis);
        String reply = nextReply(deadline);
        if (reply == null) {
            throw new HostFailureException("case host did not start within "
                    + startupTimeoutMillis + " ms");
        }
        if (!reply.equals(CaseHost.READY)) {
            process.waitFor(STREAM_DRAIN_MILLIS, TimeUnit.MILLISECONDS);
            throw new HostFailureException("case host failed to start: " + drainedErrors());
        }
    }

    private void send(String runnerClass, Path caseDirectory, List<String> arguments)
            throws IOException {
        // a host that died after the previous case, for example killed by the os, never saw this case
        if (!process.isAlive() || END_OF_STREAM.equals(replies.peek())) {
            throw new HostFailureException("case host exited before the case was sent");
        }
        try {
            writeCommand(runnerClass, caseDirectory, arguments);
        } catch (IOException exception) {
            throw new HostFailureException("could not send the case to the case host: "
                    + exception.getMessage(), exception);
        }
    }

    private void writeCommand(String runnerClass, Path caseDirectory, List<String> arguments)
            throws IOException {
        StringBuilder line = new StringBuilder(CaseHost.RUN);
        line.append(CaseHost.FIELD_SEPARATOR).append(encode(runnerClass));
        line.append(CaseHost.FIELD_SEPARATOR).append(encode(caseDirectory.toString()));
        for (String argument : arguments) {
            line.append(CaseHost.FIELD_SEPARATOR).append(encode(argument));
        }
        commands.write(line.toString());
        commands.newLine();
        commands.flush();
    }

    private String nextReply(long deadlineNanos) throws InterruptedException {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            return replies.poll();
        }
        return replies.poll(remaining, TimeUnit.NANOSECONDS);
    }

    private String drainedErrors() throws InterruptedException {
        errorReader.join(STREAM_DRAIN_MILLIS);
        return hostErrors.text();
    }

    private void readReplies() {
        String prefix = token + CaseHost.REPLY_SEPARATOR;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                // student code writing straight to the file descriptor cannot fake a reply
                if (line.startsWith(prefix)) {
                    replies.add(line.substring(prefix.length()));
                }
                line = reader.readLine();
            }
        } catch (IOException ignored) {
            // treated as end of stream below
        }
        replies.add(END_OF_STREAM);
    }

    private void readErrors() {
        byte[] buffer = new byte[8192];
        try (InputStream errors = process.getErrorStream()) {
            int count = errors.read(buffer);
            while (count != -1) {
                hostErrors.append(buffer, count);
                count = errors.read(buffer);
            }
        } catch (IOException ignored) {
            // host errors are diagnostic only
        }
    }

    private static Thread startDaemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Path hostClasspath() throws IOException {
        try {
            return Path.of(CaseHost.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | RuntimeException exception) {
            throw new IOException("cannot locate the grader classes for the case host", exception);
        }
    }

    // the host jvm failed rather than the case; the runner retries the case on a fresh host,
    // while other i/o errors (such as a missing java executable) stay infrastructure errors
    static final class HostFailureException extends IOException {
        HostFailureException(String message) {
            super(message);
        }

        HostFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    record HostCaseOutcome(int exitCode, boolean timedOut, boolean threadsLeft, boolean hostEnded,
                           long durationMillis, String hostErrors) {
        boolean needsNewHost() {
            return timedOut || threadsLeft || hostEnded || exitCode != 0;
        }
    }

    private static final class BoundedText {
        private final int limit;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private BoundedText(int limit) {
            this.limit = limit;
        }

        synchronized void append(byte[] buffer, int count) {
            int accepted = Math.min(count, limit - bytes.size());
            if (accepted > 0) {
                bytes.write(buffer, 0, accepted);
            }
        }

        synchronized void clear() {
            bytes.reset();
        }

        synchronized String text() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
