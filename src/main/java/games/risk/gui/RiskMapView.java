package games.risk.gui;

import core.components.BoardNode;
import core.properties.PropertyVector2D;
import games.risk.RiskContinent;
import games.risk.RiskGameState;
import games.risk.RiskMap;
import games.risk.RiskTerritory;
import games.risk.actions.AttackDiceChoice;
import games.risk.actions.DefenderDice;
import games.risk.actions.MoveArmiesChoice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static core.CoreConstants.nameHash;

/**
 * The board. On a map with an SVG drawing (see {@link RiskBoardShapes}), each territory is drawn in its shape, tinted
 * in its owner's colour, with its armies in a disc of that colour; each continent is outlined in its colour; and the
 * connections across the sea are dashed lines (Alaska - Kamchatka leaves by the edges). On a map without one, each
 * territory is a disc at its "position" in the map file, filled with its owner's colour and ringed in its continent's
 * colour, with lines joining neighbours (a map file without positions has its territories set round an ellipse).
 * Either way the two territories of a pending battle or move are outlined.
 */
public class RiskMapView extends JComponent {

    static final int WIDTH = 1000, HEIGHT = 520, RADIUS = 16;
    static final Color SEA = new Color(176, 206, 230);
    static final Color LINK = new Color(90, 110, 130);
    // pale or earthy, so a continent's ring is not mistaken for a player's colour
    static final Color[] CONTINENT_COLOURS = {new Color(245, 160, 60), new Color(245, 160, 190),
            new Color(110, 215, 225), new Color(140, 95, 50), new Color(185, 185, 110), new Color(255, 255, 255)};
    // the outlines of the continents on a shaped map, darker than the rings so they show on land
    static final Color[] CONTINENT_OUTLINES = {new Color(220, 120, 20), new Color(220, 90, 140),
            new Color(30, 150, 170), new Color(120, 75, 35), new Color(130, 130, 40), new Color(110, 60, 160)};

    final RiskMap map;
    final Point[] positions;
    // the shapes of the territories, or null on a map without a drawing
    final RiskBoardShapes shapes;
    final int width, height;
    // drawing to component coordinates; and each territory's shape, label point and continent outline in the latter
    final AffineTransform toView;
    final Map<RiskTerritory, Shape> viewShapes = new HashMap<>();
    final Map<RiskTerritory, Point2D> viewLabels = new HashMap<>();
    // the continents' outlines, drawn at outlinesScale (see continentOutlines)
    BufferedImage outlines;
    double outlinesScale;
    // the connections across the sea: neighbours whose shapes do not touch
    final List<RiskTerritory[]> seaLinks = new ArrayList<>();
    RiskGameState state;
    Function<RiskTerritory, String> toolTips;
    // the territory a human player has picked to act from, and the territories it may act on
    RiskTerritory selected;
    Set<RiskTerritory> targets = Set.of();

    public RiskMapView(RiskMap map) {
        this.map = map;
        List<RiskTerritory> territories = map.territories();
        positions = new Point[territories.size()];
        for (RiskTerritory t : territories) {
            BoardNode node = map.getBoard().getNodeByStringProperty(nameHash, t.name());
            if (node.getProperty("position") instanceof PropertyVector2D p) {
                positions[t.index()] = new Point(p.values.getX(), p.values.getY());
            } else {
                double angle = 2 * Math.PI * t.index() / territories.size();
                positions[t.index()] = new Point((int) (WIDTH / 2 + 0.42 * WIDTH * Math.cos(angle)),
                        (int) (HEIGHT / 2 + 0.40 * HEIGHT * Math.sin(angle)));
            }
        }
        shapes = map.svgFile() == null ? null : new RiskBoardShapes(map);
        if (shapes == null) {
            width = WIDTH;
            height = HEIGHT;
            toView = new AffineTransform();
        } else {
            Rectangle2D box = shapes.viewBox;
            double s = WIDTH / box.getWidth();
            width = WIDTH;
            height = (int) Math.ceil(box.getHeight() * s);
            toView = AffineTransform.getScaleInstance(s, s);
            toView.translate(-box.getX(), -box.getY());
            for (RiskTerritory t : territories) {
                Shape shape = toView.createTransformedShape(shapes.shape(t));
                viewShapes.put(t, shape);
                viewLabels.put(t, toView.transform(shapes.label(t), null));
            }
            // shapes within a few pixels of each other share a border
            for (RiskTerritory a : territories)
                for (RiskTerritory b : map.neighbours(a))
                    if (b.index() > a.index() && !touch(viewShapes.get(a), viewShapes.get(b)))
                        seaLinks.add(new RiskTerritory[]{a, b});
        }
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Whether a point on a's edge lies within a few pixels of b. (Not with Areas: made of these many curves, they go
     * wrong.)
     */
    private static boolean touch(Shape a, Shape b) {
        if (!a.getBounds2D().intersects(grow(b.getBounds2D(), 6))) return false;
        double[] c = new double[6];
        for (PathIterator it = a.getPathIterator(null, 0.5); !it.isDone(); it.next())
            if (it.currentSegment(c) != PathIterator.SEG_CLOSE && b.intersects(c[0] - 4, c[1] - 4, 8, 8))
                return true;
        return false;
    }

    /**
     * The continents' outlines, at the scale they are drawn at (made again when it changes): each continent's
     * territories stroked thickly in its colour, less the territories themselves, grown a little to cover the
     * hairline gaps the drawing leaves between neighbours, which would otherwise be outlined inside the continent.
     */
    private BufferedImage continentOutlines(double scale) {
        if (outlines != null && scale == outlinesScale) return outlines;
        int w = (int) Math.ceil(width * scale), h = (int) Math.ceil(height * scale);
        outlines = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        outlinesScale = scale;
        Graphics2D all = outlines.createGraphics();
        for (RiskContinent c : map.continents()) {
            BufferedImage layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = layer.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(scale, scale);
            List<RiskTerritory> territories = map.territories(c);
            g.setColor(CONTINENT_OUTLINES[c.index() % CONTINENT_OUTLINES.length]);
            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (RiskTerritory t : territories)
                g.draw(viewShapes.get(t));
            g.setComposite(AlphaComposite.Clear);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (RiskTerritory t : territories) {
                g.fill(viewShapes.get(t));
                g.draw(viewShapes.get(t));
            }
            g.dispose();
            all.drawImage(layer, 0, 0, null);
        }
        all.dispose();
        return outlines;
    }

    private static Rectangle2D grow(Rectangle2D r, double d) {
        return new Rectangle2D.Double(r.getX() - d, r.getY() - d, r.getWidth() + 2 * d, r.getHeight() + 2 * d);
    }

    public void update(RiskGameState state) {
        this.state = state;
    }

    /**
     * Whether the territories are drawn in their shapes (the map has a drawing).
     */
    public boolean isShaped() {
        return shapes != null;
    }

    /**
     * The territory's shape (its disc, on a map without a drawing), in the component's coordinates.
     */
    public Shape territoryShape(RiskTerritory t) {
        if (shapes != null) return viewShapes.get(t);
        Point p = positions[t.index()];
        return new Ellipse2D.Double(p.x - RADIUS - 4, p.y - RADIUS - 4, 2 * RADIUS + 8, 2 * RADIUS + 8);
    }

    /**
     * The territory at the point, or null.
     */
    public RiskTerritory territoryAt(Point point) {
        // the last drawn first, as it is the one seen where shapes overlap at their edges
        List<RiskTerritory> territories = map.territories();
        for (int i = territories.size() - 1; i >= 0; i--)
            if (territoryShape(territories.get(i)).contains(point))
                return territories.get(i);
        return null;
    }

    /**
     * Outlines the territory picked to act from (null for none) and, dashed, those it may act on.
     */
    public void setSelection(RiskTerritory selected, Set<RiskTerritory> targets) {
        this.selected = selected;
        this.targets = targets;
        repaint();
    }

    public void setToolTips(Function<RiskTerritory, String> toolTips) {
        this.toolTips = toolTips;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        RiskTerritory t = territoryAt(event.getPoint());
        return t == null || toolTips == null ? null : toolTips.apply(t);
    }

    static Color playerColour(int player) {
        return RiskPlayersView.PLAYER_COLOURS[player % RiskPlayersView.PLAYER_COLOURS.length];
    }

    static Color continentColour(RiskContinent continent) {
        return CONTINENT_COLOURS[continent.index() % CONTINENT_COLOURS.length];
    }

    /**
     * The territories of a pending battle or move.
     */
    private Set<RiskTerritory> outlined() {
        Set<RiskTerritory> outlined = new HashSet<>();
        if (state.currentActionInProgress() instanceof MoveArmiesChoice m) {
            outlined.add(m.from);
            outlined.add(m.to);
        } else if (state.currentActionInProgress() instanceof DefenderDice d) {
            outlined.add(d.from);
            outlined.add(d.to);
        } else if (state.currentActionInProgress() instanceof AttackDiceChoice a) {
            outlined.add(a.from);
            outlined.add(a.to);
        }
        return outlined;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(SEA);
        g.fillRect(0, 0, width, height);
        if (state == null) return;
        if (shapes != null) paintShaped(g);
        else paintDiscs(g);

        // the continents and their bonuses
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        int y = height - 12 - 16 * (map.continents().size() - 1);
        for (RiskContinent c : map.continents()) {
            g.setColor(shapes != null ? CONTINENT_OUTLINES[c.index() % CONTINENT_OUTLINES.length] : continentColour(c));
            g.fillRect(10, y - 10, 12, 12);
            g.setColor(Color.black);
            g.drawString(c.name() + " +" + c.bonus(), 28, y);
            y += 16;
        }

        // the drawing's author and licence, in the bottom right corner on a pale panel
        if (shapes != null && map.svgCredit() != null) {
            g.setFont(CREDIT_FONT);
            FontMetrics fm = g.getFontMetrics();
            String[] lines = map.svgCredit().split("\n");
            int w = Arrays.stream(lines).mapToInt(fm::stringWidth).max().orElse(0);
            int h = lines.length * fm.getHeight();
            g.setColor(new Color(255, 255, 255, 200));
            g.fillRoundRect(width - w - 10, height - h - 6, w + 8, h + 4, 6, 6);
            g.setColor(new Color(60, 60, 60));
            for (int i = 0; i < lines.length; i++)
                g.drawString(lines[i], width - 6 - fm.stringWidth(lines[i]),
                        height - 4 - fm.getDescent() - (lines.length - 1 - i) * fm.getHeight());
        }
    }

    static final Font CREDIT_FONT = new Font("SansSerif", Font.PLAIN, 10);

    private void paintShaped(Graphics2D g) {
        // the sea links first, under the land
        g.setColor(LINK);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{5f, 4f}, 0f));
        for (RiskTerritory[] link : seaLinks)
            drawLink(g, viewLabels.get(link[0]), viewLabels.get(link[1]), width);
        for (RiskTerritory t : map.territories()) {
            int owner = state.getOwner(t);
            g.setColor(owner < 0 ? new Color(245, 240, 225) : RiskPlayersView.blend(playerColour(owner), Color.white, 0.45));
            g.fill(viewShapes.get(t));
        }
        // drawn at the device's scale, so that it stays sharp when the view is drawn larger
        double scale = g.getTransform().getScaleX();
        AffineTransform saved = g.getTransform();
        g.scale(1 / scale, 1 / scale);
        g.drawImage(continentOutlines(scale), 0, 0, null);
        g.setTransform(saved);
        g.setColor(new Color(60, 60, 60));
        g.setStroke(new BasicStroke(0.8f));
        for (RiskTerritory t : map.territories())
            g.draw(viewShapes.get(t));
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(3f));
        for (RiskTerritory t : outlined())
            g.draw(viewShapes.get(t));
        paintSelection(g);

        Font armyFont = new Font("SansSerif", Font.BOLD, 12);
        Font nameFont = new Font("SansSerif", Font.PLAIN, 9);
        int r = 11;
        for (RiskTerritory t : map.territories()) {
            Point2D p = viewLabels.get(t);
            int x = (int) Math.round(p.getX()), y = (int) Math.round(p.getY());
            int owner = state.getOwner(t);
            if (owner >= 0) {
                g.setColor(playerColour(owner));
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                g.setColor(Color.black);
                g.setStroke(new BasicStroke(1f));
                g.drawOval(x - r, y - r, 2 * r, 2 * r);
                g.setFont(armyFont);
                g.setColor(RiskPlayersView.textOn(playerColour(owner)));
                drawCentred(g, String.valueOf(state.getArmies(t)), x, y + 4);
            }
            g.setFont(nameFont);
            g.setColor(Color.black);
            drawCentred(g, t.name(), x, y + r + 10);
        }
    }

    static final Color SELECTED = new Color(250, 200, 0);
    static final Stroke DASHED = new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
            new float[]{6f, 4f}, 0f);

    /**
     * The territory picked to act from, outlined, and those it may act on, dashed.
     */
    private void paintSelection(Graphics2D g) {
        g.setColor(SELECTED);
        g.setStroke(DASHED);
        for (RiskTerritory t : targets)
            if (t != selected)
                g.draw(viewShapes.get(t));
        if (selected != null) {
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(viewShapes.get(selected));
        }
    }

    private void paintDiscs(Graphics2D g) {
        g.setColor(LINK);
        g.setStroke(new BasicStroke(1.5f));
        for (RiskTerritory a : map.territories())
            for (RiskTerritory b : map.neighbours(a))
                if (b.index() > a.index())
                    drawLink(g, positions[a.index()], positions[b.index()], WIDTH);

        Set<RiskTerritory> outlined = outlined();
        Font armyFont = new Font("SansSerif", Font.BOLD, 13);
        Font nameFont = new Font("SansSerif", Font.PLAIN, 10);
        for (RiskTerritory t : map.territories()) {
            Point p = positions[t.index()];
            int owner = state.getOwner(t);
            g.setColor(continentColour(t.continent()));
            g.fillOval(p.x - RADIUS - 4, p.y - RADIUS - 4, 2 * RADIUS + 8, 2 * RADIUS + 8);
            g.setColor(owner < 0 ? Color.white : playerColour(owner));
            g.fillOval(p.x - RADIUS, p.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
            if (outlined.contains(t)) {
                g.setColor(Color.black);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(p.x - RADIUS - 6, p.y - RADIUS - 6, 2 * RADIUS + 12, 2 * RADIUS + 12);
            }
            if (t == selected || targets.contains(t)) {
                Shape ring = new Ellipse2D.Double(p.x - RADIUS - 8, p.y - RADIUS - 8, 2 * RADIUS + 16, 2 * RADIUS + 16);
                g.setColor(SELECTED);
                g.setStroke(t == selected ? new BasicStroke(4f) : DASHED);
                g.draw(ring);
            }
            if (owner >= 0) {
                g.setFont(armyFont);
                g.setColor(RiskPlayersView.textOn(playerColour(owner)));
                drawCentred(g, String.valueOf(state.getArmies(t)), p.x, p.y + 5);
            }
            g.setFont(nameFont);
            g.setColor(Color.black);
            drawCentred(g, t.name(), p.x, p.y + RADIUS + 15);
        }
    }

    /**
     * A line from a to b; one that would cross most of the map (Alaska - Kamchatka) goes round the back of the world
     * instead, from each end out through the nearer side edge.
     */
    private static void drawLink(Graphics2D g, Point2D a, Point2D b, int width) {
        if (Math.abs(a.getX() - b.getX()) <= width / 2.0) {
            g.draw(new Line2D.Double(a, b));
            return;
        }
        Point2D left = a.getX() < b.getX() ? a : b, right = a.getX() < b.getX() ? b : a;
        double midY = (left.getY() + right.getY()) / 2;
        g.draw(new Line2D.Double(left.getX(), left.getY(), 0, midY));
        g.draw(new Line2D.Double(right.getX(), right.getY(), width, midY));
    }

    private static void drawCentred(Graphics2D g, String text, int x, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x - fm.stringWidth(text) / 2, baseline);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
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
