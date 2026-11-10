package autograder.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;

/** The ByteGrade title strip, fading from white into the brand's coral and lilac. */
final class BrandHeader extends JPanel {
    private static final float[] STOPS = {0f, 0.45f, 0.8f, 1f};
    private static final Color[] COLORS = {Color.WHITE, Color.WHITE, Brand.CORAL, Brand.LILAC};
    private static final int TITLE_SIZE = 26;
    private static final int GAP = 10;

    BrandHeader() {
        super(new FlowLayout(FlowLayout.LEFT, GAP, 0));
        setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        JLabel name = new JLabel(Brand.NAME + ":");
        name.setFont(name.getFont().deriveFont(Font.BOLD, TITLE_SIZE));
        name.setForeground(Brand.INK);
        JLabel product = new JLabel("AutoGrader");
        product.setFont(product.getFont().deriveFont(Font.PLAIN, TITLE_SIZE));
        product.setForeground(Brand.INK);
        add(name);
        add(product);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new LinearGradientPaint(0, 0, Math.max(1, getWidth()), 0, STOPS, COLORS));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}
