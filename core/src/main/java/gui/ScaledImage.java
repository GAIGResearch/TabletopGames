package gui;


import javax.swing.*;
import java.awt.*;

public class ScaledImage extends JPanel {
    Image img;
    int w, h;
    Container frame;

    public ScaledImage(Image img, int w, int h, Container frame) {
        this.img = img;
        this.w = w;
        this.h = h;
        this.frame = frame;
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.3f));

        Rectangle r = frame.getBounds();
        h = r.height;
        w = r.width;

        int picW = img.getWidth(null);
        int picH = img.getHeight(null);
        double scale = w*1.0/picW;
        double s2 = h*1.0/picH;
        if (s2 > scale) scale = s2;
        g2d.drawImage(img, 0, 0, (int)(picW * scale), (int)(picH * scale), null);
        g2d.dispose();
    }
}
