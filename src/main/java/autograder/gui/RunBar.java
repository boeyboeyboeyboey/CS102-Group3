package autograder.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** Bottom bar: status text, progress bar and the Grade and Stop buttons. */
final class RunBar extends JPanel {
    private static final int GAP = 8;

    private final JLabel status = new JLabel(" ");
    private final JProgressBar progress = new JProgressBar();
    private final JButton grade = new JButton("Grade");
    private final JButton stop = new JButton("Stop");

    private final JComboBox<Integer> workers = new JComboBox<>();

    RunBar(Runnable onGrade, Runnable onStop, List<Integer> workerOptions) {
        super(new BorderLayout(GAP, GAP));
        setBorder(BorderFactory.createEmptyBorder(GAP, 0, 0, 0));
        grade.setMnemonic('G');
        stop.setMnemonic('p');
        grade.addActionListener(event -> onGrade.run());
        stop.addActionListener(event -> onStop.run());
        workerOptions.forEach(workers::addItem);
        JLabel workersLabel = new JLabel("Workers");
        workersLabel.setDisplayedMnemonic('W');
        workersLabel.setLabelFor(workers);
        progress.setVisible(false);
        progress.setStringPainted(false);
        stop.setVisible(false);
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.add(status, BorderLayout.NORTH);
        text.add(progress, BorderLayout.SOUTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0));
        buttons.add(workersLabel);
        buttons.add(workers);
        buttons.add(stop);
        buttons.add(grade);
        add(text, BorderLayout.CENTER);
        add(buttons, BorderLayout.EAST);
    }

    JButton gradeButton() {
        return grade;
    }

    int selectedWorkers() {
        return (Integer) workers.getSelectedItem();
    }

    void selectWorkers(int count) {
        workers.setSelectedItem(count);
    }

    void setStatus(String text) {
        status.setText(text == null || text.isEmpty() ? " " : text);
    }

    void setIdle() {
        progress.setVisible(false);
        stop.setVisible(false);
        grade.setEnabled(true);
        workers.setEnabled(true);
    }

    void setChecking() {
        grade.setEnabled(false);
        workers.setEnabled(false);
        setStatus(OutcomeText.CHECKING);
    }

    void setRunning() {
        grade.setEnabled(false);
        workers.setEnabled(false);
        stop.setEnabled(true);
        stop.setVisible(true);
        progress.setIndeterminate(true);
        progress.setVisible(true);
        setStatus(OutcomeText.CHECKING);
    }

    void setStopping() {
        stop.setEnabled(false);
        progress.setIndeterminate(true);
        setStatus(OutcomeText.STOPPING);
    }

    void showProgress(GradingRunWorker.Update update) {
        setStatus(update.text());
        if (update.total() > 0) {
            progress.setIndeterminate(false);
            progress.setMaximum(update.total());
            progress.setValue(update.completed());
        } else {
            progress.setIndeterminate(true);
        }
    }
}
