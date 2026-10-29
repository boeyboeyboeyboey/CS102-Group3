package autograder.gui;

import autograder.RunOutcome;
import autograder.config.RunSettings;
import autograder.config.WorkerLimits;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** The instructor window: inputs with inline readiness, run controls and results. */
final class MainWindow extends JFrame {
    private static final int MARGIN = 12;
    private static final String ADVANCED_CLOSED = "▸ Advanced";
    private static final String ADVANCED_OPEN = "▾ Advanced";

    private final GuiPreferences preferences;
    private final ReadinessMonitor monitor;
    private final PathRow config;
    private final PathRow submissions;
    private final PathRow scoreSheet;
    private final PathRow output;
    private final JPanel advanced = new JPanel(new BorderLayout());
    private final JToggleButton advancedToggle = new JToggleButton(ADVANCED_CLOSED);
    private final ResultsPanel results = new ResultsPanel();
    private final WorkerChoice workerChoice = new WorkerChoice(WorkerLimits.system());
    private final RunBar bar;
    private GradingRunWorker worker;
    private RunSettings activeSettings;
    private boolean quitWhenFinished;

    MainWindow(StartupValues values, GuiPreferences preferences) {
        super("Autograder");
        this.preferences = preferences;
        this.monitor = new ReadinessMonitor(this::inputs, this::showReadiness);
        Runnable changed = monitor::schedule;
        submissions = new PathRow("Submissions", 'S', true, null, changed);
        scoreSheet = new PathRow("Score sheet", 'e', false,
                new FileNameExtensionFilter("CSV files", "csv"), changed);
        output = new PathRow("Output folder", 'O', true, null, changed);
        config = new PathRow("Config file", 'f', false,
                new FileNameExtensionFilter("Properties files", "properties"), changed);
        bar = new RunBar(this::startGrading, this::stopGrading, workerChoice.options());
        buildLayout();
        fill(values);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestClose();
            }
        });
        getRootPane().setDefaultButton(bar.gradeButton());
        pack();
        setLocationRelativeTo(null);
        monitor.request(this::showReadiness);
    }

    private void buildLayout() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        JPanel inputs = new JPanel();
        inputs.setLayout(new BoxLayout(inputs, BoxLayout.Y_AXIS));
        inputs.add(submissions);
        inputs.add(scoreSheet);
        inputs.add(output);
        inputs.add(Box.createVerticalStrut(MARGIN / 2));
        advancedToggle.setMnemonic('A');
        advancedToggle.setBorderPainted(false);
        advancedToggle.setContentAreaFilled(false);
        advancedToggle.setFocusPainted(true);
        advancedToggle.addActionListener(event -> setAdvanced(advancedToggle.isSelected()));
        JPanel toggleRow = new JPanel(new BorderLayout());
        toggleRow.add(advancedToggle, BorderLayout.WEST);
        inputs.add(toggleRow);
        advanced.add(config, BorderLayout.CENTER);
        advanced.setVisible(false);
        inputs.add(advanced);
        content.add(inputs, BorderLayout.NORTH);
        content.add(results, BorderLayout.CENTER);
        content.add(bar, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void fill(StartupValues values) {
        config.setPath(values.config());
        submissions.setPath(values.submissions());
        scoreSheet.setPath(values.scoreSheet());
        output.setPath(values.output());
        if (values.configError() != null) {
            setAdvanced(true);
        }
    }

    private void setAdvanced(boolean open) {
        advancedToggle.setSelected(open);
        advancedToggle.setText(open ? ADVANCED_OPEN : ADVANCED_CLOSED);
        advanced.setVisible(open);
        pack();
    }

    private GuiInputs inputs() {
        return new GuiInputs(config.path(), submissions.path(), scoreSheet.path(), output.path());
    }

    private void showReadiness(Readiness readiness) {
        PathRow[] rows = {config, submissions, scoreSheet, output};
        InputField[] fields = {InputField.CONFIG, InputField.SUBMISSIONS,
                InputField.SCORE_SHEET, InputField.OUTPUT};
        for (int index = 0; index < rows.length; index++) {
            rows[index].showStatus(readiness.status(fields[index]).orElse(null));
        }
        if (readiness.status(InputField.CONFIG).filter(status -> !status.ok()).isPresent()) {
            setAdvanced(true);
        }
        showWorkers(readiness.settings());
    }

    private void showWorkers(RunSettings settings) {
        if (settings == null) {
            return;
        }
        WorkerChoice.Update update = workerChoice.settingsLoaded(settings.execution().workers());
        update.selection().ifPresent(bar::selectWorkers);
        bar.setStatus(update.warning().orElse(""));
    }

    private void startGrading() {
        setInputsEnabled(false);
        results.setVisible(false);
        bar.setChecking();
        monitor.request(this::onFinalCheck);
    }

    private void onFinalCheck(Readiness readiness) {
        showReadiness(readiness);
        if (!readiness.ready()) {
            setInputsEnabled(true);
            bar.setIdle();
            bar.setStatus(OutcomeText.FIX_FIELDS);
            focusFirstProblem(readiness);
            return;
        }
        preferences.save(inputs());
        activeSettings = readiness.settings().withWorkers(bar.selectedWorkers());
        bar.setRunning();
        worker = new GradingRunWorker(activeSettings, bar::showProgress, this::onFinished);
        worker.execute();
    }

    private void focusFirstProblem(Readiness readiness) {
        List<FieldStatus> bad = readiness.statuses().stream().filter(status -> !status.ok()).toList();
        if (bad.isEmpty()) {
            return;
        }
        switch (bad.get(0).field()) {
            case CONFIG -> config.focusField();
            case SUBMISSIONS -> submissions.focusField();
            case SCORE_SHEET -> scoreSheet.focusField();
            default -> output.focusField();
        }
    }

    private void stopGrading() {
        if (worker != null) {
            bar.setStopping();
            worker.stop();
        }
    }

    private void onFinished(RunOutcome outcome) {
        List<String> problems = worker == null ? List.of() : worker.reportProblems();
        worker = null;
        if (quitWhenFinished) {
            exit();
            return;
        }
        bar.setIdle();
        bar.setStatus("");
        results.show(outcome, activeSettings, problems);
        setInputsEnabled(true);
        pack();
    }

    private void requestClose() {
        if (worker == null) {
            exit();
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Grading is running. Stop and quit?",
                "Autograder", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION && worker != null) {
            quitWhenFinished = true;
            stopGrading();
        } else if (answer == JOptionPane.YES_OPTION) {
            exit();
        }
    }

    private void exit() {
        dispose();
        System.exit(0);
    }

    private void setInputsEnabled(boolean enabled) {
        for (PathRow row : List.of(config, submissions, scoreSheet, output)) {
            row.setInputEnabled(enabled);
        }
        advancedToggle.setEnabled(enabled);
    }
}
