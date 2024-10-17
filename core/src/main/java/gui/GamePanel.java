package gui;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JComponent {
    Image background;
    Color bgColor;
    boolean keepBackgroundRatio = true;
    float alpha = 0.3f;

    public void setBackground(Image background) {
        this.background = background;
    }
    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }

    public void setKeepBackgroundRatio(boolean keepBackgroundRatio) {
        this.keepBackgroundRatio = keepBackgroundRatio;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (background != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));

            Rectangle r = this.getBounds();
            int h = r.height;
            int w = r.width;

            int picW = background.getWidth(null);
            int picH = background.getHeight(null);
            double scale = w*1.0/picW;
            double s2 = h*1.0/picH;
            if (keepBackgroundRatio) {
                if (s2 > scale) scale = s2;
                else s2 = scale;
            }
            g2d.drawImage(background, 0, 0, (int)(picW * scale), (int)(picH * s2), null);
            g2d.dispose();
        } else if (bgColor != null) {
            g.setColor(bgColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }
}
