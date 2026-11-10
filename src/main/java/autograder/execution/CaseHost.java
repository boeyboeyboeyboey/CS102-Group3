package autograder.execution;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Child jvm that runs the cases of one question part. Each case gets a fresh class loader, its own
 * output files, empty stdin and restored system properties, locale and time zone, while the parent
 * enforces the time limit and replaces the host after anything abnormal.
 */
public final class CaseHost {
    static final String READY = "READY";
    static final String STARTED = "STARTED";
    static final String DONE = "DONE";
    static final String RUN = "RUN";
    static final String FIELD_SEPARATOR = "\t";
    static final String REPLY_SEPARATOR = " ";
    static final String STDOUT_FILE = "case-stdout.txt";
    static final String STDERR_FILE = "case-stderr.txt";
    private static final int UNCAUGHT_EXIT_STATUS = 1;
    private static final int USAGE_EXIT_STATUS = 2;
    private static final String CASE_THREAD_NAME = "main";
    private static final String CASE_GROUP_NAME = "case";
    private static final String MAIN_METHOD_NAME = "main";

    private final String token;
    private final URL[] classpath;
    private final int maxOutputBytes;
    private final PrintStream protocol;
    private final PrintStream hostErrors;

    private CaseHost(String token, URL[] classpath, int maxOutputBytes,
                     PrintStream protocol, PrintStream hostErrors) {
        this.token = token;
        this.classpath = classpath.clone();
        this.maxOutputBytes = maxOutputBytes;
        this.protocol = protocol;
        this.hostErrors = hostErrors;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: CaseHost <token> <classes directory> <max output bytes>");
            System.exit(USAGE_EXIT_STATUS);
        }
        URL[] classpath = {Path.of(args[1]).toUri().toURL()};
        CaseHost host = new CaseHost(args[0], classpath, Integer.parseInt(args[2]),
                System.out, System.err);
        InputStream commands = System.in;
        System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
        host.serve(new BufferedReader(new InputStreamReader(commands, StandardCharsets.UTF_8)));
    }

    private void serve(BufferedReader commands) throws IOException {
        send(READY);
        String line = commands.readLine();
        while (line != null) {
            CaseCommand command = CaseCommand.parse(line);
            send(STARTED);
            CaseOutcome outcome = runCase(command);
            send(DONE + REPLY_SEPARATOR + outcome.exitStatus() + REPLY_SEPARATOR + outcome.threadsLeft());
            line = commands.readLine();
        }
    }

    private void send(String message) {
        protocol.println(token + REPLY_SEPARATOR + message);
        protocol.flush();
    }

    private CaseOutcome runCase(CaseCommand command) throws IOException {
        Path directory = command.caseDirectory();
        SavedGlobals saved = SavedGlobals.capture();
        try (OutputStream out = new BoundedFileOutput(directory.resolve(STDOUT_FILE), maxOutputBytes);
             OutputStream err = new BoundedFileOutput(directory.resolve(STDERR_FILE), maxOutputBytes);
             PrintStream caseOut = new PrintStream(out, true, StandardCharsets.UTF_8);
             PrintStream caseErr = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            System.setOut(caseOut);
            System.setErr(caseErr);
            System.setIn(new ByteArrayInputStream(new byte[0]));
            return execute(command);
        } finally {
            System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
            System.setErr(hostErrors);
            saved.restore();
        }
    }

    private CaseOutcome execute(CaseCommand command) throws IOException {
        ThreadGroup group = new ThreadGroup(CASE_GROUP_NAME);
        AtomicInteger exitStatus = new AtomicInteger();
        try (URLClassLoader loader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader())) {
            Thread caseThread = new Thread(group,
                    () -> exitStatus.set(invokeMain(loader, command.runnerClass(), command.arguments())),
                    CASE_THREAD_NAME);
            caseThread.setContextClassLoader(loader);
            caseThread.start();
            joinUninterruptibly(caseThread);
            // a separate jvm would also wait for non-daemon threads before exiting
            awaitNonDaemonThreads(group);
            boolean threadsLeft = !liveThreads(group).isEmpty();
            return new CaseOutcome(exitStatus.get(), threadsLeft);
        }
    }

    private static int invokeMain(ClassLoader loader, String runnerClass, String[] arguments) {
        Method main;
        try {
            Class<?> runner = Class.forName(runnerClass, false, loader);
            main = runner.getMethod(MAIN_METHOD_NAME, String[].class);
            if (!Modifier.isStatic(main.getModifiers())) {
                throw new NoSuchMethodException("main is not static");
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            System.err.println("Error: Could not find or load main class " + runnerClass);
            System.err.println("Caused by: " + exception);
            return UNCAUGHT_EXIT_STATUS;
        }
        try {
            main.invoke(null, (Object) arguments);
            return 0;
        } catch (InvocationTargetException exception) {
            reportUncaught(exception.getCause());
        } catch (Throwable throwable) {
            reportUncaught(throwable);
        }
        return UNCAUGHT_EXIT_STATUS;
    }

    private static void reportUncaught(Throwable throwable) {
        System.err.print("Exception in thread \"" + CASE_THREAD_NAME + "\" ");
        throwable.printStackTrace(System.err);
    }

    private static void awaitNonDaemonThreads(ThreadGroup group) {
        Thread waiting = firstNonDaemon(group);
        while (waiting != null) {
            joinUninterruptibly(waiting);
            waiting = firstNonDaemon(group);
        }
    }

    private static Thread firstNonDaemon(ThreadGroup group) {
        for (Thread thread : liveThreads(group)) {
            if (!thread.isDaemon()) {
                return thread;
            }
        }
        return null;
    }

    private static List<Thread> liveThreads(ThreadGroup group) {
        Thread[] threads = new Thread[group.activeCount() + 16];
        int count = group.enumerate(threads, true);
        List<Thread> live = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            if (threads[index].isAlive()) {
                live.add(threads[index]);
            }
        }
        return live;
    }

    private static void joinUninterruptibly(Thread thread) {
        boolean joined = false;
        while (!joined) {
            try {
                thread.join();
                joined = true;
            } catch (InterruptedException ignored) {
                // the parent stops the whole host when a case runs too long
            }
        }
    }

    private record CaseOutcome(int exitStatus, boolean threadsLeft) {
    }

    record CaseCommand(String runnerClass, Path caseDirectory, String[] arguments) {
        static CaseCommand parse(String line) {
            String[] fields = line.split(FIELD_SEPARATOR, -1);
            if (fields.length < 3 || !fields[0].equals(RUN)) {
                throw new IllegalArgumentException("malformed case host command");
            }
            String[] arguments = new String[fields.length - 3];
            for (int index = 3; index < fields.length; index++) {
                arguments[index - 3] = decode(fields[index]);
            }
            return new CaseCommand(decode(fields[1]), Path.of(decode(fields[2])), arguments);
        }

        private static String decode(String field) {
            return URLDecoder.decode(field, StandardCharsets.UTF_8);
        }
    }

    private record SavedGlobals(Properties properties, Locale locale, TimeZone timeZone) {
        static SavedGlobals capture() {
            Properties copy = new Properties();
            copy.putAll(System.getProperties());
            return new SavedGlobals(copy, Locale.getDefault(), TimeZone.getDefault());
        }

        void restore() {
            Properties copy = new Properties();
            copy.putAll(properties);
            System.setProperties(copy);
            Locale.setDefault(locale);
            TimeZone.setDefault(timeZone);
        }
    }

    private static final class BoundedFileOutput extends OutputStream {
        private final FileOutputStream file;
        private final int limit;
        private int written;

        private BoundedFileOutput(Path path, int limit) throws IOException {
            this.file = new FileOutputStream(path.toFile());
            this.limit = limit;
        }

        @Override
        public synchronized void write(int value) throws IOException {
            if (written < limit) {
                file.write(value);
                written++;
            }
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
            int accepted = Math.min(length, limit - written);
            if (accepted > 0) {
                file.write(bytes, offset, accepted);
                written += accepted;
            }
        }

        @Override
        public synchronized void close() throws IOException {
            file.close();
        }
    }
}
