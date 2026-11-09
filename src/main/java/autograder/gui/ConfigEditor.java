package autograder.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Edits the config file's text in place, so comments and layout are kept. */
final class ConfigEditor extends JDialog {
    private static final String NOTICE = "Saved changes apply to the next run.";
    private static final Dimension SIZE = new Dimension(820, 620);
    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final int GAP = 8;

    private final Path file;
    private final String original;
    private final Consumer<ConfigFileStore.Saved> onSaved;
    private final JTextArea text;
    private final JTextArea problem = new JTextArea();

    private ConfigEditor(Window owner, Path file, String original, Consumer<ConfigFileStore.Saved> onSaved) {
        super(owner, "Edit " + file.getFileName(), ModalityType.APPLICATION_MODAL);
        this.file = file;
        this.original = original;
        this.onSaved = onSaved;
        this.text = new JTextArea(original);
        buildLayout();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel();
            }
        });
        setSize(SIZE);
        setLocationRelativeTo(owner);
    }

    /** Opens the editor, or explains why the file can't be edited. */
    static void open(Window owner, Path file, Consumer<ConfigFileStore.Saved> onSaved) {
        if (file == null) {
            JOptionPane.showMessageDialog(owner, "Choose a config file first.", "Edit config",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            new ConfigEditor(owner, file, ConfigFileStore.read(file), onSaved).setVisible(true);
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(owner, "Couldn't open " + file, "Edit config",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildLayout() {
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
        text.setCaretPosition(0);
        text.getAccessibleContext().setAccessibleName("Config file text");
        problem.setEditable(false);
        problem.setLineWrap(true);
        problem.setWrapStyleWord(true);
        problem.setOpaque(false);
        problem.setForeground(ERROR_COLOR);
        problem.getAccessibleContext().setAccessibleName("Save problem");
        JButton save = new BrandButton("Save", true);
        save.addActionListener(event -> save());
        JButton cancel = new BrandButton("Cancel", false);
        cancel.addActionListener(event -> cancel());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0));
        actions.add(cancel);
        actions.add(save);
        JPanel bottom = new JPanel(new BorderLayout(0, GAP));
        bottom.add(problem, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, GAP));
        content.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        content.add(new JLabel(NOTICE), BorderLayout.NORTH);
        content.add(new JScrollPane(text), BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void save() {
        try {
            ConfigFileStore.Saved saved = ConfigFileStore.save(file, original, text.getText());
            dispose();
            onSaved.accept(saved);
        } catch (IOException | RuntimeException exception) {
            String reason = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            problem.setText("Not saved: " + reason);
        }
    }

    private void cancel() {
        boolean changed = !text.getText().equals(original);
        if (!changed || JOptionPane.showConfirmDialog(this, "Discard your changes?", "Edit config",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            dispose();
        }
    }
}
