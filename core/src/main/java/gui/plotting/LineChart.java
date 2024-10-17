package gui.plotting;

import org.knowm.xchart.*;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.Marker;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LineChart extends JFrame {

    XYChart chart;
    Color[] colors = new Color[]{
            new Color(47, 132, 220),
            new Color(97, 220, 108),
            new Color(220, 97, 79),
            new Color(112, 220, 219),
            new Color(220, 215, 44),
            new Color(220, 79, 194),
            new Color(90, 220, 169),
            new Color(220, 134, 44),
            new Color(147, 71, 220),
            new Color(161, 220, 46),
            new Color(220, 113, 135),
            new Color(77, 70, 220),
            new Color(41, 220, 58),
            new Color(220, 29, 49),
            new Color(21, 187, 220),
    };
    int nSeries;

    public LineChart(String title, String xLabel, String yLabel) {

        // Create Chart
        chart = new XYChartBuilder()
                        .width(600)
                        .height(300)
                        .title("")
                        .xAxisTitle(xLabel)
                        .yAxisTitle(yLabel)
                        .build();
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setMarkerSize(0);
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

    public void addSeries(double[] yData, String label) {
        Color c = colors[nSeries];
        double[] xData = new double[yData.length];
        for (int i = 0; i < yData.length; i++) {
            xData[i] = i;
        }

        XYSeries ser = chart.addSeries(label, xData, yData);
        ser.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        ser.setLineColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 130));
        ser.setMarkerColor(c);
        ser.setLineStyle(SeriesLines.SOLID);

        nSeries++;

        try {
            repaint();
        } catch (Exception ignored) {}
    }

    public void addSeriesErrorShading(double[] yData, double[] yErr, String label) {
        Color c = colors[nSeries];
        double[] xData = new double[yData.length];
        double[] xxData = new double[yData.length*2+1];
        double[] yyData = new double[yData.length*2+1];
        for (int i = 0; i < yData.length; i++) {
            xData[i] = i;

            // x points ascending, then descending
            xxData[i] = i;
            xxData[yData.length*2 - i - 1] = i;

            // Data points y+err, then data points y-err descending
            yyData[i] = yData[i] + yErr[i];
            yyData[yData.length*2 - i - 1] = yData[i] - yErr[i];
        }
        // Repeat first point to close polygon
        xxData[yData.length*2] = 0;
        yyData[yData.length*2] = yData[0] + yErr[0];

        XYSeries ser = chart.addSeries(label, xData, yData);
        ser.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        ser.setLineColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
        ser.setMarkerColor(c);
        ser.setLineStyle(SeriesLines.DASH_DASH);

        XYSeries series = chart.addSeries(label+"1", xxData, yyData);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.PolygonArea);
        series.setFillColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
        series.setMarker(new Marker() {
            @Override
            public void paint(Graphics2D graphics2D, double v, double v1, int i) {}
        });
        series.setLineStyle(SeriesLines.NONE);
        series.setShowInLegend(false);

        nSeries++;

        try {
            repaint();
        } catch (Exception ignored) {}
    }

    public void save(String filename) {
        try {
            if (filename == null) {
                filename = "./chart_" + chart.getTitle() + ".png";
            }
            BitmapEncoder.saveBitmapWithDPI(chart, filename, BitmapEncoder.BitmapFormat.PNG, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        double[] data = new double[]{10.0, 5.0, 10.0, 5.0};
        double[] data2 = new double[]{2.0, 5.0, 3.0, 7.0};
        double[] err = new double[]{1.0, 1.0, 1.0, 1.0};

        LineChart lc = new LineChart("title", "x", "y");
        lc.addSeries(data, "label");
        lc.addSeriesErrorShading(data2, err, "label2");
    }
}
