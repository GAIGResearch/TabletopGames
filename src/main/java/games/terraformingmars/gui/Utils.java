package games.terraformingmars.gui;

import utilities.ImageIO;
import utilities.Vector2D;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

public class Utils {

    public final static Color[] playerColors = new Color[]{Color.yellow, Color.red, Color.pink, Color.green, Color.cyan};

    public static void drawShadowString(Graphics2D g, String text, int x, int y) {
        drawShadowString(g, text, x, y, null, null);
    }

    public static void drawShadowString(Graphics2D g, String text, int x, int y, Color color, Color shadow) {
        TextLayout textLayout = new TextLayout(text, g.getFont(), g.getFontRenderContext());

        if (shadow == null) shadow = Color.black;

        g.setPaint(shadow);
        textLayout.draw(g, x + 2, y + 2);

        if (color == null) color = Color.white;  // white default
        g.setPaint(color);
        textLayout.draw(g, x, y);
    }

    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle2D rectToCenterIn, Color color, Color shadow, int size) {
        Font f = g.getFont();

        if (size != -1) {
            g.setFont(new Font(f.getName(), f.getStyle(), size));
        }
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        // Determine the X coordinate for the text
        int xText = (int)(rectToCenterIn.getX() + (rectToCenterIn.getWidth() - metrics.stringWidth(text)) / 2);
        int yText = (int)(rectToCenterIn.getY() + ((rectToCenterIn.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent());
        drawShadowString(g, text, xText, yText, color, shadow);

        g.setFont(f);
    }

    public static void drawStringCentered(Graphics2D g, String text, Rectangle2D rectToCenterIn, Color color, int size) {
        Font f = g.getFont();

        if (size != -1) {
            g.setFont(new Font(f.getName(), f.getStyle(), size));
        }
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        // Adjust size of text so it fits in given rect
        while (metrics.stringWidth(text) > rectToCenterIn.getWidth()) {
            size -= 1;
            g.setFont(new Font(f.getName(), f.getStyle(), size));
            metrics = g.getFontMetrics(g.getFont());
        }
        // Determine the X coordinate for the text
        int xText = (int)(rectToCenterIn.getX() + (rectToCenterIn.getWidth() - metrics.stringWidth(text)) / 2);
        int yText = (int)(rectToCenterIn.getY() + ((rectToCenterIn.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent());

        g.setColor(color);
        g.drawString(text, xText, yText);

        g.setFont(f);
    }

    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle2D rectToCenterIn) {
        drawShadowStringCentered(g, text, rectToCenterIn, null, null, -1);
    }
    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle2D rectToCenterIn, Color color) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, null, -1);
    }
    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle2D rectToCenterIn, Color color, Color shadow) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, shadow, -1);
    }

    public static void drawImage(Graphics2D g, String path, int x, int y, int width, int height) {
        Image image = ImageIO.GetInstance().getImage(path);
        drawImage(g, image, x, y, width, height);
    }

    public static void drawImage(Graphics2D g, Image img, int x, int y, int width, int height) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scaleW = width*1.0/w;
        double scaleH = height*1.0/h;
        g.drawImage(img, x, y, (int) (w*scaleW), (int) (h*scaleH), null);
    }

    public static Rectangle2D drawImage(Graphics2D g, Image img, int x, int y, int size) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scale;
        if (w > h) scale = size*1.0/w;
        else scale = size*1.0/h;
        g.drawImage(img, x, y, (int) (w*scale), (int) (h*scale), null);

        return new Rectangle2D.Double(x, y, (int) (w*scale), (int) (h*scale));
    }

    public static Vector2D scaleLargestDimImg(Image img, int size) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scale;
        if (w > h) scale = size*1.0/w;
        else scale = size*1.0/h;
        return new Vector2D((int) (w*scale), (int) (h*scale));
    }
}
