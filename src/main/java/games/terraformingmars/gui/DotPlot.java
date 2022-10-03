package games.terraformingmars.gui;

import javax.swing.*;
import java.awt.*;

import static games.terraformingmars.stats.TMStatsVisualiser.*;

public class DotPlot extends JComponent {
    double[] data;
    int minY, maxY;
    int xTickWidth;

    final Dimension size;
    final static int padding = 2;
    final static int maxWidth = 300, maxHeight = 20, dotRadius = maxHeight/2;
    final static Color dotColor = new Color(64, 230, 248, 81);

    public DotPlot(double[] data, int minY, int maxY) {
        this.data = data;
        this.minY = minY;
        this.maxY = maxY;
        xTickWidth = Math.max(maxWidth / maxY, 1);
        size = new Dimension(maxWidth + padding*3, maxHeight+padding);
    }
    public DotPlot(Double[] data, int minY, int maxY) {
        this.data = new double[data.length];
        for (int i = 0 ; i < data.length; i++) {
            this.data[i] = data[i];
        }
        this.minY = minY;
        this.maxY = maxY;
        xTickWidth = Math.max(maxWidth / maxY, 1);
        size = new Dimension(maxWidth + padding*3, maxHeight+padding);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Draw data
        int yPos = maxHeight/2 - dotRadius/2;
        for (int i = 0; i < data.length; i++) {
            // Draw dots
            g2.setColor(dotColor);
            int xPos = padding + (int)data[i] * xTickWidth + xTickWidth/2 - dotRadius/2;
            g2.fillOval(xPos, yPos, dotRadius, dotRadius);
        }

        // Draw outline
        g2.setColor(fontColor);
        g2.drawRect(0, 0, maxWidth + padding*2, maxHeight);

    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
