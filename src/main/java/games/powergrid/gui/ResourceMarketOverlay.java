package games.powergrid.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import javax.swing.JPanel;

import javax.swing.JComponent;

import java.util.EnumMap;
import java.util.Map;

import games.powergrid.PowerGridParameters;

class ResourceMarketOverlay extends JComponent {
    private final java.util.EnumMap<PowerGridParameters.Resource, Integer> avail;
    private final java.awt.Point[][] slotCentersPx; 
    private final PowerGridMapPannel mapPanel; //TODO refractor the PowerGridMapPannel 
    ResourceMarketOverlay(PowerGridMapPannel mapPanel,
                          Point[][] slotCentersPx,
                          EnumMap<PowerGridParameters.Resource, Integer> avail) {
        this.mapPanel = mapPanel;
        this.slotCentersPx = slotCentersPx;
        this.avail = avail;
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double sx = mapPanel.getScaleX(), sy = mapPanel.getScaleY();
        int diameter = Math.max(8, (int)(18 * Math.min(sx, sy))); // scale puck size with board

        for (int row = 0; row < slotCentersPx.length; row++) {
            int count = Math.min(avail.getOrDefault(PowerGridParameters.Resource.values()[row], 0),
                                 slotCentersPx[row].length);
            for (int i = 0; i < count; i++) {
                Point pNative = slotCentersPx[row][i]; // nth cheapest slot
                int cx = (int) Math.round(pNative.x * sx);
                int cy = (int) Math.round(pNative.y * sy);
                int r = diameter / 2;

                // shadow
                g2.setColor(new Color(0,0,0,60));
                g2.fillOval(cx - r + 2, cy - r + 3, diameter, diameter);

                // puck
                Color color = switch (PowerGridParameters.Resource.values()[row]) {
                    case COAL -> new Color(101, 67, 33);
                    case OIL -> new Color(30, 30, 30);
                    case GAS -> new Color(135, 206, 235);
                    case URANIUM -> new Color(255, 99, 71);
                };
                Color outline_color = switch (PowerGridParameters.Resource.values()[row]) {
	                case COAL -> new Color(255,255,255,120);
	                case OIL -> new Color(64, 64, 64);
	                case GAS -> new Color(25, 25, 112);
	                case URANIUM -> new Color(178, 34, 34);
            };
                g2.setColor(color);
                g2.fillOval(cx - r, cy - r, diameter, diameter);

                // outline
                g2.setColor(outline_color);
                g2.setStroke(new BasicStroke(Math.max(1f, diameter * 0.f)));
                g2.drawOval(cx - r, cy - r, diameter, diameter);
            }
        }
        g2.dispose();
    }

    public void updateComponent(Map<PowerGridParameters.Resource, Integer> newAvail) {
        for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values()) {
            // null-safe default to 0
            this.avail.put(r, newAvail.getOrDefault(r, 0));
        }
        repaint();
    }

}
