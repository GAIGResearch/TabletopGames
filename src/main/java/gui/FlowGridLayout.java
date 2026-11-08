package gui;

import java.awt.*;

/**
 * Lays out the visible components of a container as a grid of equal cells that fills each row across the available
 * width before starting the next row. Every cell is as wide as the widest visible component (but no wider than the
 * container) and as tall as the tallest, and the cells then share out any width left over in the row. Hidden
 * components take no cell.
 * <p>
 * The width available is the container's own width once it has one, otherwise the preferred width given here. In a
 * JScrollPane the container should track the viewport width (see ActionGridPanel), so the rows wrap to the visible
 * width and only a vertical scrollbar is needed.
 */
public class FlowGridLayout implements LayoutManager {

    private final int hgap, vgap, preferredWidth;

    public FlowGridLayout(int hgap, int vgap, int preferredWidth) {
        this.hgap = hgap;
        this.vgap = vgap;
        this.preferredWidth = preferredWidth;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private Dimension cellSize(Container parent, int innerWidth) {
        int w = 0, h = 0;
        for (Component c : parent.getComponents())
            if (c.isVisible()) {
                Dimension d = c.getPreferredSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
        return new Dimension(Math.min(w, Math.max(1, innerWidth)), h);
    }

    private int innerWidth(Container parent) {
        Insets in = parent.getInsets();
        int width = parent.getWidth() > 0 ? parent.getWidth() : preferredWidth;
        return width - in.left - in.right;
    }

    private int columns(int innerWidth, Dimension cell) {
        return Math.max(1, (innerWidth + hgap) / (cell.width + hgap));
    }

    private int visibleCount(Container parent) {
        int n = 0;
        for (Component c : parent.getComponents())
            if (c.isVisible()) n++;
        return n;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int innerWidth = innerWidth(parent);
            Dimension cell = cellSize(parent, innerWidth);
            int n = visibleCount(parent);
            int rows = n == 0 ? 0 : (n + columns(innerWidth, cell) - 1) / columns(innerWidth, cell);
            int height = rows == 0 ? 0 : rows * cell.height + (rows - 1) * vgap;
            return new Dimension(innerWidth + in.left + in.right, height + in.top + in.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int innerWidth = innerWidth(parent);
            Dimension cell = cellSize(parent, innerWidth);
            int cols = columns(innerWidth, cell);
            cell.width = Math.max(cell.width, (innerWidth - (cols - 1) * hgap) / cols);
            int i = 0;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                int x = in.left + (i % cols) * (cell.width + hgap);
                int y = in.top + (i / cols) * (cell.height + vgap);
                c.setBounds(x, y, cell.width, cell.height);
                i++;
            }
        }
    }
}
