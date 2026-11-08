package games.perudo.gui;

import java.awt.*;

import static games.perudo.PerudoParameters.PACO;

/**
 * Draws Perudo dice: pips for the ordinary faces, a gold star for a Paco, and a dark die with a question mark for
 * a die the viewer may not see.
 */
public class PerudoDiceView {

    static final Color DIE = new Color(250, 250, 245);
    static final Color HIDDEN_DIE = new Color(70, 45, 30);
    static final Color PACO_STAR = new Color(214, 160, 20);

    // pip positions on a 3 x 3 grid, by face (index 0 unused)
    private static final int[][][] PIPS = {
            {},
            {{1, 1}},
            {{0, 0}, {2, 2}},
            {{0, 0}, {1, 1}, {2, 2}},
            {{0, 0}, {2, 0}, {0, 2}, {2, 2}},
            {{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}},
            {{0, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {2, 2}}
    };

    private PerudoDiceView() {
    }

    /**
     * Draws a die showing the face, with its top-left corner at (x, y).
     */
    public static void drawDie(Graphics2D g, int x, int y, int size, int face) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = size / 4;
        g.setColor(DIE);
        g.fillRoundRect(x, y, size, size, arc, arc);
        g.setColor(Color.darkGray);
        g.drawRoundRect(x, y, size, size, arc, arc);
        if (face == PACO) {
            g.setColor(PACO_STAR);
            g.fillPolygon(star(x + size / 2.0, y + size / 2.0, size * 0.4, size * 0.17));
        } else if (face < PIPS.length) {
            g.setColor(Color.black);
            int pip = Math.max(3, size / 6);
            double step = size / 4.0;
            for (int[] p : PIPS[face]) {
                int cx = (int) Math.round(x + step * (p[0] + 1));
                int cy = (int) Math.round(y + step * (p[1] + 1));
                g.fillOval(cx - pip / 2, cy - pip / 2, pip, pip);
            }
        } else {
            // a die with more faces than pips can show gets its number
            drawCentred(g, String.valueOf(face), x, y, size, Color.black);
        }
    }

    /**
     * Draws a die the viewer may not see, with its top-left corner at (x, y).
     */
    public static void drawHiddenDie(Graphics2D g, int x, int y, int size) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = size / 4;
        g.setColor(HIDDEN_DIE);
        g.fillRoundRect(x, y, size, size, arc, arc);
        g.setColor(Color.black);
        g.drawRoundRect(x, y, size, size, arc, arc);
        drawCentred(g, "?", x, y, size, Color.lightGray);
    }

    private static void drawCentred(Graphics2D g, String text, int x, int y, int size, Color colour) {
        Font old = g.getFont();
        g.setFont(old.deriveFont(Font.BOLD, size * 0.5f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(colour);
        g.drawString(text, x + (size - fm.stringWidth(text)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
        g.setFont(old);
    }

    private static Polygon star(double cx, double cy, double outer, double inner) {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double angle = Math.PI / 5 * i - Math.PI / 2;
            star.addPoint((int) Math.round(cx + r * Math.cos(angle)), (int) Math.round(cy + r * Math.sin(angle)));
        }
        return star;
    }
}
