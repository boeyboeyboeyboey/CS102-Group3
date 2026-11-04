package autograder.ui;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalCancellationTest {
    @Test
    void qInterruptsTheGradingThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException exception) {
                interrupted.countDown();
            }
        });
        worker.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));
        try (TerminalCancellation ignored = TerminalCancellation.start(
                new StringReader("help\nq\n"), worker,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8))) {
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        } finally {
            worker.interrupt();
            worker.join(1000);
        }
    }
}
