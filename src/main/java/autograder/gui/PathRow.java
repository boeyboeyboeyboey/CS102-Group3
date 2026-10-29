package autograder.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** A labelled path field with a chooser button and an inline status line. */
final class PathRow extends JPanel {
    private static final int LABEL_WIDTH = 110;
    private static final int FIELD_COLUMNS = 36;
    private static final int GAP = 6;
    private static final Color OK_COLOR = new Color(0x1B5E20);
    private static final Color ERROR_COLOR = new Color(0xB00020);

    private final JLabel label = new JLabel();
    private final JTextField field = new JTextField(FIELD_COLUMNS);
    private final JButton choose = new JButton("Choose…");
    private final JLabel status = new JLabel(" ");
    private final boolean directories;
    private final FileNameExtensionFilter filter;

    PathRow(String text, char mnemonic, boolean directories, FileNameExtensionFilter filter,
            Runnable onChange) {
        super(new GridBagLayout());
        this.directories = directories;
        this.filter = filter;
        label.setText(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
        choose.getAccessibleContext().setAccessibleName("Choose " + text.toLowerCase());
        choose.addActionListener(event -> chooseFile());
        field.getDocument().addDocumentListener(new ChangeListener(onChange));
        layoutParts();
    }

    Path path() {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Path.of(text);
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    void setPath(Path path) {
        field.setText(path == null ? "" : path.toString());
    }

    void showStatus(FieldStatus fieldStatus) {
        if (fieldStatus == null) {
            status.setText(" ");
            field.getAccessibleContext().setAccessibleDescription(null);
            return;
        }
        String message = fieldStatus.message();
        String text;
        if (fieldStatus.ok()) {
            text = message.isEmpty() ? " " : "✓ " + message;
        } else {
            text = "⚠ " + message;
        }
        status.setForeground(fieldStatus.ok() ? OK_COLOR : ERROR_COLOR);
        status.setText(text);
        field.getAccessibleContext().setAccessibleDescription(message);
    }

    void setInputEnabled(boolean enabled) {
        field.setEnabled(enabled);
        choose.setEnabled(enabled);
    }

    void focusField() {
        field.requestFocusInWindow();
    }

    private void layoutParts() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(GAP / 2, 0, 0, GAP);
        add(label, c);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(field, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(GAP / 2, 0, 0, 0);
        add(choose, c);
        c.gridy = 1;
        c.gridx = 1;
        c.insets = new Insets(0, 2, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        add(status, c);
        setBorder(BorderFactory.createEmptyBorder());
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(directories
                ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        if (filter != null) {
            chooser.setFileFilter(filter);
        }
        Path current = path();
        if (current != null) {
            chooser.setSelectedFile(current.toFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static final class ChangeListener implements DocumentListener {
        private final Runnable onChange;

        ChangeListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            onChange.run();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            onChange.run();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            onChange.run();
        }
    }
}
