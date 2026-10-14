package games.ventlife.gui;

import core.components.grid.HexCoord;
import games.ventlife.VentlifeGameState;
import games.ventlife.actions.ResolveCovering;
import games.ventlife.components.*;
import gui.HexLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The vent field, scaled to fit: the top hex of each covered position in its terrain's colour with its level, thick
 * lines between hexes of different tiles, the last tile placed outlined in orange, and the creatures as discs in their
 * owner's colour. The empty seafloor next to the field is drawn faintly, with the positions' coordinates as the action
 * buttons name them. A covered creature waiting for its owner's choice is drawn faded at the position it was covered
 * at, ringed in black. For a human player's clicks, hexes may be outlined, and a tile drawn faintly where it would
 * go.
 */
public class VentlifeFieldView extends JComponent {

    static final int WIDTH = 820, HEIGHT = 600, MARGIN = 12;
    static final double MAX_SIZE = 40;
    static final Color SEA = new Color(18, 40, 66);
    static final Color SEAFLOOR = new Color(60, 85, 110);
    static final Color LAST_TILE = new Color(255, 120, 0);

    VentlifeGameState state;
    // set when painting
    HexLayout layout = new HexLayout(MAX_SIZE, WIDTH / 2.0, HEIGHT / 2.0);
    Map<HexCoord, Color> outlines = new HashMap<>();
    // the terrains of a tile shown where it would be placed
    Map<HexCoord, Terrain> preview = new HashMap<>();
    Function<MouseEvent, String> toolTips;

    public void update(VentlifeGameState state) {
        this.state = state;
    }

    public void setOutlines(Map<HexCoord, Color> outlines, Map<HexCoord, Terrain> preview) {
        this.outlines = outlines;
        this.preview = preview;
        repaint();
    }

    public void setToolTips(Function<MouseEvent, String> toolTips) {
        this.toolTips = toolTips;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        return toolTips == null || state == null ? null : toolTips.apply(e);
    }

    static Color terrainColour(Terrain t) {
        return switch (t) {
            case BLACK_SMOKER -> new Color(45, 40, 50);
            case BASALT_RIDGE -> new Color(120, 118, 115);
            case DIFFUSE_VENTS -> new Color(110, 175, 230);
            case MICROBIAL_MAT -> new Color(232, 232, 220);
        };
    }

    /**
     * Light text on the dark terrains, dark on the light ones.
     */
    static Color textOn(Terrain t) {
        return t == Terrain.BLACK_SMOKER || t == Terrain.BASALT_RIDGE ? Color.white : Color.black;
    }

    /**
     * The pixel at the centre of the position, as last painted.
     */
    public Point centre(HexCoord h) {
        return layout.pixel(h);
    }

    /**
     * The position under the pixel, covered or not.
     */
    HexCoord hexAt(Point p) {
        return layout.hexAt(p.x, p.y);
    }

    /**
     * The empty seafloor next to the field.
     */
    private Set<HexCoord> seafloor() {
        Set<HexCoord> retValue = new HashSet<>();
        for (HexCoord h : state.getField().keySet())
            for (HexCoord n : h.neighbours())
                if (state.getCell(n) == null)
                    retValue.add(n);
        return retValue;
    }

    /**
     * Fits the field and the seafloor round it into the view.
     */
    private void layOut(Set<HexCoord> seafloor) {
        Set<HexCoord> all = new HashSet<>(state.getField().keySet());
        all.addAll(seafloor);
        if (all.isEmpty())
            all.add(HexCoord.ORIGIN);
        layout = HexLayout.fitting(all, WIDTH, HEIGHT, MARGIN, MAX_SIZE);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(SEA);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        if (state == null) return;

        Set<HexCoord> seafloor = seafloor();
        layOut(seafloor);
        boolean labels = layout.size() >= 16;
        Font coordFont = new Font("SansSerif", Font.PLAIN, (int) Math.max(8, layout.size() * 0.28));

        g.setStroke(new BasicStroke(1f));
        for (HexCoord h : seafloor) {
            Point c = centre(h);
            g.setColor(SEAFLOOR);
            g.draw(layout.outline(h, layout.size() - 1));
            if (labels) {
                g.setFont(coordFont);
                drawCentred(g, h.toString(), c.x, c.y + coordFont.getSize() / 2);
            }
        }
        if (state.getField().isEmpty()) {
            g.setColor(Color.white);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawCentred(g, "The first tile goes with its Black Smoker at (0,0)", WIDTH / 2, HEIGHT - 30);
        }

        Font levelFont = new Font("SansSerif", Font.BOLD, (int) Math.max(8, layout.size() * 0.32));
        for (Map.Entry<HexCoord, HexCell> e : state.getField().entrySet()) {
            Point c = centre(e.getKey());
            HexCell cell = e.getValue();
            g.setColor(terrainColour(cell.terrain()));
            g.fill(layout.outline(e.getKey(), layout.size()));
            g.setColor(textOn(cell.terrain()));
            g.setFont(levelFont);
            drawCentred(g, String.valueOf(cell.level()), c.x, (int) (c.y - layout.size() * 0.45));
            if (labels && state.getCreature(e.getKey()) == null) {
                g.setFont(coordFont);
                drawCentred(g, e.getKey().toString(), c.x, (int) (c.y + layout.size() * 0.6));
            }
        }

        // the sides between tiles, and round the field; the last tile placed in orange
        int lastTile = state.getTilesPlaced() - 1;
        for (int pass = 0; pass < 2; pass++)
            for (Map.Entry<HexCoord, HexCell> e : state.getField().entrySet()) {
                boolean last = e.getValue().tileId() == lastTile;
                if (last != (pass == 1))
                    continue;
                g.setColor(last ? LAST_TILE : Color.black);
                g.setStroke(new BasicStroke(last ? 3f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int d = 0; d < 6; d++) {
                    HexCell other = state.getCell(e.getKey().neighbour(d));
                    if (other != null && other.tileId() == e.getValue().tileId())
                        continue;
                    g.draw(new Line2D.Double(layout.corner(e.getKey(), d), layout.corner(e.getKey(), d + 1)));
                }
            }

        for (Map.Entry<HexCoord, Creature> e : state.getCreatures().entrySet())
            drawCreature(g, centre(e.getKey()), e.getValue(), false);

        Composite opaque = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        for (Map.Entry<HexCoord, Terrain> e : preview.entrySet()) {
            g.setColor(terrainColour(e.getValue()));
            g.fill(layout.outline(e.getKey(), layout.size() - 3));
        }
        g.setComposite(opaque);
        for (Map.Entry<HexCoord, Color> e : outlines.entrySet()) {
            g.setColor(e.getValue());
            g.setStroke(new BasicStroke(3f));
            g.draw(layout.outline(e.getKey(), layout.size() - 3));
        }

        if (state.isActionInProgress() && state.currentActionInProgress() instanceof ResolveCovering rc) {
            ResolveCovering.Covered waiting = rc.waiting();
            Point c = centre(waiting.from());
            drawCreature(g, c, waiting.creature(), true);
            g.setColor(Color.black);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f));
            int ring = (int) (layout.size() * 0.75);
            g.drawOval(c.x - ring, c.y - ring, 2 * ring, 2 * ring);
        }
    }

    private void drawCreature(Graphics2D g, Point c, Creature creature, boolean faded) {
        int radius = (int) Math.max(5, layout.size() * 0.5);
        Color colour = VentlifePlayersView.playerColour(creature.owner());
        Composite old = g.getComposite();
        if (faded)
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g.setColor(colour);
        g.fillOval(c.x - radius, c.y - radius + 2, 2 * radius, 2 * radius);
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(c.x - radius, c.y - radius + 2, 2 * radius, 2 * radius);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) Math.max(8, layout.size() * 0.42)));
        drawCentred(g, VentlifePlayersView.abbreviation(creature.species())
                + (creature.count() > 1 ? "x" + creature.count() : ""), c.x, c.y + 2 + g.getFont().getSize() / 3);
        g.setComposite(old);
    }

    static void drawCentred(Graphics2D g, String text, int x, int baseline) {
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
