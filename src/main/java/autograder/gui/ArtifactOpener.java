package autograder.gui;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/** Opens an output with the system default application, or shows its path when that fails. */
final class ArtifactOpener {
    private static final int PATH_FIELD_COLUMNS = 40;

    private ArtifactOpener() {
    }

    static void open(Component parent, Path path) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
                return;
            }
        } catch (IOException | RuntimeException exception) {
            // fall through to showing the path
        }
        showPath(parent, path);
    }

    private static void showPath(Component parent, Path path) {
        JTextField field = new JTextField(path.toString(), PATH_FIELD_COLUMNS);
        field.setEditable(false);
        field.getAccessibleContext().setAccessibleName("File location");
        Object[] message = {"Couldn't open the file. It is at:", field};
        JOptionPane.showMessageDialog(parent, message, "Open", JOptionPane.ERROR_MESSAGE);
    }
}
