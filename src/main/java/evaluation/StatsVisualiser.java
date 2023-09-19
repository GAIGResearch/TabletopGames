package evaluation;

import core.interfaces.IStatisticLogger;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.Event;
import evaluation.summarisers.*;
import games.GameType;
import games.terraformingmars.stats.TMStatsVisualiser;
import gui.TiledImage;
import gui.plotting.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/**
 * Used to visualise the data recorded by metrics in a game. Includes many helpful methods.
 * New visualisers implemented should:
 *      1. Extend this class
 *      2. Add their constructor in the {@link #getVisualiserForGame(GameType, List)} method in this class as a new case
 * See example for {@link GameType#TerraformingMars} in {@link TMStatsVisualiser}
 * The visualisers are called to display stats at the end of a run *
 */
public abstract class StatsVisualiser extends JFrame {
    private final Map<Event.GameEvent, IStatisticLogger> loggers;
    protected static int fontSize = 16;
    protected static Color fontColor = Color.white;
    protected static Color backgroundColor = Color.black;
    protected GridBagConstraints gridBagConstraints;

    public StatsVisualiser(List<MetricsGameListener> listeners) {
        if (listeners.size() > 1) System.out.println("Only showing first listener");
//        loggers = listeners.get(0).get();
        loggers = new HashMap<>(); // todo

        // Default colors
        getContentPane().setBackground(backgroundColor);
        UIManager.put("Label.foreground", fontColor);

        // Defaults to grid bag layout
        getContentPane().setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
    }

    public void setFrameProperties(Dimension dimension) {
        // Frame properties
        setSize(dimension);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        revalidate();
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setExtendedState(JFrame.MAXIMIZED_BOTH);
        repaint();
    }

    protected void setBackgroundImage(BufferedImage bg) {
        TexturePaint image = new TexturePaint(bg, new Rectangle2D.Float(0,0, bg.getWidth(), bg.getHeight()));
        TiledImage backgroundImage = new TiledImage(image);
        setContentPane(backgroundImage);
        getContentPane().setLayout(new GridBagLayout());
    }

    /**
     * Retrieves the data recorded for a particular metric, given the game event it responded to
     * @param metricName - name of metric
     * @param event - game event metric responds to
     * @return - TAGStatSummary object encompassing recorded data
     */
    protected TAGStatSummary getStats(String metricName, Event.GameEvent event) {
        return loggers.get(event).summary().get(metricName + ":" + event);
    }

    /**
     * Visualises data for a given metric as a dot plot (horizontal, 1-dimensional data)
     * @param metricName - name of metric
     * @param event - event metric responds to
     * @param minY - minimum value in the 1-dimension (to bound plot consistently)
     * @param maxY - maximum value in the 1-dimension (to bound plot consistently)
     * @param alternatePrintName - name that will be used for the plot summary label instead of the metric class name (can be ignored / null)
     */
    protected void visualiseMetricAsDotPlot(String metricName, Event.GameEvent event, int minY, int maxY, String alternatePrintName) {
        TAGNumericStatSummary stats = visualiseNumericMetricAsLabel(metricName, event, alternatePrintName);

        gridBagConstraints.gridy++;
        DotPlot dotPlot = new DotPlot(stats.getElements().toArray(new Double[0]), minY, maxY);
        setDefaultDotPlotProperties(dotPlot);
        getContentPane().add(dotPlot, gridBagConstraints);
    }
    protected void visualiseMetricAsDotPlot(String metricName, Event.GameEvent event, int minY, int maxY) {
        visualiseMetricAsDotPlot(metricName, event, minY, maxY, null);
    }

    /**
     * Prints the name of the metric, its mean and standard deviation, based on recorded data. No plot.
     * @param metricName - name of metric
     * @param event - event metric responds to
     * @param alternatePrintName - name that will be used for the plot summary label instead of the metric class name (can be ignored / null)
     * @return
     */
    protected TAGNumericStatSummary visualiseNumericMetricAsLabel(String metricName, Event.GameEvent event, String alternatePrintName) {
        gridBagConstraints.gridy++;
        TAGNumericStatSummary stats = (TAGNumericStatSummary) getStats(metricName, event);
        List<String> sorted = new ArrayList<>(loggers.get(event).summary().keySet());
        Collections.sort(sorted);
        if (alternatePrintName == null) alternatePrintName = metricName.split(":")[0];
        getContentPane().add(new JLabel(String.format("%30s  %8.3g +/- %6.2g", alternatePrintName + ":", stats.mean(), stats.sd())), gridBagConstraints);
        return stats;
    }
    protected TAGNumericStatSummary visualiseNumericMetricAsLabel(String metricName, Event.GameEvent event) {
        return visualiseNumericMetricAsLabel(metricName, event, null);
    }

    /**
     * Visualises 2-dimensional numeric metric data as a bar plot
     * @param data - array of double; index is X-axis, value in array at index is Y-axis
     * @param xLabel - label for the X axis of the plot
     * @param yLabel - label for the Y axis of the plot
     * @param fontStyle - style of font for labels
     * @param xTickLabels - labels for xTicks
     * @param xTickImages - images for the xTicks (replaces labels if supplied; can be null)
     * @param gridHeight - how many rows does this plot take in the grid bag layout; suggested for default properties here: 7
     */
    protected void visualiseMetricAsBarPlot(double[] data, String xLabel, String yLabel, Font fontStyle, String[] xTickLabels, Image[] xTickImages, int gridHeight) {
        BarPlot barPlot1 = new BarPlot(data, xLabel, yLabel, fontSize, fontStyle, fontColor);
        barPlot1.setxTicks(xTickLabels);
        if (xTickImages != null) {
            barPlot1.setxTickImages(xTickImages);
        }
        setDefaultBarPlotProperties(barPlot1);
        gridBagConstraints.gridy ++;
        gridBagConstraints.gridheight = gridHeight;
        getContentPane().add(barPlot1, gridBagConstraints);
        gridBagConstraints.gridheight = 1;
    }

    /**
     * Visualises the win rate for components as a scrollable bar plot. Can click the bars to see details of the information.
     * @param playedByWinner - TAGOccurrenceStatSummary, containing the list of component names for those components played by the winner
     * @param playedByAll - TAGOccurrenceStatSummary, containing the list of all component names played in the game
     * @param xLabel - label for the X axis (Y axis label is "Win rate")
     * @param fontStyle - style of font for the X and Y labels
     * @param gridHeight - how many rows does this plot take in the grid bag layout; suggested for default properties here: 7
     */
    protected void visualiseComponentWinRate(TAGOccurrenceStatSummary playedByWinner, TAGOccurrenceStatSummary playedByAll, String xLabel, Font fontStyle, int gridHeight) {
        double[] winRate = new double[playedByWinner.getElements().size()];
        String[] xTicks = new String[playedByWinner.getElements().size()];
        int i = 0;
        for (Object component: playedByWinner.getElements().keySet()) {
            if (component.equals("")) continue;
            winRate[i] = playedByWinner.getElements().get(component)*100.0 / playedByAll.getElements().get(component);
            xTicks[i] = (String) component;
            i++;
        }
        // Create bar plot
        BarPlot barPlot = new BarPlot(winRate, "Win Rate", xLabel, fontSize, fontStyle, fontColor);
        barPlot.setxTicks(xTicks);
        setDefaultBarPlotProperties(barPlot);
        barPlot.setOrderData(true);
        barPlot.setDrawYvalue(false);

        // Add it to scroll pane
        JScrollPane pane = new JScrollPane(barPlot);
        pane.setOpaque(false);
        pane.setBorder(null);
        pane.getViewport().setOpaque(false);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.getHorizontalScrollBar().setOpaque(false);
        pane.getHorizontalScrollBar().setUnitIncrement(16);
        pane.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = fontColor;
                this.trackColor = backgroundColor;
            }
        });
        pane.setPreferredSize(new Dimension(320, 170));

        // Add pane to frame
        gridBagConstraints.gridheight = gridHeight;
        gridBagConstraints.gridy ++;
        getContentPane().add(pane, gridBagConstraints);
        gridBagConstraints.gridheight = 1;
    }

    protected void setDefaultBarPlotProperties(BarPlot barPlot) {
        barPlot.setUnitHeight(20);
        barPlot.setPadding(1);
        barPlot.setMaxWidth(300);
        barPlot.setMaxHeight(150);
        barPlot.setHoverOverWidth(100);
        barPlot.setHoverOverHeight(20);
        barPlot.setBarColor(new Color(174, 241, 124, 190));
        barPlot.setHoverOverColor(new Color(0,0,0,150));
        barPlot.setStretchY(true);
        barPlot.setReverseOrder(true);
        barPlot.setOrderData(false);
    }

    protected void setDefaultDotPlotProperties(DotPlot dotPlot) {
        dotPlot.setDotColor(new Color(22, 230, 250, 40));
        dotPlot.setOutlineColor(fontColor);
        dotPlot.setMaxWidth(300);
        dotPlot.setMaxHeight(20);
        dotPlot.setPadding(2);
    }

    protected void setDefaultPiePlotProperties(PiePlot piePlot) {
        piePlot.setMaxWidth(200);
        piePlot.setMaxHeight(200);
    }

    static StatsVisualiser getVisualiserForGame(GameType gameType, List<MetricsGameListener> listeners) {
        switch (gameType) {
            case TerraformingMars: return new TMStatsVisualiser(listeners);
            default: return null;
        }
    }
}
