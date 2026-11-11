package autograder.gui;

import autograder.RunOutcome;
import autograder.config.RunSettings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;

/** Completion view: headline, per-student table and one button per output. */
final class ResultsPanel extends JPanel {
    private static final int GAP = 8;
    private static final int TABLE_HEIGHT = 200;
    private static final int TABLE_WIDTH = 520;
    private static final int MAX_SCORE_DIGITS = 2;
    private static final int BUTTON_COLUMNS = 5;

    private final JTextArea headline = new JTextArea();
    private final JTextArea problems = new JTextArea();
    private final JTable table = new JTable();
    private final JScrollPane tableScroll = new JScrollPane(table);
    private final JPanel buttons = new JPanel(new ResponsiveButtonLayout(GAP, BUTTON_COLUMNS));
    private OutputViewer viewer;

    ResultsPanel() {
        super(new BorderLayout(0, GAP));
        setBorder(BorderFactory.createEmptyBorder(GAP, 0, 0, 0));
        configureHeadline();
        configureTable();
        configureProblems();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(headline, BorderLayout.NORTH);
        top.add(problems, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setVisible(false);
    }

    void show(RunOutcome outcome, RunSettings settings) {
        show(outcome, settings, List.of());
    }

    void show(RunOutcome outcome, RunSettings settings, List<String> reportProblems) {
        show(OutcomeText.headline(outcome), outcome, settings, reportProblems);
    }

    /** Shows the outcome with how long the run took under the headline. */
    void show(RunOutcome outcome, RunSettings settings, List<String> reportProblems, long elapsedMillis) {
        show(OutcomeText.headline(outcome, elapsedMillis), outcome, settings, reportProblems);
    }

    private void show(String headlineText, RunOutcome outcome, RunSettings settings,
                      List<String> reportProblems) {
        if (viewer != null) {
            viewer.setVisible(false);
        }
        headline.setText(headlineText);
        String problemText = OutcomeText.reportProblems(reportProblems);
        problems.setText(problemText);
        problems.setVisible(!problemText.isEmpty());
        boolean hasRun = outcome.run() != null;
        tableScroll.setVisible(hasRun);
        table.setModel(hasRun ? ResultsTableModel.from(outcome.run())
                : new ResultsTableModel(List.of()));
        buttons.removeAll();
        if (settings != null) {
            addButtons(RunArtifacts.from(outcome, settings), outcome);
        }
        setVisible(true);
        revalidate();
    }

    private void addButtons(List<Artifact> artifacts, RunOutcome outcome) {
        for (Artifact artifact : artifacts) {
            buttons.add(openButton(artifact));
        }
        if (ResultsExport.available(artifacts)) {
            JButton export = new BrandButton("Export all…", false);
            export.addActionListener(event -> ResultsExport.chooseAndWrite(this, artifacts,
                    outcome.run().directory()));
            buttons.add(export);
        }
    }

    private JButton openButton(Artifact artifact) {
        JButton button = new BrandButton(artifact.label(), false);
        button.addActionListener(event -> open(artifact));
        return button;
    }

    private void open(Artifact artifact) {
        if (artifact.view() == Artifact.View.EXTERNAL) {
            ArtifactOpener.open(this, artifact.path());
            return;
        }
        if (viewer == null) {
            viewer = new OutputViewer(SwingUtilities.getWindowAncestor(this));
        }
        viewer.display(artifact);
    }

    private void configureHeadline() {
        headline.setEditable(false);
        headline.setLineWrap(true);
        headline.setWrapStyleWord(true);
        headline.setOpaque(false);
        headline.setFont(headline.getFont().deriveFont(Font.BOLD));
        headline.setForeground(Brand.INK);
        headline.getAccessibleContext().setAccessibleName("Result");
    }

    private void configureProblems() {
        problems.setEditable(false);
        problems.setLineWrap(true);
        problems.setWrapStyleWord(true);
        problems.setOpaque(false);
        problems.setVisible(false);
        problems.getAccessibleContext().setAccessibleName("Report problems");
    }

    private void configureTable() {
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        Brand.style(table);
        table.getAccessibleContext().setAccessibleName("Student scores");
        tableScroll.setPreferredSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(MAX_SCORE_DIGITS);
        table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(value instanceof Number ? format.format(value) : "");
            }
        });
    }
}
