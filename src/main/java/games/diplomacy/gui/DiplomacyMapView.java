package games.diplomacy.gui;

import core.components.BoardNode;
import core.properties.PropertyVector2D;
import games.diplomacy.*;
import games.diplomacy.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;

import static core.CoreConstants.nameHash;

/**
 * The board as a schematic map: each province a disc at its "position" in the map file (sea blue, land buff, a
 * supply centre ringed and tinted with its owner's colour), joined to its neighbours, with the units on it (an army
 * a disc, a fleet a boat-shaped badge, in their power's colour). Drawn over it, the orders of the last resolved
 * phase (a move an arrow, black if it succeeded and red and dashed if not; supports green, convoys blue), and the
 * orders given so far this phase by the powers whose orders the viewer may see (purple). In a retreat phase, the
 * dislodged units are drawn beside their province with a red border.
 */
public class DiplomacyMapView extends JComponent {

    static final double SCALE = 0.9;
    static final int WIDTH = 900, HEIGHT = 700, RADIUS = 14, UNIT = 10;
    static final Color BACKGROUND = new Color(236, 229, 210);
    static final Color SEA = new Color(158, 196, 228);
    static final Color LAND = new Color(214, 198, 160);
    static final Color LINK = new Color(170, 160, 140);
    static final Color SEA_LINK = new Color(120, 160, 200);
    static final Color SUCCESS = Color.black;
    static final Color FAILURE = new Color(200, 30, 30);
    static final Color SUPPORT = new Color(30, 140, 50);
    static final Color CONVOY = new Color(30, 90, 200);
    static final Color PENDING = new Color(140, 60, 170);
    static final Color RETREAT = new Color(230, 120, 0);
    // the unit colours of the rulebook: Austria red, England dark blue, France light blue, Germany black, Italy
    // green, Russia white, Turkey yellow
    static final Color[] POWER_COLOURS = {new Color(200, 40, 40), new Color(25, 35, 120), new Color(120, 205, 250),
            new Color(55, 55, 55), new Color(40, 150, 60), new Color(250, 250, 250), new Color(235, 200, 40)};

    final DiplomacyMap map;
    final Point[] positions;
    final IntPredicate ordersVisible;
    DiplomacyGameState state;
    Map<DiplomacyProvince, Color> outlines = new HashMap<>();
    Function<MouseEvent, String> toolTips;

    /**
     * @param ordersVisible whether the orders a power has given this phase may be shown (to this viewer)
     */
    public DiplomacyMapView(DiplomacyMap map, IntPredicate ordersVisible) {
        this.map = map;
        this.ordersVisible = ordersVisible;
        positions = new Point[map.nProvinces()];
        for (DiplomacyProvince p : map.provinces()) {
            BoardNode node = map.getBoard().getNodeByStringProperty(nameHash, p.name());
            if (node.getProperty("position") instanceof PropertyVector2D v) {
                positions[p.index()] = new Point((int) (v.values.getX() * SCALE), (int) (v.values.getY() * SCALE));
            } else {
                double angle = 2 * Math.PI * p.index() / map.nProvinces();
                positions[p.index()] = new Point((int) (WIDTH / 2 + 0.42 * WIDTH * Math.cos(angle)),
                        (int) (HEIGHT / 2 + 0.42 * HEIGHT * Math.sin(angle)));
            }
        }
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void update(DiplomacyGameState state) {
        this.state = state;
        repaint();
    }

    static Color powerColour(int power) {
        return POWER_COLOURS[power % POWER_COLOURS.length];
    }

    static Color textOn(Color c) {
        return c.getRed() * 0.3 + c.getGreen() * 0.59 + c.getBlue() * 0.11 > 150 ? Color.black : Color.white;
    }

    /**
     * The province whose disc is at the point, or null.
     */
    public DiplomacyProvince provinceAt(Point point) {
        for (DiplomacyProvince p : map.provinces())
            if (point.distance(positions[p.index()]) <= RADIUS + 3)
                return p;
        return null;
    }

    /**
     * Provinces to outline, with the colour of each (replacing any outlines before).
     */
    public void setOutlines(Map<DiplomacyProvince, Color> outlines) {
        this.outlines = outlines;
        repaint();
    }

    public void setToolTips(Function<MouseEvent, String> toolTips) {
        this.toolTips = toolTips;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return toolTips == null ? null : toolTips.apply(event);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        if (state == null) return;

        g.setStroke(new BasicStroke(1.2f));
        for (DiplomacyProvince a : map.provinces())
            for (DiplomacyProvince b : map.adjacent(a))
                if (b.index() > a.index()) {
                    boolean water = a.type() == DiplomacyProvince.Type.SEA || b.type() == DiplomacyProvince.Type.SEA;
                    g.setColor(water ? SEA_LINK : LINK);
                    Point pa = positions[a.index()], pb = positions[b.index()];
                    g.drawLine(pa.x, pa.y, pb.x, pb.y);
                }

        for (DiplomacyProvince p : map.provinces())
            drawProvince(g, p);

        for (DiplomacyResult r : state.getLastResults())
            drawOrder(g, r.order(), r.success() ? null : FAILURE, false);
        for (int power = 0; power < state.getNPlayers(); power++)
            if (ordersVisible.test(power))
                for (DiplomacyOrder o : state.getOrders(power))
                    drawOrder(g, o, PENDING, true);

        for (DiplomacyProvince p : map.provinces()) {
            DiplomacyUnit u = state.getUnit(p);
            Point c = positions[p.index()];
            if (u != null)
                drawUnit(g, u, c.x, c.y, false);
            DiplomacyUnit d = state.getDislodged(p);
            if (d != null)
                drawUnit(g, d, c.x + RADIUS, c.y - RADIUS, true);
        }

        for (Map.Entry<DiplomacyProvince, Color> e : outlines.entrySet()) {
            Point c = positions[e.getKey().index()];
            g.setColor(e.getValue());
            g.setStroke(new BasicStroke(3f));
            g.drawOval(c.x - RADIUS - 4, c.y - RADIUS - 4, 2 * RADIUS + 8, 2 * RADIUS + 8);
        }
    }

    private void drawProvince(Graphics2D g, DiplomacyProvince p) {
        Point c = positions[p.index()];
        Color fill = p.type() == DiplomacyProvince.Type.SEA ? SEA : LAND;
        int owner = state.getOwner(p);
        if (p.supplyCentre())
            fill = owner < 0 ? new Color(246, 241, 228) : blend(powerColour(owner), Color.white, 0.55);
        g.setColor(fill);
        g.fillOval(c.x - RADIUS, c.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
        g.setColor(p.supplyCentre() ? Color.black : new Color(120, 110, 90));
        g.setStroke(new BasicStroke(p.supplyCentre() ? 2.2f : 1f));
        g.drawOval(c.x - RADIUS, c.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
        if (p.supplyCentre()) {
            // a star marks a supply centre
            g.fill(star(c.x + RADIUS - 2, c.y - RADIUS + 2, 4.5));
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(p.type() == DiplomacyProvince.Type.SEA ? new Color(30, 60, 110) : Color.black);
        drawCentred(g, p.name(), c.x, c.y + RADIUS + 11);
    }

    private void drawUnit(Graphics2D g, DiplomacyUnit u, int x, int y, boolean dislodged) {
        Color colour = powerColour(u.owner());
        Shape shape;
        if (u.isFleet()) {
            // a boat: a flat deck over a narrower keel
            Path2D boat = new Path2D.Double();
            boat.moveTo(x - UNIT - 2, y - 4);
            boat.lineTo(x + UNIT + 2, y - 4);
            boat.lineTo(x + UNIT - 3, y + 7);
            boat.lineTo(x - UNIT + 3, y + 7);
            boat.closePath();
            shape = boat;
        } else {
            shape = new java.awt.geom.Ellipse2D.Double(x - UNIT + 1, y - UNIT + 1, 2 * UNIT - 2, 2 * UNIT - 2);
        }
        g.setColor(colour);
        g.fill(shape);
        g.setColor(dislodged ? FAILURE : Color.black);
        g.setStroke(new BasicStroke(dislodged ? 2.5f : 1.3f));
        g.draw(shape);
        g.setColor(textOn(colour));
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        drawCentred(g, u.type().letter, x, y + (u.isFleet() ? 5 : 4));
        if (!u.coast().isEmpty()) {
            g.setColor(Color.black);
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            drawCentred(g, u.coast(), x, y - UNIT - 1);
        }
    }

    /**
     * Draws an order: a move or retreat as an arrow, a support or convoy as a dotted line to the unit (or the move)
     * it helps, a build as a ring and a disband as a cross. colour null means the order's usual colour.
     */
    private void drawOrder(Graphics2D g, DiplomacyOrder order, Color colour, boolean pending) {
        float width = pending ? 2f : 2.6f;
        boolean dashed = colour == FAILURE;
        if (order instanceof Move m) {
            arrow(g, centre(m.unit), centre(m.to.province()), colour == null ? SUCCESS : colour, width, dashed);
        } else if (order instanceof Retreat r) {
            arrow(g, centre(r.unit), centre(r.to.province()), colour == null ? RETREAT : colour, width, dashed);
        } else if (order instanceof SupportHold s) {
            line(g, centre(s.unit), centre(s.supported), colour == null ? SUPPORT : colour, width, true);
        } else if (order instanceof SupportMove s) {
            line(g, centre(s.unit), midpoint(s.from, s.to), colour == null ? SUPPORT : colour, width, true);
        } else if (order instanceof Convoy c) {
            line(g, centre(c.unit), midpoint(c.from, c.to), colour == null ? CONVOY : colour, width, true);
        } else if (order instanceof Build b) {
            // a dashed ring, distinct from the solid outlines of the clickable map
            Point p = centre(b.location.province());
            g.setColor(colour == null ? SUCCESS : colour);
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 4f}, 0f));
            g.drawOval(p.x - RADIUS - 7, p.y - RADIUS - 7, 2 * RADIUS + 14, 2 * RADIUS + 14);
        } else if (order instanceof Disband d) {
            Point p = centre(d.unit);
            g.setColor(colour == null || colour == FAILURE ? FAILURE : colour);
            g.setStroke(new BasicStroke(width + 1));
            g.drawLine(p.x - 9, p.y - 9, p.x + 9, p.y + 9);
            g.drawLine(p.x - 9, p.y + 9, p.x + 9, p.y - 9);
        }
    }

    private Point centre(DiplomacyProvince p) {
        return positions[p.index()];
    }

    private Point midpoint(DiplomacyProvince a, DiplomacyProvince b) {
        Point pa = centre(a), pb = centre(b);
        return new Point((pa.x + pb.x) / 2, (pa.y + pb.y) / 2);
    }

    private static void line(Graphics2D g, Point a, Point b, Color colour, float width, boolean dotted) {
        g.setColor(colour);
        g.setStroke(dotted ? new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{2f, 5f}, 0f) : new BasicStroke(width));
        g.draw(new Line2D.Double(a, b));
    }

    /**
     * An arrow from a to b, stopping at the edge of b's disc; a failed move ends in a cross instead of a head.
     */
    private static void arrow(Graphics2D g, Point a, Point b, Color colour, float width, boolean failed) {
        double dx = b.x - a.x, dy = b.y - a.y, len = Math.hypot(dx, dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        double sx = a.x + ux * (RADIUS - 2), sy = a.y + uy * (RADIUS - 2);
        double ex = b.x - ux * (RADIUS + 2), ey = b.y - uy * (RADIUS + 2);
        g.setColor(colour);
        g.setStroke(failed ? new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f,
                new float[]{6f, 4f}, 0f) : new BasicStroke(width));
        g.draw(new Line2D.Double(sx, sy, ex, ey));
        g.setStroke(new BasicStroke(width));
        if (failed) {
            double s = 5;
            g.draw(new Line2D.Double(ex - s, ey - s, ex + s, ey + s));
            g.draw(new Line2D.Double(ex - s, ey + s, ex + s, ey - s));
        } else {
            Path2D head = new Path2D.Double();
            head.moveTo(ex, ey);
            head.lineTo(ex - ux * 10 - uy * 5, ey - uy * 10 + ux * 5);
            head.lineTo(ex - ux * 10 + uy * 5, ey - uy * 10 - ux * 5);
            head.closePath();
            g.fill(head);
        }
    }

    private static Shape star(double cx, double cy, double r) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            double radius = i % 2 == 0 ? r : r * 0.45;
            double x = cx + radius * Math.cos(angle), y = cy - radius * Math.sin(angle);
            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.closePath();
        return star;
    }

    static Color blend(Color a, Color b, double t) {
        return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t), (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    private static void drawCentred(Graphics2D g, String text, int x, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x - fm.stringWidth(text) / 2, baseline);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
