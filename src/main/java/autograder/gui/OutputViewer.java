package autograder.gui;

import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.Window;
import java.io.IOException;

/** One window that shows a run's reports inside the GUI, beside the results. */
final class OutputViewer extends JDialog {
    private static final Dimension SIZE = new Dimension(960, 640);

    OutputViewer(Window owner) {
        super(owner, ModalityType.MODELESS);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(SIZE);
        setLocationRelativeTo(owner);
    }

    /** Shows the artifact here, or in the system's application when it can't be read. */
    void display(Artifact artifact) {
        JComponent content;
        try {
            content = artifact.view() == Artifact.View.FEEDBACK
                    ? new FeedbackView(artifact.path())
                    : new CsvView(CsvTableModel.load(artifact.path()));
        } catch (IOException | RuntimeException exception) {
            ArtifactOpener.open(getOwner(), artifact.path());
            return;
        }
        setContentPane(content);
        setTitle(artifact.label());
        revalidate();
        repaint();
        setVisible(true);
        toFront();
    }
}
