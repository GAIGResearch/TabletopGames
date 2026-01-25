package utilities;

import org.davidmoten.text.utils.WordWrap;

import java.awt.*;
import java.awt.font.TextLayout;

public class GUIUtils {

    public static void drawShadowString(Graphics2D g, String text, int x, int y) {
        drawShadowString(g, text, x, y, null, null);
    }

    public static void drawShadowString(Graphics2D g, String text, int x, int y, Color color, Color shadow) {
        TextLayout textLayout = new TextLayout(text, g.getFont(), g.getFontRenderContext());

//        if (shadow == null) shadow = TMGUI.bgColor;

        g.setPaint(shadow);
        textLayout.draw(g, x + 2, y + 2);

//        if (color == null) color = TMGUI.fontColor;  // white default
        g.setPaint(color);
        textLayout.draw(g, x, y);
    }

    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, Color shadow, int size) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, shadow, size, false);
    }

    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, Color shadow, int size, boolean split) {
        Font f = g.getFont();

        if (size != -1) {
            g.setFont(new Font(f.getName(), f.getStyle(), size));
        } else {
            size = f.getSize();
        }
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());

        if (split) {
            // Split text on multiple lines to fit in width of rect, then adjust font size to fit in height of rect
            String wrapped = WordWrap.from(text)
                            .maxWidth(rectToCenterIn.getWidth()/metrics.stringWidth("o"))
                            .insertHyphens(false) // true is the default
                            .wrap();
            String[] wraps = wrapped.split("\n");
            int h = metrics.getHeight() * wraps.length;
            while (h > rectToCenterIn.getHeight()) {
                // reduce font size until text fits in width & height
                size -= 1;
                g.setFont(new Font(f.getName(), f.getStyle(), size));
                metrics = g.getFontMetrics(g.getFont());

                wrapped = WordWrap.from(text)
                                .maxWidth(rectToCenterIn.getWidth()/metrics.stringWidth("o"))
                                .insertHyphens(false) // true is the default
                                .wrap();
                wraps = wrapped.split("\n");
                h = metrics.getHeight() * wraps.length;
            }
            int i = 0;
            int y = rectToCenterIn.y;
            for (String s : wraps) {
                drawShadowStringCentered(g, wraps[i], new Rectangle(rectToCenterIn.x, y, rectToCenterIn.width, metrics.getHeight()));
                y += metrics.getHeight();
                i++;
            }

            g.setFont(f);
            return;
        } else {
            // Adjust size of text so it fits in given width of rect
            while (metrics.stringWidth(text) > rectToCenterIn.getWidth()) {
                size -= 1;
                g.setFont(new Font(f.getName(), f.getStyle(), size));
                metrics = g.getFontMetrics(g.getFont());
            }
        }

        // Determine the X coordinate for the text
        int xText = (int)(rectToCenterIn.getX() + (rectToCenterIn.getWidth() - metrics.stringWidth(text)) / 2);
        int yText = (int)(rectToCenterIn.getY() + ((rectToCenterIn.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent());
        drawShadowString(g, text, xText, yText, color, shadow);

        g.setFont(f);
    }

    public static void drawStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, boolean split) {
        drawStringCentered(g, text, rectToCenterIn, color, -1, split);
    }

    public static void drawStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color) {
        drawStringCentered(g, text, rectToCenterIn, color, -1, false);
    }

    public static void drawStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, int size) {
        drawStringCentered(g, text, rectToCenterIn, color, size, false);
    }
    public static void drawStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, int size, boolean split) {
        Font f = g.getFont();
        g.setColor(color);

        if (size != -1) {
            g.setFont(new Font(f.getName(), f.getStyle(), size));
        } else {
            size = f.getSize();
        }
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());

        if (split) {
            // Split text on multiple lines to fit in width of rect, then adjust font size to fit in height of rect

            String wrapped = WordWrap.from(text)
                    .maxWidth(rectToCenterIn.getWidth()/metrics.stringWidth("o"))
                    .insertHyphens(false) // true is the default
                    .wrap();
            String[] wraps = wrapped.split("\n");
            int h = metrics.getHeight() * wraps.length;
            while (h > rectToCenterIn.getHeight()) {
                // reduce font size until text fits in width & height
                size -= 1;
                g.setFont(new Font(f.getName(), f.getStyle(), size));
                metrics = g.getFontMetrics(g.getFont());

                wrapped = WordWrap.from(text)
                        .maxWidth(rectToCenterIn.getWidth()/metrics.stringWidth("o"))
                        .insertHyphens(false) // true is the default
                        .wrap();
                wraps = wrapped.split("\n");
                h = metrics.getHeight() * wraps.length;
            }
            int y = rectToCenterIn.y;
            for (String s : wraps) {
//                drawShadowStringCentered(g, s, new Rectangle(rectToCenterIn.x, y, rectToCenterIn.width, metrics.getHeight()));
                g.drawString(s, rectToCenterIn.x, y);
                y += metrics.getHeight();
            }

            g.setFont(f);
            return;
        } else {
            // Adjust size of text so it fits in given width of rect
            while (metrics.stringWidth(text) > rectToCenterIn.getWidth()) {
                size -= 1;
                g.setFont(new Font(f.getName(), f.getStyle(), size));
                metrics = g.getFontMetrics(g.getFont());
            }
        }
        // Determine the X coordinate for the text
        int xText = (int)(rectToCenterIn.getX() + (rectToCenterIn.getWidth() - metrics.stringWidth(text)) / 2);
        int yText = (int)(rectToCenterIn.getY() + ((rectToCenterIn.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent());

        g.drawString(text, xText, yText);

        g.setFont(f);
    }

    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn) {
        drawShadowStringCentered(g, text, rectToCenterIn, null, null, -1, false);
    }
    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, null, -1, false);
    }
    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, Color shadow) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, shadow, -1, false);
    }

    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, boolean split) {
        drawShadowStringCentered(g, text, rectToCenterIn, null, null, -1, split);
    }
    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, boolean split) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, null, -1, split);
    }
    public static void drawShadowStringCentered(Graphics2D g, String text, Rectangle rectToCenterIn, Color color, Color shadow, boolean split) {
        drawShadowStringCentered(g, text, rectToCenterIn, color, shadow, -1, split);
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

    public static Rectangle drawImage(Graphics2D g, Image img, int x, int y, int size) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scale;
        if (w > h) scale = size*1.0/w;
        else scale = size*1.0/h;
        g.drawImage(img, x, y, (int) (w*scale), (int) (h*scale), null);

        return new Rectangle(x, y, (int) (w*scale), (int) (h*scale));
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
