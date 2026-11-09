package autograder.gui;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** A list of students beside the selected student's formatted feedback. */
final class FeedbackView extends JPanel {
    static final String EMPTY = "No feedback files were written for this run.";
    private static final String EXTENSION = ".md";
    private static final int LIST_WIDTH = 220;

    private final JEditorPane page = new JEditorPane();

    FeedbackView(Path directory) throws IOException {
        super(new BorderLayout());
        List<Path> files = feedbackFiles(directory);
        if (files.isEmpty()) {
            add(new JLabel(EMPTY, SwingConstants.CENTER), BorderLayout.CENTER);
            return;
        }
        page.setEditable(false);
        page.setContentType("text/html");
        page.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        page.getAccessibleContext().setAccessibleName("Student feedback");
        JList<String> students = new JList<>(files.stream().map(FeedbackView::studentName)
                .toArray(String[]::new));
        students.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        students.getAccessibleContext().setAccessibleName("Students");
        students.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && students.getSelectedIndex() >= 0) {
                show(files.get(students.getSelectedIndex()));
            }
        });
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(students), new JScrollPane(page));
        split.setDividerLocation(LIST_WIDTH);
        add(split, BorderLayout.CENTER);
        students.setSelectedIndex(0);
    }

    static List<Path> feedbackFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(EXTENSION))
                    .sorted().toList();
        }
    }

    private static String studentName(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - EXTENSION.length());
    }

    private void show(Path file) {
        try {
            page.setText(MarkdownHtml.render(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            page.setText(MarkdownHtml.render("Couldn't read this feedback file."));
        }
        page.setCaretPosition(0);
    }
}
