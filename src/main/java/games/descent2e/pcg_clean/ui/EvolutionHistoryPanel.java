package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.spatial.GenerationStatistics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Two small dependency-free Java2D charts for spatial-EA progress. */
public final class EvolutionHistoryPanel extends JPanel {
    public EvolutionHistoryPanel(List<GenerationStatistics> history) {
        setLayout(new GridLayout(2, 1, 8, 8));
        add(new LineChart("Constraint violations (lower is better)", history,
                GenerationStatistics::minimumViolations, GenerationStatistics::averageViolations,
                "minimum", "population mean", new Color(190, 48, 42), new Color(239, 145, 50)));
        add(new LineChart("Weighted fitness (higher is better)", history,
                GenerationStatistics::bestFitness, GenerationStatistics::averageFitness,
                "best at minimum violations", "population mean", new Color(31, 119, 180), new Color(44, 160, 80)));
    }

    public static void showInWindow(List<GenerationStatistics> history) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Spatial EA progress");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.add(new EvolutionHistoryPanel(history));
            frame.setSize(950, 700);
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    private static final class LineChart extends JPanel {
        private static final int LEFT = 64, RIGHT = 24, TOP = 38, BOTTOM = 42;
        private final String title;
        private final List<GenerationStatistics> history;
        private final ToDoubleFunction<GenerationStatistics> first;
        private final ToDoubleFunction<GenerationStatistics> second;
        private final String firstLabel, secondLabel;
        private final Color firstColour, secondColour;

        private LineChart(String title, List<GenerationStatistics> history,
                          ToDoubleFunction<GenerationStatistics> first,
                          ToDoubleFunction<GenerationStatistics> second,
                          String firstLabel, String secondLabel, Color firstColour, Color secondColour) {
            this.title = title; this.history = List.copyOf(history); this.first = first; this.second = second;
            this.firstLabel = firstLabel; this.secondLabel = secondLabel;
            this.firstColour = firstColour; this.secondColour = secondColour;
            setBackground(Color.WHITE);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (history.isEmpty()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth() - LEFT - RIGHT, height = getHeight() - TOP - BOTTOM;
                double max = history.stream().mapToDouble(s -> Math.max(first.applyAsDouble(s), second.applyAsDouble(s))).max().orElse(1);
                double min = history.stream().mapToDouble(s -> Math.min(first.applyAsDouble(s), second.applyAsDouble(s))).min().orElse(0);
                if (max == min) max = min + 1;
                drawAxes(g, width, height, min, max);
                drawSeries(g, first, firstColour, width, height, min, max);
                drawSeries(g, second, secondColour, width, height, min, max);
                drawLegend(g);
            } finally { g.dispose(); }
        }

        private void drawAxes(Graphics2D g, int width, int height, double min, double max) {
            g.setColor(Color.DARK_GRAY);
            g.drawLine(LEFT, TOP, LEFT, TOP + height);
            g.drawLine(LEFT, TOP + height, LEFT + width, TOP + height);
            g.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g.drawString(title, LEFT, 22);
            g.setFont(getFont().deriveFont(11f));
            g.drawString("%.3f".formatted(max), 8, TOP + 5);
            g.drawString("%.3f".formatted(min), 8, TOP + height);
            g.drawString("generation", LEFT + width / 2 - 25, getHeight() - 8);
        }

        private void drawSeries(Graphics2D g, ToDoubleFunction<GenerationStatistics> metric, Color colour,
                                int width, int height, double min, double max) {
            Path2D path = new Path2D.Double();
            for (int i = 0; i < history.size(); i++) {
                double x = LEFT + i * width / (double) Math.max(1, history.size() - 1);
                double y = TOP + height - (metric.applyAsDouble(history.get(i)) - min) * height / (max - min);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            g.setColor(colour); g.setStroke(new BasicStroke(2f)); g.draw(path);
        }

        private void drawLegend(Graphics2D g) {
            int x = Math.max(LEFT + 10, getWidth() - 260);
            g.setColor(firstColour); g.fillRect(x, 12, 14, 4); g.setColor(Color.DARK_GRAY); g.drawString(firstLabel, x + 20, 18);
            g.setColor(secondColour); g.fillRect(x, 27, 14, 4); g.setColor(Color.DARK_GRAY); g.drawString(secondLabel, x + 20, 33);
        }
    }
}
