package autograder.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalDashboardTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void plainModeShowsProgressWithoutTerminalControlCharacters() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalDashboard dashboard = new TerminalDashboard(
                new PrintStream(bytes, true, StandardCharsets.UTF_8),
                DashboardSettings.Mode.PLAIN, true, Map.of());
        dashboard.onProgress("grading", "student\u001b[31m.zip", 0, 2);
        dashboard.onProgress("grading", "student\u001b[31m.zip", 1, 2);
        dashboard.onProgress("grading", "student\u001b[31m.zip", 1, 2);

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(2, output.lines().count());
        assertTrue(output.contains("1/2 (50%) done"));
        assertFalse(output.contains("\u001b"));
        assertTrue(output.contains("student?[31m.zip"));
    }

    @Test
    void autoModeRespectsNonInteractiveAndNoColorTerminals() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        new TerminalDashboard(output, DashboardSettings.Mode.AUTO, false, Map.of())
                .onProgress("ingestion", "one.zip", 1, 1);
        new TerminalDashboard(output, DashboardSettings.Mode.AUTO, true, Map.of("NO_COLOR", "1"))
                .onProgress("ingestion", "one.zip", 1, 1);
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001b"));
    }

    @Test
    void forcedAnsiModeColorsCompletedProgress() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new TerminalDashboard(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                DashboardSettings.Mode.ANSI, false, Map.of())
                .onProgress("reporting", "ready", 1, 1);
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("\u001b[32m"));
    }

    @Test
    void modeIsConfigurableAndInvalidValuesAreRejected() throws Exception {
        Path file = temporaryDirectory.resolve("dashboard.properties");
        Files.writeString(file, "dashboard.mode=plain\n");
        assertEquals(DashboardSettings.Mode.PLAIN, DashboardSettings.load(file).mode());
        Files.writeString(file, "dashboard.mode=unknown\n");
        assertThrows(IllegalArgumentException.class, () -> DashboardSettings.load(file));
    }
}
