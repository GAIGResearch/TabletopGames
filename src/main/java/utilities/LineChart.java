package utilities;

import org.knowm.xchart.*;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.Marker;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class LineChart extends JFrame {

    public LineChart(double[] xData, double[] yData, double[] yErr, String title, String xLabel, String yLabel, String label, boolean save) {

        // Create Chart
        XYChart chart = new XYChartBuilder()
                        .width(600)
                        .height(400)
                        .title("")
                        .xAxisTitle(xLabel)
                        .yAxisTitle(yLabel)
                        .build();
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setMarkerSize(5);

        // Data points y+err, then data points y-err descending
        double[] xxData = new double[xData.length*2+1];
        double[] yyData = new double[yData.length*2+1];
        for (int i = 0; i < xData.length; i++) {
            xxData[i] = xData[i];
            xxData[xData.length*2 - i - 1] = xData[i];
            yyData[i] = yData[i] + yErr[i];
            yyData[yData.length*2 - i - 1] = yData[i] - yErr[i];
        }
        // Repeat first point to close polygon
        xxData[xData.length*2] = xData[0];
        yyData[xData.length*2] = yData[0] + yErr[0];

        XYSeries ser = chart.addSeries(label+"1", xData, yData);
        ser.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        XYSeries series = chart.addSeries(label, xxData, yyData);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.PolygonArea);
        series.setFillColor(new Color(63, 110, 135, 122));
        series.setMarker(new Marker() {
            @Override
            public void paint(Graphics2D graphics2D, double v, double v1, int i) {}
        });
        series.setLineStyle(SeriesLines.NONE);

        // Save it in high-res
        if (save) {
            try {
                BitmapEncoder.saveBitmapWithDPI(chart, "./chart_" + title + ".png", BitmapEncoder.BitmapFormat.PNG, 300);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
}
