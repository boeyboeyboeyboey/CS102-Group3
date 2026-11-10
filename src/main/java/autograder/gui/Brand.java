package autograder.gui;

import java.awt.Color;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.UIManager;

/** ByteGrade's colours  */
final class Brand {
    static final Color RED = new Color(0xB1251A);
    static final Color RED_DARK = new Color(0x8E1D14);
    static final Color INK = new Color(0x1B191A);
    static final Color CORAL = new Color(0xE9AB9F);
    static final Color LILAC = new Color(0xDCB3BD);
    static final Color SELECTION = new Color(0xF8DFDB);
    static final Color OUTLINE = new Color(0xCFC9C9);
    static final Color HOVER = new Color(0xFBF1EF);
    static final Color DISABLED = new Color(0xE4E0E0);
    static final Color DISABLED_TEXT_BASE = new Color(0xA8A2A2);
    private static final int ROW_HEIGHT = 24;

    private Brand() {
    }

    /** Call after the look and feel is set and before any window is created. */
    static void install() {
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("OptionPane.background", Color.WHITE);
    }

    static void style(JTable table) {
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(INK);
        table.setRowHeight(ROW_HEIGHT);
    }

    static void style(JList<?> list) {
        list.setSelectionBackground(SELECTION);
        list.setSelectionForeground(INK);
    }
}
