package autograder.ui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

public final class TerminalCancellation implements AutoCloseable {
    private volatile boolean active = true;

    private TerminalCancellation(Reader input, Thread worker, PrintStream output) {
        Thread monitor = new Thread(() -> listen(input, worker), "terminal-cancellation");
        monitor.setDaemon(true);
        output.println("Type q then Enter to cancel this run.");
        monitor.start();
    }

    public static TerminalCancellation start(Console console, Thread worker, PrintStream output) {
        return console == null ? null : new TerminalCancellation(console.reader(), worker, output);
    }

    static TerminalCancellation start(Reader input, Thread worker, PrintStream output) {
        return new TerminalCancellation(input, worker, output);
    }

    private void listen(Reader input, Thread worker) {
        try {
            BufferedReader lines = new BufferedReader(input);
            String line;
            while (active && (line = lines.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("q") && active) {
                    worker.interrupt();
                    return;
                }
            }
        } catch (IOException ignored) {
            // a closed terminal must not stop grading
        }
    }

    @Override
    public void close() {
        active = false;
    }
}
