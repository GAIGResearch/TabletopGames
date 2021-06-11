package gui;

import javax.swing.*;
import java.awt.*;

public class TiledImage extends JPanel {
    TexturePaint paint;

    public TiledImage(TexturePaint paint) {
        this.paint = paint;
    }

    protected void paintComponent(Graphics g) {
        ((Graphics2D)g).setPaint(paint);
        g.fillRect(0,0, getWidth(), getHeight());
    }
}