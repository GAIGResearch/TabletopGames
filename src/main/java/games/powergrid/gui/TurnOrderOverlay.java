package games.powergrid.gui;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TurnOrderOverlay extends JComponent {

    public static class Entry {
        public final String label;
        public final Color color;
        public final boolean active;  // true if in current round order

        public Entry(String label, Color color, boolean active) {
            this.label = label;
            this.color = color;
            this.active = active;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int radius = 12;
    private int spacing = 10;
    private int inactiveXOffset = 35;          
    private float inactiveAlpha = 0.75f;       
    private Point anchorPoint = new Point(0, 0);

    public TurnOrderOverlay() { setOpaque(false); }


    public void setTurnOrder(List<Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        repaint();
    }

    public void setAnchorPoint(Point p) { this.anchorPoint = p; repaint(); }
    public void setInactiveXOffset(int px) { this.inactiveXOffset = px; repaint(); }
    public void setRadius(int r) { this.radius = r; repaint(); }
    public void setSpacing(int s) { this.spacing = s; repaint(); }
    public void setInactiveAlpha(float a) { this.inactiveAlpha = Math.max(0f, Math.min(1f, a)); repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (entries.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int d = radius * 2;
        int baseX = anchorPoint.x;
        int y = anchorPoint.y;

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();

        for (Entry e : entries) {
            int x = baseX + (e.active ? 0 : inactiveXOffset);

            // Fill 
            Composite old = g2.getComposite();
            if (!e.active) g2.setComposite(AlphaComposite.SrcOver.derive(inactiveAlpha));
            g2.setPaint(e.color);
            g2.fillOval(x, y, d, d);
            g2.setComposite(old);

            // Border
            g2.setPaint(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x, y, d, d);

            // Label
            int tx = x + (d - fm.stringWidth(e.label)) / 2;
            int ty = y + (d + fm.getAscent() - fm.getDescent()) / 2;
            g2.setPaint(Color.BLACK);
            g2.drawString(e.label, tx, ty);

            y += d + spacing;
        }
        g2.dispose();
    }
}
