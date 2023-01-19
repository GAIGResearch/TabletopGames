package gui.plotting;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class BarPlot extends JComponent {
    double[] data;
    String[] xTicks;
    Image[] xTickImages;
    String yLabel, xLabel;
    double minY, maxY;

    List<Data> dataAll;

    boolean useXTickImages, drawYvalue = true;
    double barWidth, unitHeight = 20;
    boolean stretchY = true, orderData, reverseOrder = true;

    Dimension size;
    int padding = 1;
    int maxWidth = 300, maxHeight = 150;
    Color barColor = new Color(174, 241, 124, 190);
    int hoverOverWidth = 100, hoverOverHeight = 20;
    Color hoverOverColor = new Color(0,0,0,150);
    int fontSize;
    Font font;
    Color fontColor;

    HashMap<Rectangle, Integer> bars; // map from location on screen to idx in data array
    Rectangle highlight;

    public BarPlot(double[] data, String yLabel, String xLabel, int fontSize, Font font, Color fontColor) {
        this.data = data;
        this.yLabel = yLabel;
        this.xLabel = xLabel;
        this.fontSize = fontSize;
        this.font = font;
        this.fontColor = fontColor;
        dataAll = new ArrayList<>();
        xTicks = new String[data.length];
        xTickImages = new Image[data.length];
        minY = data[0];
        maxY = data[0];
        for (int i = 0; i < data.length; i++) {
            xTicks[i] = "" + i;
            if (data[i] > maxY) maxY = data[i];
            if (data[i] < minY) minY = data[i];
            dataAll.add(new Data(data[i], xTicks[i], xTickImages[i]));
        }
        barWidth = Math.max(maxWidth / data.length, 10);
        if (maxY * unitHeight > maxHeight || stretchY) unitHeight = maxHeight / maxY;

        size = new Dimension((int)(barWidth * data.length) + fontSize + padding*2, maxHeight + fontSize*2 + padding*5);
        bars = new HashMap<>();

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                // Remove highlight
                highlight = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click, highlight cell
                    for (Rectangle r: bars.keySet()) {
                        if (r == null) continue;
                        if (r.contains(e.getPoint())) {
                            highlight = r;
                            break;
                        }
                    }
                } else {
                    // Remove highlight
                    highlight = null;
                }
            }
        });
    }

    public void setxTicks(String[] xTicks) {
        this.xTicks = xTicks;
        for (int i = 0; i < data.length; i++) {
            dataAll.get(i).xTick = xTicks[i];
        }
    }

    public void setxTickImages(Image[] xTickImages) {
        this.xTickImages = xTickImages;
        this.useXTickImages = true;
        for (int i = 0; i < data.length; i++) {
            dataAll.get(i).xTickImage = xTickImages[i];
        }
    }

    public void setUseXTickImages(boolean useXTickImages) {
        this.useXTickImages = useXTickImages;
    }

    public void setDrawYvalue(boolean drawYvalue) {
        this.drawYvalue = drawYvalue;
    }

    public void setOrderData(boolean orderData) {
        this.orderData = orderData;
    }

    public void setUnitHeight(double unitHeight) {
        this.unitHeight = unitHeight;
    }

    public void setStretchY(boolean stretchY) {
        this.stretchY = stretchY;
    }

    public void setReverseOrder(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public void setBarColor(Color barColor) {
        this.barColor = barColor;
    }

    public void setHoverOverWidth(int hoverOverWidth) {
        this.hoverOverWidth = hoverOverWidth;
    }

    public void setHoverOverHeight(int hoverOverHeight) {
        this.hoverOverHeight = hoverOverHeight;
    }

    public void setHoverOverColor(Color hoverOverColor) {
        this.hoverOverColor = hoverOverColor;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        if (maxY * unitHeight > maxHeight || stretchY) unitHeight = maxHeight / maxY;
        size = new Dimension((int)(barWidth * data.length) + fontSize + padding*2, maxHeight + fontSize*2 + padding*5);
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        barWidth = Math.max(maxWidth / data.length, 10);
        size = new Dimension((int)(barWidth * data.length) + fontSize + padding*2, maxHeight + fontSize*2 + padding*5);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (font != null) {
            g2.setFont(font);
        }
        FontMetrics fm = g2.getFontMetrics();

        // Order data if required (card win rates should be ordered)
        if (orderData) {
            dataAll.sort(reverseOrder ? Comparator.reverseOrder() : Comparator.naturalOrder());
        }

        // Draw data
        for (int i = 0; i < data.length; i++) {
            double yValue = dataAll.get(i).yValue;

            // Draw bar
            g2.setColor(barColor);
            int height = Math.max((int)(unitHeight * yValue),1);
            Rectangle rect = new Rectangle(fontSize + padding + Math.max((int)(i*barWidth),1),
                    padding + Math.max((int)((maxHeight - maxY*unitHeight) + (maxY - yValue) * unitHeight), 1),
                    (int)barWidth - padding*2,
                    height);
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            bars.put(rect, i);

            // Draw Y value
            if (drawYvalue) {
                g2.setColor(fontColor);
                int textWidth = fm.stringWidth("" + yValue);
                if (height < fontSize * 2) {
                    // Draw above
                    g2.drawString("" + yValue, rect.x + (int)(barWidth / 2) - textWidth / 2,
                            rect.y - fontSize - padding);
                } else {
                    // Draw just below
                    g2.drawString("" + yValue, rect.x + (int)(barWidth / 2) - textWidth / 2,
                            rect.y + fontSize + padding);
                }
            }

            // Draw tick images
            if (useXTickImages) {
                g2.drawImage(dataAll.get(i).xTickImage, rect.x + (int)(barWidth / 2) - fontSize / 2, maxHeight + padding * 3, fontSize, fontSize, null);
            }
        }

        // Draw outline
        g2.setColor(fontColor);
        g2.drawRect(fontSize, 0, (int)(barWidth * data.length), maxHeight + padding*2);

        // Click on data bar to show tick x label and data value text bigger (to see card win rate info)
        if (highlight != null) {
            int i = bars.get(highlight);
            String str = String.format("%30s %8.3g", xLabel + ": " + dataAll.get(i).xTick + "; " + yLabel + ":", dataAll.get(i).yValue);
            int textWidth = fm.stringWidth(str) + padding*2;

            g2.setColor(hoverOverColor);
            Rectangle adjusted = highlight;

            Rectangle rect = new Rectangle(adjusted.x, adjusted.y, Math.max(hoverOverWidth,textWidth), hoverOverHeight);
            if (rect.x+rect.width > size.width) rect.x = size.width-rect.width;
            if (rect.y+rect.height > maxHeight) rect.y = maxHeight - rect.height;
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);

            g2.setColor(fontColor);
            g2.drawString(str, rect.x+padding, rect.y + hoverOverHeight/2);
//            System.out.println(str);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    static class Data implements Comparable<Data> {
        public double yValue;
        public String xTick;
        public Image xTickImage;

        public Data(double yValue, String xTick, Image xTickImage) {
            this.yValue = yValue;
            this.xTick = xTick;
            this.xTickImage = xTickImage;
        }

        static boolean sortByValue = true, sortByTick;

        @Override
        public int compareTo(@NotNull Data o) {
            if (sortByTick) return xTick.compareTo(o.xTick);
            if (sortByValue) if (yValue < o.yValue) return -1; else if (yValue > o.yValue) return 1;
            return 0;
        }
    }
}
