package autograder.gui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FontMetrics;

/** A CSV report as a sortable table, with the selected row's full text underneath. */
final class CsvView extends JPanel {
    static final String EMPTY = "Nothing to show: this report has no rows.";
    private static final int MIN_COLUMN_WIDTH = 70;
    private static final int MAX_COLUMN_WIDTH = 420;
    private static final int COLUMN_PADDING = 24;
    private static final int SAMPLED_ROWS = 50;
    private static final int TABLE_HEIGHT = 400;

    CsvView(CsvTableModel model) {
        super(new BorderLayout());
        if (model.getRowCount() == 0) {
            add(new JLabel(EMPTY, SwingConstants.CENTER), BorderLayout.CENTER);
            return;
        }
        JTable table = new JTable(model);
        configure(table, model);
        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.getAccessibleContext().setAccessibleName("Selected row");
        table.getSelectionModel().addListSelectionListener(event -> showRow(table, model, details));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(details));
        split.setDividerLocation(TABLE_HEIGHT);
        split.setResizeWeight(1);
        add(split, BorderLayout.CENTER);
        table.setRowSelectionInterval(0, 0);
    }

    private static void configure(JTable table, CsvTableModel model) {
        TableRowSorter<CsvTableModel> sorter = new TableRowSorter<>(model);
        for (int column = 0; column < model.getColumnCount(); column++) {
            sorter.setComparator(column, CsvTableModel.VALUE_ORDER);
        }
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        Brand.style(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // cells hold student output, which swing would otherwise render if it starts with <html>
        DefaultTableCellRenderer plain = new DefaultTableCellRenderer();
        plain.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, plain);
        FontMetrics metrics = table.getFontMetrics(table.getFont());
        for (int column = 0; column < model.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(width(model, column, metrics));
        }
    }

    private static int width(CsvTableModel model, int column, FontMetrics metrics) {
        int widest = metrics.stringWidth(model.getColumnName(column));
        for (int row = 0; row < Math.min(SAMPLED_ROWS, model.getRowCount()); row++) {
            widest = Math.max(widest, metrics.stringWidth(model.getValueAt(row, column)));
        }
        return Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, widest + COLUMN_PADDING));
    }

    private static void showRow(JTable table, CsvTableModel model, JTextArea details) {
        int row = table.getSelectedRow();
        details.setText(row < 0 ? "" : model.details(table.convertRowIndexToModel(row)));
        details.setCaretPosition(0);
    }
}
