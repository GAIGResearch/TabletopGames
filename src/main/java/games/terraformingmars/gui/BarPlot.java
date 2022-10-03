package games.terraformingmars.gui;

import javax.swing.*;
import java.awt.*;

import static games.terraformingmars.stats.TMStatsVisualiser.*;

public class BarPlot extends JComponent {
    double[] data;
    String[] xTicks;
    Image[] xTickImages;
    String yLabel, xLabel;
    int minY, maxY;

    boolean useXTickImages, drawYvalue = true;
    int barWidth, unitHeight = 20;

    final Dimension size;
    final static int padding = 1;
    final static int maxWidth = 300, maxHeight = 150;
    final static Color barColor = new Color(174, 241, 124, 190);

    public BarPlot(double[] data, String yLabel, String xLabel) {
        this.data = data;
        this.yLabel = yLabel;
        this.xLabel = xLabel;
        xTicks = new String[data.length];
        minY = (int)Math.floor(data[0]);
        maxY = (int)Math.ceil(data[0]);
        for (int i = 0; i < data.length; i++) {
            xTicks[i] = "" + i;
            if (data[i] > maxY) maxY = (int)Math.ceil(data[i]);
            if (data[i] < minY) minY = (int)Math.floor(data[i]);
        }
        barWidth = Math.max(maxWidth / data.length,1);
        if (maxY * unitHeight > maxHeight) unitHeight = Math.max(maxHeight / maxY,1);

        size = new Dimension(maxWidth + fontSize + padding*2, maxHeight + fontSize*2 + padding*5);
    }

    public void setxTicks(String[] xTicks) {
        this.xTicks = xTicks;
    }

    public void setxTickImages(Image[] xTickImages) {
        this.xTickImages = xTickImages;
        this.useXTickImages = true;
    }

    public void setUseXTickImages(boolean useXTickImages) {
        this.useXTickImages = useXTickImages;
    }

    public void setDrawYvalue(boolean drawYvalue) {
        this.drawYvalue = drawYvalue;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(defaultFontSmall);
        FontMetrics fm = g2.getFontMetrics();

        // Draw data
        for (int i = 0; i < data.length; i++) {
            // Draw bar
            g2.setColor(barColor);
            int height = unitHeight * Math.max((int)data[i],1);
            g2.fillRect(fontSize + padding+i*barWidth,
                        padding + (maxHeight - maxY*unitHeight) + (maxY - Math.max((int)data[i],1)) * unitHeight,
                     barWidth-padding*2,
                           height);
            // Draw Y value
            if (drawYvalue) {
                g2.setColor(fontColor);
                int textWidth = fm.stringWidth("" + data[i]);
                if (height < fontSize * 2) {
                    // Draw above
                    g2.drawString("" + data[i], fontSize + padding + i * barWidth + barWidth / 2 - textWidth / 2,
                            padding + (maxHeight - maxY * unitHeight) + (maxY - Math.max((int) data[i], 1)) * unitHeight - fontSize - padding);
                } else {
                    // Draw just below
                    g2.drawString("" + data[i], fontSize + padding + i * barWidth + barWidth / 2 - textWidth / 2,
                            padding + (maxHeight - maxY * unitHeight) + (maxY - Math.max((int) data[i], 1)) * unitHeight + fontSize + padding);
                }
            }

            // Draw tick images
            if (useXTickImages) {
                g2.drawImage(xTickImages[i], fontSize + padding + i * barWidth + barWidth / 2 - fontSize / 2, maxHeight + padding * 3, fontSize, fontSize, null);
            }

            // TODO: hover over data to show tick x label and data value text bigger (to see card win rate info)
            // TODO: order data boolean (card win rates should be ordered
            // TODO: scroll if plot too big to fit in window
        }

        // Draw outline
        g2.setColor(fontColor);
        g2.drawRect(fontSize, 0, maxWidth, maxHeight + padding*2);

    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
