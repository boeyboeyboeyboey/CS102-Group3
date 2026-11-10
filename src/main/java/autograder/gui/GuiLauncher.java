package autograder.gui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;

/** Entry point of the instructor GUI. */
public final class GuiLauncher {
    private static final String CRASH_MESSAGE =
            "Something went wrong and the last action could not be completed. "
                    + "You can keep working or close and reopen the grader.";

    private GuiLauncher() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The GUI needs a display. Run the command-line version: "
                    + "java -jar <jar> <config.properties>");
            System.exit(1);
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            // keep the default look and feel
        }
        installCrashHandler();
        Brand.install();
        GuiPreferences preferences = GuiPreferences.system();
        StartupValues values = StartupValues.resolve(args, preferences.load(),
                Path.of("").toAbsolutePath(), StartupValues.codeLocation());
        SwingUtilities.invokeLater(() -> new MainWindow(values, preferences).setVisible(true));
    }

    private static void installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    JOptionPane.showMessageDialog(null, CRASH_MESSAGE, Brand.NAME,
                            JOptionPane.ERROR_MESSAGE);
                } catch (RuntimeException ignored) {
                    // the dialog itself failed; the stack trace is already on stderr
                }
            });
        });
    }
}
