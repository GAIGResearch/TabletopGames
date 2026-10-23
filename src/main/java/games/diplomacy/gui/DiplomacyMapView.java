package games.diplomacy.gui;

import core.components.BoardNode;
import core.properties.PropertyVector2D;
import games.diplomacy.*;
import games.diplomacy.actions.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;

import static core.CoreConstants.nameHash;

/**
 * The board drawn on the map image (DiplomacyMap.imageFile), scaled to fit. Each land province is tinted in the colour
 * of the power controlling it (left plain if nobody does), found from the map's regions image. The units stand at
 * the "position" of their province in the map file (a fleet on a separate coast at that coast's position): an army a
 * cannon, a fleet a ship, in their power's colour. Drawn over them, the orders of the last resolved phase (a move an
 * arrow, black if it succeeded and red and dashed if not; supports green, convoys blue), and the orders given so far
 * this phase by the powers whose orders the viewer may see (purple). In a retreat phase, the dislodged units are
 * drawn beside their province with a red border. Everything is drawn in the coordinates of the map image.
 */
public class DiplomacyMapView extends JComponent {

    static final double SCALE = 0.68;
    // about half the width of a unit's icon (ARMY and FLEET, enlarged by ICON_SCALE)
    static final int UNIT = 17;
    static final double ICON_SCALE = 1.2;
    // the plain land of the map image: the pixels tinted with a province's controller
    static final int NEUTRAL = 0xffece2c6;
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

    // an army: a cannon, its barrel raised over a carriage on a large wheel, centred on the origin
    static final Shape ARMY;
    // a fleet: a ship, a hull with a raised bow, a superstructure and a funnel, centred on the origin
    static final Shape FLEET;

    static {
        Area army = new Area(new RoundRectangle2D.Double(-15, -4, 26, 7, 6, 6));
        army.transform(AffineTransform.getRotateInstance(-0.38, -2, 0));
        army.add(new Area(new Rectangle2D.Double(-13, 0, 18, 5)));
        Area wheel = new Area(new Ellipse2D.Double(-8, -1, 14, 14));
        wheel.subtract(new Area(new Ellipse2D.Double(-3, 4, 4, 4)));
        army.add(wheel);
        army.transform(AffineTransform.getTranslateInstance(0, -4));
        army.transform(AffineTransform.getScaleInstance(ICON_SCALE, ICON_SCALE));
        ARMY = army;

        Path2D hull = new Path2D.Double();
        hull.moveTo(-19, 0);
        hull.lineTo(19, 0);
        hull.lineTo(22, -4);
        hull.lineTo(16, 8);
        hull.lineTo(-15, 8);
        hull.closePath();
        Area fleet = new Area(hull);
        fleet.add(new Area(new Rectangle2D.Double(-11, -6, 18, 6)));
        fleet.add(new Area(new Rectangle2D.Double(-6, -11, 9, 5)));
        fleet.add(new Area(new Rectangle2D.Double(-3, -16, 3, 5)));
        fleet.transform(AffineTransform.getTranslateInstance(0, 3));
        fleet.transform(AffineTransform.getScaleInstance(ICON_SCALE, ICON_SCALE));
        FLEET = fleet;
    }

    final DiplomacyMap map;
    final IntPredicate ordersVisible;
    final BufferedImage base;
    final int imageWidth, imageHeight;
    // the province index at each pixel of the image, or -1 (borders, impassable land)
    final int[] region;
    // whether each pixel lies within a few pixels of its province's edge, where an outline is drawn
    final boolean[] nearEdge;
    final Point[] positions;
    final Map<DiplomacyLocation, Point> coastPositions = new HashMap<>();
    // the map as last tinted, and the controllers it was tinted for
    BufferedImage tinted;
    int[] tintedFor;
    BufferedImage outlineImage;
    DiplomacyGameState state;
    Map<DiplomacyProvince, Color> outlines = new HashMap<>();
    Function<MouseEvent, String> toolTips;

    /**
     * @param ordersVisible whether the orders a power has given this phase may be shown (to this viewer)
     */
    public DiplomacyMapView(DiplomacyMap map, IntPredicate ordersVisible) {
        this.map = map;
        this.ordersVisible = ordersVisible;
        if (map.imageFile() == null || map.regionsFile() == null)
            throw new IllegalArgumentException(map.fileName + " gives no map image for the GUI");
        try {
            base = toArgb(ImageIO.read(new File(map.imageFile())));
            BufferedImage regions = ImageIO.read(new File(map.regionsFile()));
            imageWidth = base.getWidth();
            imageHeight = base.getHeight();
            region = new int[imageWidth * imageHeight];
            for (int y = 0; y < imageHeight; y++)
                for (int x = 0; x < imageWidth; x++)
                    region[y * imageWidth + x] = ((regions.getRGB(x, y) >> 16) & 255) - 1;
        } catch (IOException e) {
            throw new RuntimeException("Could not read the images of " + map.fileName, e);
        }
        nearEdge = new boolean[region.length];
        for (int y = 0; y < imageHeight; y++)
            for (int x = 0; x < imageWidth; x++)
                nearEdge[y * imageWidth + x] = region[y * imageWidth + x] >= 0 && nearEdge(x, y, 3);

        positions = new Point[map.nProvinces()];
        for (DiplomacyProvince p : map.provinces()) {
            BoardNode node = map.getBoard().getNodeByStringProperty(nameHash, p.name());
            positions[p.index()] = point(node, "position");
            for (String coast : p.coasts())
                coastPositions.put(new DiplomacyLocation(p, coast), point(node, "position/" + coast));
        }
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Whether a pixel within distance d of (x, y) lies outside the image or the province there.
     */
    private boolean nearEdge(int x, int y, int d) {
        int r = region[y * imageWidth + x];
        for (int dy = -d; dy <= d; dy++)
            for (int dx = -d; dx <= d; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= imageWidth || ny >= imageHeight || region[ny * imageWidth + nx] != r)
                    return true;
            }
        return false;
    }

    private Point point(BoardNode node, String property) {
        if (!(node.getProperty(property) instanceof PropertyVector2D v))
            throw new IllegalArgumentException(map.fileName + ": " + node.getComponentName() + " has no " + property);
        return new Point((int) v.values.getX(), (int) v.values.getY());
    }

    private static BufferedImage toArgb(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = copy.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return copy;
    }

    public void update(DiplomacyGameState state) {
        this.state = state;
        repaint();
    }

    static Color powerColour(int power) {
        return POWER_COLOURS[power % POWER_COLOURS.length];
    }

    /**
     * The tint of the land a power controls.
     */
    static Color territoryColour(int power) {
        return blend(powerColour(power), Color.white, 0.5);
    }

    /**
     * The scale from the map image to the component.
     */
    double scale() {
        return Math.min(getWidth() / (double) imageWidth, getHeight() / (double) imageHeight);
    }

    /**
     * The province at the point of the component (or the nearest within a few pixels, on a border), or null.
     */
    public DiplomacyProvince provinceAt(Point point) {
        double s = scale();
        int x = (int) (point.x / s), y = (int) (point.y / s);
        for (int d = 0; d <= 4; d++)
            for (int dy = -d; dy <= d; dy++)
                for (int dx = -d; dx <= d; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= imageWidth || ny >= imageHeight) continue;
                    int r = region[ny * imageWidth + nx];
                    if (r >= 0)
                        return map.provinces().get(r);
                }
        return null;
    }

    // each province's pixels as a shape in the image's coordinates, by province index
    private final Map<Integer, Shape> provinceShapes = new HashMap<>();
    // the same scaled to the component, at shapeScale
    private final Map<Integer, Shape> scaledShapes = new HashMap<>();
    private double shapeScale;

    /**
     * The province's pixels as a shape in the component's coordinates.
     */
    public Shape provinceShape(DiplomacyProvince province) {
        double s = scale();
        if (s != shapeScale) {
            scaledShapes.clear();
            shapeScale = s;
        }
        int index = province.index();
        return scaledShapes.computeIfAbsent(index, i -> AffineTransform.getScaleInstance(s, s)
                .createTransformedShape(provinceShapes.computeIfAbsent(i, this::traceProvince)));
    }

    private Shape traceProvince(int index) {
        // one rectangle for each run of the province's pixels in a row, joined into an area
        Path2D.Double runs = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int y = 0; y < imageHeight; y++)
            for (int x = 0; x < imageWidth; x++) {
                if (region[y * imageWidth + x] != index) continue;
                int start = x;
                while (x < imageWidth && region[y * imageWidth + x] == index) x++;
                runs.append(new Rectangle(start, y, x - start, 1), false);
            }
        return new Area(runs);
    }

    /**
     * Provinces to outline, with the colour of each (replacing any outlines before).
     */
    public void setOutlines(Map<DiplomacyProvince, Color> outlines) {
        // This is called on every GUI update, and the outline image is costly to remake.
        if (outlines.equals(this.outlines)) return;
        this.outlines = outlines;
        outlineImage = null;
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
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double s = scale();
        g.scale(s, s);
        if (state == null) {
            g.drawImage(base, 0, 0, null);
            g.dispose();
            return;
        }
        g.drawImage(tinted(), 0, 0, null);
        if (!outlines.isEmpty())
            g.drawImage(outlineImage(), 0, 0, null);

        for (DiplomacyResult r : state.getLastResults())
            drawOrder(g, r.order(), r.success() ? null : FAILURE, false);
        for (int power = 0; power < state.getNPlayers(); power++)
            if (ordersVisible.test(power))
                for (DiplomacyOrder o : state.getOrders(power))
                    drawOrder(g, o, PENDING, true);

        for (DiplomacyProvince p : map.provinces()) {
            DiplomacyUnit u = state.getUnit(p);
            if (u != null) {
                Point c = unitPosition(p, u);
                drawUnit(g, u, c.x, c.y, false);
            }
            DiplomacyUnit d = state.getDislodged(p);
            if (d != null) {
                Point c = unitPosition(p, d);
                drawUnit(g, d, c.x + 2 * UNIT, c.y - UNIT, true);
            }
        }
        g.dispose();
    }

    /**
     * The map with each land province tinted by its controller: made again only when control has changed.
     */
    private BufferedImage tinted() {
        int[] owners = new int[map.nProvinces()];
        for (DiplomacyProvince p : map.provinces())
            owners[p.index()] = state.getOwner(p);
        if (tinted != null && Arrays.equals(owners, tintedFor))
            return tinted;
        int[] colours = new int[owners.length];
        for (int i = 0; i < owners.length; i++)
            colours[i] = owners[i] < 0 ? NEUTRAL : territoryColour(owners[i]).getRGB();
        int[] pixels = base.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
        for (int i = 0; i < pixels.length; i++)
            if (pixels[i] == NEUTRAL && region[i] >= 0)
                pixels[i] = colours[region[i]];
        tinted = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        tinted.setRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);
        tintedFor = owners;
        return tinted;
    }

    /**
     * The outlined provinces: a band of the colour inside each one's edge, over a faint wash of it.
     */
    private BufferedImage outlineImage() {
        if (outlineImage != null)
            return outlineImage;
        int[] bands = new int[map.nProvinces()];
        int[] washes = new int[map.nProvinces()];
        for (Map.Entry<DiplomacyProvince, Color> e : outlines.entrySet()) {
            int rgb = e.getValue().getRGB() & 0xffffff;
            bands[e.getKey().index()] = 0xff000000 | rgb;
            washes[e.getKey().index()] = 0x40000000 | rgb;
        }
        int[] pixels = new int[region.length];
        for (int i = 0; i < pixels.length; i++)
            if (region[i] >= 0)
                pixels[i] = nearEdge[i] ? bands[region[i]] : washes[region[i]];
        outlineImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        outlineImage.setRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);
        return outlineImage;
    }

    private Point unitPosition(DiplomacyProvince p, DiplomacyUnit u) {
        return at(new DiplomacyLocation(p, u.coast()));
    }

    private Point at(DiplomacyLocation location) {
        return location.coast().isEmpty() ? positions[location.province().index()] : coastPositions.get(location);
    }

    private void drawUnit(Graphics2D g, DiplomacyUnit u, int x, int y, boolean dislodged) {
        Shape shape = AffineTransform.getTranslateInstance(x, y).createTransformedShape(u.isFleet() ? FLEET : ARMY);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(AffineTransform.getTranslateInstance(2, 2).createTransformedShape(shape));
        g.setColor(powerColour(u.owner()));
        g.fill(shape);
        g.setColor(dislodged ? FAILURE : Color.black);
        g.setStroke(new BasicStroke(dislodged ? 3f : 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    /**
     * Draws an order: a move or retreat as an arrow, a support or convoy as a dotted line to the unit (or the move)
     * it helps, a build as a ring and a disband as a cross. colour null means the order's usual colour.
     */
    private void drawOrder(Graphics2D g, DiplomacyOrder order, Color colour, boolean pending) {
        // the pending orders (a plan, while it is made) are drawn more strongly than the last results, to stand out
        float width = pending ? 5.5f : 3.8f;
        boolean dashed = colour == FAILURE;
        if (order instanceof Move m) {
            arrow(g, centre(m.unit), at(m.to), colour == null ? SUCCESS : colour, width, dashed);
        } else if (order instanceof Retreat r) {
            arrow(g, centre(r.unit), at(r.to), colour == null ? RETREAT : colour, width, dashed);
        } else if (order instanceof SupportHold s) {
            line(g, centre(s.unit), centre(s.supported), colour == null ? SUPPORT : colour, width);
        } else if (order instanceof SupportMove s) {
            line(g, centre(s.unit), midpoint(s.from, s.to), colour == null ? SUPPORT : colour, width);
        } else if (order instanceof Convoy c) {
            line(g, centre(c.unit), midpoint(c.from, c.to), colour == null ? CONVOY : colour, width);
        } else if (order instanceof Build b) {
            // a dashed ring, distinct from the solid outlines of the clickable map
            Point p = at(b.location);
            g.setColor(colour == null ? SUCCESS : colour);
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{6f, 5f}, 0f));
            int r = UNIT + 10;
            g.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);
        } else if (order instanceof Disband d) {
            Point p = centre(d.unit);
            g.setColor(colour == null || colour == FAILURE ? FAILURE : colour);
            g.setStroke(new BasicStroke(width + 1.5f));
            int r = UNIT + 2;
            g.drawLine(p.x - r, p.y - r, p.x + r, p.y + r);
            g.drawLine(p.x - r, p.y + r, p.x + r, p.y - r);
        }
    }

    /**
     * Where the unit in the province stands (a dislodged one before it was dislodged), or the province's position
     * if it is empty.
     */
    private Point centre(DiplomacyProvince p) {
        DiplomacyUnit u = state.getUnit(p);
        if (u == null) u = state.getDislodged(p);
        return u == null ? positions[p.index()] : unitPosition(p, u);
    }

    private Point midpoint(DiplomacyProvince a, DiplomacyProvince b) {
        Point pa = centre(a), pb = centre(b);
        return new Point((pa.x + pb.x) / 2, (pa.y + pb.y) / 2);
    }

    /**
     * A dotted line from a to b.
     */
    private static void line(Graphics2D g, Point a, Point b, Color colour, float width) {
        g.setColor(colour);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 7f}, 0f));
        g.draw(new Line2D.Double(a, b));
    }

    /**
     * An arrow from a to b, stopping short of the units at either end; a failed move ends in a cross instead of a
     * head.
     */
    private static void arrow(Graphics2D g, Point a, Point b, Color colour, float width, boolean failed) {
        double dx = b.x - a.x, dy = b.y - a.y, len = Math.hypot(dx, dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        double sx = a.x + ux * UNIT, sy = a.y + uy * UNIT;
        double ex = b.x - ux * (UNIT + 4), ey = b.y - uy * (UNIT + 4);
        g.setColor(colour);
        g.setStroke(failed ? new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f,
                new float[]{9f, 6f}, 0f) : new BasicStroke(width));
        g.draw(new Line2D.Double(sx, sy, ex, ey));
        g.setStroke(new BasicStroke(width));
        if (failed) {
            double s = 7;
            g.draw(new Line2D.Double(ex - s, ey - s, ex + s, ey + s));
            g.draw(new Line2D.Double(ex - s, ey + s, ex + s, ey - s));
        } else {
            Path2D head = new Path2D.Double();
            head.moveTo(ex + ux * 4, ey + uy * 4);
            head.lineTo(ex - ux * 12 - uy * 7, ey - uy * 12 + ux * 7);
            head.lineTo(ex - ux * 12 + uy * 7, ey - uy * 12 - ux * 7);
            head.closePath();
            g.fill(head);
        }
    }

    static Color blend(Color a, Color b, double t) {
        return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t), (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) (imageWidth * SCALE), (int) (imageHeight * SCALE));
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
