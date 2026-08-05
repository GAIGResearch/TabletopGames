package games.descent2e.pcg;

import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeatMap {

    private final HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite;
    private float minX = Float.MAX_VALUE;
    private float maxX = Float.MIN_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxY = Float.MIN_VALUE;
    private float minFitness = Float.MAX_VALUE;
    private float maxFitness = Float.MIN_VALUE;

    private final int xRange;
    private final int yRange;

    public static class Entry {
        public float x;
        public float y;
        public int id;
        public float fitness;
    }

    public HeatMap(HashMap<Pair<Float, Float>, Pair<Integer, Float>> elite, String name) {

        this.elite = elite;

        List<Entry> entries = new ArrayList<>();

        for (Pair<Float, Float> key : elite.keySet()) {
            Pair<Integer, Float> result = elite.get(key);
            Entry entry = new Entry();
            entry.x = key.a;
            entry.y = key.b;
            entry.id = result.a;
            entry.fitness = result.b;

            entries.add(entry);

            if (key.a < minX)
                minX = key.a;
            if (key.a > maxX)
                maxX = key.a;
            if (key.b < minY)
                minY = key.b;
            if (key.b > maxY)
                maxY = key.b;
            if (result.b < minFitness)
                minFitness = result.b;
            if (result.b > maxFitness)
                maxFitness = result.b;
        }

        xRange = (int) (maxX - minX + 1);
        yRange = (int) (maxY - minY + 1);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(name);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new HeatMapPanel(entries, minX, maxX, minY, maxY, minFitness, maxFitness, xRange, yRange));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static class HeatMapPanel extends JPanel {

        private final List<Entry> entries;

        private float minX;
        private float maxX;
        private float minY;
        private float maxY;
        private float minFitness;
        private float maxFitness;
        private int xRange;
        private int yRange;

        private final int cellSize = 30;
        private final int margin = 300;

        public HeatMapPanel(List<Entry> entries, float minX, float maxX, float minY, float maxY, float minFitness, float maxFitness, int xRange, int yRange) {
            this.entries = entries;

            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minFitness = minFitness;
            this.maxFitness = maxFitness;
            this.xRange = xRange;
            this.yRange = yRange;


            int width = xRange * cellSize + (margin * 2);
            int height = yRange * cellSize + (margin * 2);

            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (Entry e : entries) {
                int x = (int) (margin + (e.x - minX) * cellSize);
                int y = (int) (margin + (maxY - e.y) * cellSize);

                g.setColor(getColour(e.fitness));
                g.fillRect(x, y, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, cellSize, cellSize);

                String text = String.valueOf(e.id);
                FontMetrics fm = g.getFontMetrics();
                int tx = x + (cellSize - fm.stringWidth(text)) / 2;
                int ty = y + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();

                g.drawString(text, tx, ty);
            }

            // X-axis labels
            for (int i = 0; i < xRange; i++) {
                int x = (int) (margin + (i - minX) * cellSize + cellSize / 2);
                String label = String.valueOf(i);
                g.drawString(label, x - g.getFontMetrics().stringWidth(label) / 2, margin - 10);
            }

            // Y-axis labels
            for (int gr = 0; gr < yRange; gr++) {
                int y = (int) (margin + (maxY - gr) * cellSize + cellSize / 2);
                String label = String.valueOf(gr);
                g.drawString(label,20,y + g.getFontMetrics().getAscent() / 2);
            }

            // Axis titles
            g.drawString("Size", getWidth() / 2, 20);

            Graphics2D gg = (Graphics2D) g.create();
            gg.rotate(-Math.PI / 2);
            gg.drawString("Groups", -getHeight() / 2, 20);
            gg.dispose();

            drawLegend(g);
        }

        private void drawLegend(Graphics2D graphics) {

            int x = getWidth() - 50;
            int y = margin;
            int h = 250;
            int w = 20;

            for (int i = 0; i < h; i++) {

                double t = 1.0 - (double) i / h;

                graphics.setColor(interpolate(t));

                graphics.drawLine(x, y + i, x + w, y + i);
            }

            graphics.setColor(Color.BLACK);
            graphics.drawRect(x, y, w, h);

            graphics.drawString(String.format("%.2f", maxFitness), x + 30, y + 10);
            graphics.drawString(String.format("%.2f", minFitness), x + 30, y + h);
        }

        private Color getColour(double fitness) {
            if (maxFitness == minFitness)
                return Color.RED;
            return interpolate((fitness - minFitness) / (maxFitness - minFitness));
        }

        // Cycle through Blue, Cyan, Green, Yellow, Red
        private Color interpolate(double t) {

            if (t < 0.25) {
                return blend(Color.BLUE, Color.CYAN, t / 0.25);
            } else if (t < 0.5) {
                return blend(Color.CYAN, Color.GREEN, (t - 0.25) / 0.25);
            } else if (t < 0.75) {
                return blend(Color.GREEN, Color.YELLOW, (t - 0.5) / 0.25);
            } else {
                return blend(Color.YELLOW, Color.RED, (t - 0.75) / 0.25);
            }
        }

        private Color blend(Color x, Color y, double t) {
            int r = (int)(x.getRed() * (1 - t) + y.getRed() * t);
            int g = (int)(x.getGreen() * (1 - t) + y.getGreen() * t);
            int b = (int)(x.getBlue() * (1 - t) + y.getBlue() * t);
            return new Color(r, g, b);
        }
    }
}