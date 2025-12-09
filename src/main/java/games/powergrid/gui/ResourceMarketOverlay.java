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
    private final PowerGridMapPannel mapPanel; //TODO refractor the PowerGridMapPannel 
    private static final Point[][] SLOT_COORDS_NATIVE = new java.awt.Point[][]{
        // COAL row 
        { 	new java.awt.Point(1061, 894), new java.awt.Point(1037, 894), //9
        	new java.awt.Point(958, 894),new java.awt.Point(934, 894), //8
        	new java.awt.Point(879, 894), new java.awt.Point(855, 894), //7
          new java.awt.Point(801, 894), new java.awt.Point(777, 894),new java.awt.Point(754, 894),//6
          new java.awt.Point(722, 894),new java.awt.Point(698, 894),new java.awt.Point(674, 894),//5
          new java.awt.Point(644,  894), new java.awt.Point(620,  894), new java.awt.Point(596,  894), //4 
          new java.awt.Point(565,  893), new java.awt.Point(541,  893), new java.awt.Point(517,  893), new java.awt.Point(493,  893), //3 
          new java.awt.Point(462,  892), new java.awt.Point(438,  892), new java.awt.Point(414,  892), new java.awt.Point(390,  892), //2 
          new java.awt.Point(359,  892), new java.awt.Point(335,  892), new java.awt.Point(311,  892), new java.awt.Point(287,  892)//1
          },

        // GAS row
        {
		    new java.awt.Point(958,  920), new java.awt.Point(934,  920),new java.awt.Point(910,  920), //8
		    new java.awt.Point(879,  920), new java.awt.Point(855,  920),new java.awt.Point(831,  920), //7
		    new java.awt.Point(801,  920), new java.awt.Point(777,  920), new java.awt.Point(754,  920), //6
		    new java.awt.Point(722,  919), new java.awt.Point(698,  919), new java.awt.Point(674,  919), //5
		    new java.awt.Point(644,  919), new java.awt.Point(620,  919), new java.awt.Point(596,  919), //4 
		    new java.awt.Point(565,  919), new java.awt.Point(541,  919), new java.awt.Point(517,  919), //3 
		    new java.awt.Point(462,  919), new java.awt.Point(438,  919), new java.awt.Point(414,  919), //2 
		    new java.awt.Point(360,  919), new java.awt.Point(336,  919), new java.awt.Point(312,  919),  //1 
        },

        // OIL row
        {
        	new java.awt.Point(1061, 949), new java.awt.Point(1037, 949),new java.awt.Point(1013, 949), new java.awt.Point(989, 949), //9
            new java.awt.Point(958,  949), new java.awt.Point(934,  949), //8
            new java.awt.Point(881,  948), new java.awt.Point(857,  948), //7
            new java.awt.Point(803,  948), new java.awt.Point(779,  948), //6
            new java.awt.Point(724,  948), new java.awt.Point(700,  948), //5
            new java.awt.Point(644,  948), new java.awt.Point(620,  948),  //4 
            new java.awt.Point(565,  947), new java.awt.Point(541,  947), //3 
            new java.awt.Point(462,  947), new java.awt.Point(438,  947),  //2 
            new java.awt.Point(360,  947), new java.awt.Point(336,  947),  //1 
        },



        // URANIUM row
        {
            new java.awt.Point(1061, 975), new java.awt.Point(1037, 975), //9
            new java.awt.Point(958,  975), new java.awt.Point(934,  975), //8
            new java.awt.Point(881,  976), new java.awt.Point(857,  976), //7
            new java.awt.Point(803,  976),  //6
            new java.awt.Point(724,  976),  //5
            new java.awt.Point(644,  976),  //4 
            new java.awt.Point(565,  974),  //3 
            new java.awt.Point(462,  974),  //2 
            new java.awt.Point(360,  974),   //1 
        },
    };
    
    ResourceMarketOverlay(PowerGridMapPannel mapPanel,EnumMap<PowerGridParameters.Resource, Integer> avail) {
        this.mapPanel = mapPanel;
        this.avail = avail;
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double sx = mapPanel.getScaleX(), sy = mapPanel.getScaleY();
        int diameter = Math.max(8, (int)(18 * Math.min(sx, sy))); // scale puck size with board

        for (int row = 0; row < SLOT_COORDS_NATIVE.length; row++) {
            int count = Math.min(avail.getOrDefault(PowerGridParameters.Resource.values()[row], 0),
            		SLOT_COORDS_NATIVE[row].length);
            for (int i = 0; i < count; i++) {
                Point pNative = SLOT_COORDS_NATIVE[row][i]; // nth cheapest slot
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
