package gui.plotting;


import javax.swing.*;
import java.awt.*;

public class PiePlot extends JComponent {
    double[] data;
    String[] dataLabels;
    double sumData;

    public static int fontSize = 16;
    public static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    public static Font defaultFontSmall = new Font("Prototype", Font.BOLD, fontSize-5);
    public static Color fontColor = Color.white;

    final Dimension size;
    final static int padding = 1;
    int maxWidth = 200, maxHeight = 200;
    final static Color[] sliceColors = new Color[] {
            new Color(124, 241, 179, 190),
            new Color(124, 227, 241, 190),
//            new Color(124, 192, 241, 190),
            new Color(140, 124, 241, 190),
            new Color(241, 124, 239, 190),
//            new Color(241, 124, 190, 190),
            new Color(241, 124, 124, 190),
    };

    public PiePlot(double[] data, String[] dataLabels) {
        this.data = data;
        this.dataLabels = dataLabels;
        sumData = 0;
        for (double datum : data) {
            sumData += datum;
        }

        size = new Dimension(maxWidth, maxHeight);
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(defaultFontSmall);
        FontMetrics fm = g2.getFontMetrics();

        // Draw data
        int startAngle = 0;
        double total = 0;
        for (int i = 0; i < data.length; i++) {
            int arcAngle = (int) (data[i] * 360 / sumData);
            if (i == data.length-1) arcAngle = padding + (int)Math.ceil((sumData - total) * 360 / sumData);
            total += data[i];

            g2.setColor(sliceColors[i]);
            g2.fillArc(0, 0, maxWidth, maxHeight, startAngle, arcAngle);
            g2.setColor(fontColor);
            g2.drawArc(0, 0, maxWidth, maxHeight, startAngle, arcAngle);

            startAngle += arcAngle;
        }

        // Draw labels
//        startAngle = 0;
//        total = 0;
//        for (int i = 0; i < data.length; i++) {
//            int arcAngle = (int) (data[i] * 360 / sumData);
//            if (i == data.length-1) arcAngle = padding + (int)Math.ceil((sumData - total) * 360 / sumData);
//            total += data[i];
//
//            double angle = Math.toRadians(startAngle + (arcAngle / 2.));
//            int labelWidth = fm.stringWidth(dataLabels[i]);
//            double x = maxWidth/2. + Math.cos(angle) * (maxWidth / 4. + labelWidth / 2.);
//            double y = maxHeight/2. + Math.sin(angle) * (maxHeight / 4. + fontSize / 2.);
//            x -= labelWidth / 2.;
//            Graphics2D g2d = (Graphics2D) g2.create();
//            g2d.rotate(startAngle + (arcAngle / 2.), x + labelWidth/2., y + fontSize/2.);
//            g2d.drawString(dataLabels[i], (int)x, (int)y);
//            g2d.dispose();
//
//            startAngle += arcAngle;
//        }

        // Draw outline
        g2.setColor(fontColor);
        g2.drawOval(0, 0, maxWidth, maxHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

}
