package autograder.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;

/** Entry point of the instructor GUI. */
public final class GuiLauncher {
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
        GuiPreferences preferences = GuiPreferences.system();
        StartupValues values = StartupValues.resolve(args, preferences.load(),
                Path.of("").toAbsolutePath(), StartupValues.codeLocation());
        SwingUtilities.invokeLater(() -> new MainWindow(values, preferences).setVisible(true));
    }
}
