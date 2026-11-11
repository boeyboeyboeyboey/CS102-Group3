package autograder.gui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.function.ToIntFunction;

/** One left-aligned row while it fits, otherwise a grid with a fixed column count. */
final class ResponsiveButtonLayout implements LayoutManager {
    private final FlowLayout row;
    private final GridLayout grid;
    private final ToIntFunction<Container> screenWidth;
    private boolean sizedAsRow = true;

    ResponsiveButtonLayout(int gap, int columns) {
        this(gap, columns, ResponsiveButtonLayout::screenWidthFor);
    }

    ResponsiveButtonLayout(int gap, int columns, ToIntFunction<Container> screenWidth) {
        this.screenWidth = screenWidth;
        row = new FlowLayout(FlowLayout.LEFT, gap, 0);
        grid = new GridLayout(0, columns, gap, gap);
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
        row.addLayoutComponent(name, component);
        grid.addLayoutComponent(name, component);
    }

    @Override
    public void removeLayoutComponent(Component component) {
        row.removeLayoutComponent(component);
        grid.removeLayoutComponent(component);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        sizedAsRow = fitsRow(parent);
        return active(sizedAsRow).preferredLayoutSize(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return active(fitsRow(parent)).minimumLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        boolean asRow = fitsRow(parent);
        active(asRow).layoutContainer(parent);
        if (asRow != sizedAsRow && parent instanceof JComponent panel) {
            // the parent cached a preferred height for the other shape; ask for a fresh pass afterwards
            sizedAsRow = asRow;
            SwingUtilities.invokeLater(panel::revalidate);
        }
    }

    private LayoutManager active(boolean asRow) {
        return asRow ? row : grid;
    }

    private boolean fitsRow(Container parent) {
        int needed = row.preferredLayoutSize(parent).width;
        int width = parent.getWidth();
        return needed <= (width > 0 ? width : screenWidth.applyAsInt(parent));
    }

    // before the window is shown there is no width yet, so the usable screen width decides
    private static int screenWidthFor(Container parent) {
        if (GraphicsEnvironment.isHeadless()) {
            return Integer.MAX_VALUE;
        }
        try {
            GraphicsConfiguration configuration = parent.getGraphicsConfiguration();
            Rectangle bounds = configuration != null ? configuration.getBounds()
                    : GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            Insets screen = configuration != null
                    ? Toolkit.getDefaultToolkit().getScreenInsets(configuration) : new Insets(0, 0, 0, 0);
            return bounds.width - screen.left - screen.right - chromeWidth(parent);
        } catch (HeadlessException e) {
            return Integer.MAX_VALUE;
        }
    }

    // horizontal insets of every ancestor up to the window, which the row has to fit inside as well
    private static int chromeWidth(Container parent) {
        int total = 0;
        for (Container c = parent.getParent(); c != null; c = c.getParent()) {
            Insets insets = c.getInsets();
            total += insets.left + insets.right;
            if (c instanceof Window) {
                break;
            }
        }
        return total;
    }
}
