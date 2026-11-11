package autograder.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponsiveButtonLayoutTest {
    private static final int GAP = 8;
    private static final int BUTTON_WIDTH = 100;
    private static final int ROW_WIDTH = 10 * BUTTON_WIDTH + 11 * GAP;

    private static JPanel panel(int buttons, int screenWidth) {
        JPanel panel = new JPanel(new ResponsiveButtonLayout(GAP, 5, parent -> screenWidth));
        for (int i = 0; i < buttons; i++) {
            JButton button = new JButton("b" + i);
            button.setPreferredSize(new Dimension(BUTTON_WIDTH, 28));
            panel.add(button);
        }
        return panel;
    }

    private static Set<Integer> rows(JPanel panel) {
        return Arrays.stream(panel.getComponents()).map(Component::getY).collect(Collectors.toSet());
    }

    private static void layoutAt(JPanel panel, int width) {
        panel.setSize(width, panel.getPreferredSize().height);
        panel.doLayout();
    }

    @Test
    void unshownPanelOnWideScreenGetsTheSingleRow() {
        JPanel panel = panel(10, 2000);
        assertEquals(new Dimension(ROW_WIDTH, 28), panel.getPreferredSize());
    }

    @Test
    void unshownPanelOnNarrowScreenGetsTheGrid() {
        JPanel panel = panel(10, ROW_WIDTH - 1);
        assertEquals(new Dimension(5 * BUTTON_WIDTH + 4 * GAP, 2 * 28 + GAP), panel.getPreferredSize());
    }

    @Test
    void singleRowKeepsNaturalWidthsFromTheLeftWhenItFits() {
        JPanel panel = panel(10, 2000);
        layoutAt(panel, ROW_WIDTH);
        assertEquals(1, rows(panel).size());
        assertEquals(GAP, panel.getComponent(0).getX());
        assertEquals(GAP + BUTTON_WIDTH + GAP, panel.getComponent(1).getX());
        assertEquals(BUTTON_WIDTH, panel.getComponent(9).getWidth());
    }

    @Test
    void gridTakesOverBelowTheRowWidthAndReturnsWhenWidened() {
        JPanel panel = panel(10, 2000);
        layoutAt(panel, ROW_WIDTH - 1);
        assertEquals(2, rows(panel).size());
        assertEquals(panel.getComponent(0).getY(), panel.getComponent(4).getY());
        layoutAt(panel, ROW_WIDTH);
        assertEquals(1, rows(panel).size());
    }

    @Test
    void fewButtonsStayOnOneRowWhenTheyFit() {
        JPanel panel = panel(3, 2000);
        layoutAt(panel, 600);
        assertEquals(1, rows(panel).size());
    }
}
