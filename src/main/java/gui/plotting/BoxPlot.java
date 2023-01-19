package gui.plotting;

import org.knowm.xchart.*;
import org.knowm.xchart.style.BoxStyler;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class BoxPlot extends JFrame {

    BoxChart chart;
    int nSeries;

    public BoxPlot(String title, String xLabel, String yLabel) {

        // Create Chart
        chart = new BoxChartBuilder()
                        .height(400)
                        .title("")
                        .xAxisTitle(xLabel)
                        .yAxisTitle(yLabel)
                        .build();
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setToolTipsEnabled(true);
        chart.getStyler().setMarkerSize(8);
        chart.getStyler().setBoxplotCalCulationMethod(BoxStyler.BoxplotCalCulationMethod.N_PLUS_1);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.getStyler().setAxisTitleFont(new Font("Arial", Font.BOLD, 20));
        chart.getStyler().setAxisTickLabelsFont(new Font("Arial", Font.PLAIN, 16));
        chart.getStyler().setLegendFont(new Font("Arial", Font.PLAIN, 18));
        chart.getStyler().setPlotBackgroundColor(Color.white);
        chart.getStyler().setChartBackgroundColor(Color.white);

        setTitle(title);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Chart
        JPanel chartPanel = new XChartPanel<>(chart);
        add(chartPanel, BorderLayout.CENTER);

        // Display the window.
        pack();
        setVisible(true);
    }

    public void addSeries(List<Double> yData, String label) {
        BoxSeries ser = chart.addSeries(label, yData);
        nSeries++;

        try {
            repaint();
        } catch (Exception ignored) {}
    }

    public void save(String filename) {
        try {
            if (filename == null) {
                filename = "./box_" + chart.getTitle() + ".png";
            }
            BitmapEncoder.saveBitmapWithDPI(chart, filename, BitmapEncoder.BitmapFormat.PNG, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BoxPlot lc = new BoxPlot("title", "x", "y");
        lc.addSeries(Arrays.asList(40.0, 30.0, 20.0, 60.0, 250.0), "label");
        lc.addSeries(Arrays.asList(-20.0, -10.0, -30.0, -15.0, -25.0), "label2");
    }
}
