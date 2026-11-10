package games.ventlife.gui;

import games.ventlife.VentlifeGameState;
import games.ventlife.components.Species;
import games.ventlife.components.Terrain;
import games.ventlife.components.VentTile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * The players beside the field, each with their score so far, the tokens left in their supply for each species in
 * play, and the tile they hold (face down unless the viewer may see it). Below them are the draw deck, the drafted
 * species during the draft, and the key to the species' abbreviations. For a human player's clicks, species in the key may
 * be outlined.
 */
public class VentlifePlayersView extends JComponent {

    static final int WIDTH = 310, HEIGHT = VentlifeFieldView.HEIGHT;
    static final Color BACKGROUND = new Color(225, 232, 238);
    static final Color[] PLAYER_COLOURS = {new Color(205, 45, 45), new Color(40, 90, 215), new Color(30, 140, 60),
            new Color(140, 60, 180)};

    final String[] agentNames;
    final BiPredicate<VentlifeGameState, Integer> tileVisible;
    VentlifeGameState state;
    // where each species' line of the key was drawn
    final Map<Species, Rectangle> keyRows = new EnumMap<>(Species.class);
    Map<Species, Color> outlines = new HashMap<>();
    Function<MouseEvent, String> toolTips;

    /**
     * @param tileVisible whether, in the state drawn, the tile a player holds may be shown face up
     */
    public VentlifePlayersView(String[] agentNames, BiPredicate<VentlifeGameState, Integer> tileVisible) {
        this.agentNames = agentNames;
        this.tileVisible = tileVisible;
    }

    public void update(VentlifeGameState state) {
        this.state = state;
    }

    public void setOutlines(Map<Species, Color> outlines) {
        this.outlines = outlines;
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

    /**
     * The species whose line of the key is under the point, or null.
     */
    public Species speciesAt(Point p) {
        for (Map.Entry<Species, Rectangle> e : keyRows.entrySet())
            if (e.getValue().contains(p))
                return e.getKey();
        return null;
    }

    static Color playerColour(int player) {
        return PLAYER_COLOURS[player % PLAYER_COLOURS.length];
    }

    static String abbreviation(Species s) {
        return switch (s) {
            case TUBE_WORM -> "W";
            case VOLCANO_SNAIL -> "Sn";
            case VENT_SHRIMP -> "Sh";
            case EELPOUT_FISH -> "F";
            case YETI_CRAB -> "C";
            case OCTOPUS -> "O";
            case VENT_SPONGE -> "Sp";
        };
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        if (state == null) return;

        Font bold = new Font("SansSerif", Font.BOLD, 13), plain = new Font("SansSerif", Font.PLAIN, 12);
        int y = 8;
        for (int p = 0; p < state.getNPlayers(); p++) {
            boolean toMove = state.isNotTerminal() && p == state.getCurrentPlayer();
            g.setColor(playerColour(p));
            g.fillRect(8, y, 6, 92);
            g.setColor(Color.black);
            g.setFont(bold);
            g.drawString((toMove ? "> " : "") + "Player " + p + " (" + agentNames[p] + ")", 20, y + 14);
            g.setFont(plain);
            g.drawString("Score " + (int) state.getGameScore(p), 20, y + 32);
            int x = 20;
            for (Species s : state.getSpeciesInPlay()) {
                g.drawString(abbreviation(s) + " " + state.getSupply(p, s), x, y + 50);
                x += 50;
            }
            if (state.getSpeciesInPlay().isEmpty())
                g.drawString("No species yet", x, y + 50);
            g.drawString("Tile:", 20, y + 76);
            if (state.getHand(p).getSize() == 0)
                g.drawString("none", 56, y + 76);
            else if (tileVisible.test(state, p))
                drawTile(g, state.getHand(p).peek(), 60, y + 60);
            else
                drawTile(g, null, 60, y + 60);
            y += 104;
        }

        g.setColor(Color.black);
        g.setFont(plain);
        g.drawString("Draw deck: " + state.getDrawDeck().getSize() + " tiles", 12, y + 12);
        y += 30;
        if (state.getGamePhase() == VentlifeGameState.Phase.DRAFT || !state.getDrafted().isEmpty()) {
            StringBuilder drafted = new StringBuilder();
            for (Species s : state.getDrafted())
                drafted.append(drafted.isEmpty() ? "" : ", ").append(abbreviation(s));
            g.drawString((state.getNPlayers() == 3 ? "Drafted out: " : "Drafted: ")
                    + (drafted.isEmpty() ? "none yet" : drafted), 12, y);
            y += 22;
        }
        g.setFont(bold);
        g.drawString("Species", 12, y);
        g.setFont(plain);
        boolean drafting = state.getGamePhase() == VentlifeGameState.Phase.DRAFT;
        for (Species s : Species.values()) {
            y += 17;
            boolean inPlay = state.getSpeciesInPlay().contains(s);
            String note = drafting ? (state.getDrafted().contains(s) ? " (drafted)" : "")
                    : inPlay ? "" : " (not in play)";
            g.setColor(inPlay || drafting ? Color.black : Color.gray);
            g.drawString(abbreviation(s) + "  " + s.label + note, 20, y);
            Rectangle row = new Rectangle(14, y - 13, WIDTH - 28, 17);
            keyRows.put(s, row);
            Color outline = outlines.get(s);
            if (outline != null) {
                g.setColor(outline);
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(row.x, row.y, row.width, row.height, 6, 6);
                g.setStroke(new BasicStroke(1f));
            }
        }
    }

    /**
     * A tile as the rulebook prints it: the Black Smoker on top, the left terrain below left and the right below
     * right; face down (null) as three grey hexes.
     */
    private void drawTile(Graphics2D g, VentTile tile, int x, int y) {
        double s = 9;
        double w = s * VentlifeFieldView.SQRT3;
        double[][] centres = {{x + w / 2, y + s}, {x, y + 2.5 * s}, {x + w, y + 2.5 * s}};
        Terrain[] terrains = tile == null ? null : new Terrain[]{Terrain.BLACK_SMOKER, tile.left, tile.right};
        for (int i = 0; i < 3; i++) {
            Path2D hex = new Path2D.Double();
            for (int k = 0; k < 6; k++) {
                double angle = Math.toRadians(60 * k - 30);
                double px = centres[i][0] + s * Math.cos(angle), py = centres[i][1] + s * Math.sin(angle);
                if (k == 0) hex.moveTo(px, py);
                else hex.lineTo(px, py);
            }
            hex.closePath();
            g.setColor(terrains == null ? Color.gray : VentlifeFieldView.terrainColour(terrains[i]));
            g.fill(hex);
            g.setColor(Color.black);
            g.draw(hex);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(tile == null ? "hidden" : tile.left.label + " / " + tile.right.label, (int) (x + 2 * w + 4),
                (int) (y + 2.2 * s));
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
