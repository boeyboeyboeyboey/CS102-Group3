package autograder.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A flat rounded button: red for the main action, outlined for everything else. */
final class BrandButton extends JButton {
    private static final int ARC = 10;
    private static final int PADDING_Y = 6;
    private static final int PADDING_X = 14;
    private static final float FOCUS_WIDTH = 2f;

    private final boolean primary;

    BrandButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(BorderFactory.createEmptyBorder(PADDING_Y, PADDING_X, PADDING_Y, PADDING_X));
        setForeground(primary ? Color.WHITE : Brand.INK);
        setBackground(Brand.DISABLED_TEXT_BASE);
    }

    // the system look on macOS ignores colours, so every platform uses the plain one
    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = (int) FOCUS_WIDTH;
        int width = getWidth() - 2 * inset - 1;
        int height = getHeight() - 2 * inset - 1;
        g.setColor(fill());
        g.fillRoundRect(inset, inset, width, height, ARC, ARC);
        if (!primary) {
            g.setColor(Brand.OUTLINE);
            g.drawRoundRect(inset, inset, width, height, ARC, ARC);
        }
        if (isFocusOwner()) {
            g.setColor(Brand.INK);
            g.setStroke(new BasicStroke(FOCUS_WIDTH));
            g.drawRoundRect(inset, inset, width, height, ARC, ARC);
        }
        g.dispose();
        super.paintComponent(graphics);
    }

    private Color fill() {
        if (!isEnabled()) {
            return Brand.DISABLED;
        }
        boolean active = getModel().isRollover() || getModel().isPressed();
        if (primary) {
            return active ? Brand.RED_DARK : Brand.RED;
        }
        return active ? Brand.HOVER : Color.WHITE;
    }
}
