package autograder.gui;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Runs readiness checks off the event thread and drops results that are no longer current. */
final class ReadinessMonitor {
    static final int DEBOUNCE_MILLIS = 300;

    private final Supplier<GuiInputs> source;
    private final Consumer<Readiness> display;
    private final ReadinessChecker checker = new ReadinessChecker();
    private final Timer debounce;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "readiness-check");
        thread.setDaemon(true);
        return thread;
    });
    // only touched on the event thread
    private int generation;

    ReadinessMonitor(Supplier<GuiInputs> source, Consumer<Readiness> display) {
        this.source = source;
        this.display = display;
        this.debounce = new Timer(DEBOUNCE_MILLIS, event -> request(this.display));
        this.debounce.setRepeats(false);
    }

    /** Re-checks shortly after the last change. */
    void schedule() {
        debounce.restart();
    }

    /** Checks now and hands the result to callback unless a newer check has been requested. */
    void request(Consumer<Readiness> callback) {
        debounce.stop();
        int requested = ++generation;
        GuiInputs inputs = source.get();
        executor.submit(() -> {
            Readiness readiness = safeCheck(inputs);
            SwingUtilities.invokeLater(() -> {
                if (requested == generation) {
                    callback.accept(readiness);
                }
            });
        });
    }

    private Readiness safeCheck(GuiInputs inputs) {
        try {
            return checker.check(inputs);
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            return new Readiness(java.util.List.of(
                    FieldStatus.error(InputField.CONFIG, message)), null);
        }
    }
}
