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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.nameHash;

/**
 * The board: each territory a disc at its "position" in the map file, filled with its owner's colour and showing its
 * armies, ringed in its continent's colour. Lines join neighbours; a line that would cross the whole map (Alaska -
 * Kamchatka) leaves by the nearer edge instead. The two territories of a pending move-in or defence are outlined.
 * A map file without positions has its territories set round an ellipse.
 */
public class RiskMapView extends JComponent {

    static final int WIDTH = 1000, HEIGHT = 520, RADIUS = 16;
    static final Color SEA = new Color(176, 206, 230);
    static final Color LINK = new Color(90, 110, 130);
    // pale or earthy, so a continent's ring is not mistaken for a player's colour
    static final Color[] CONTINENT_COLOURS = {new Color(245, 160, 60), new Color(245, 160, 190),
            new Color(110, 215, 225), new Color(140, 95, 50), new Color(185, 185, 110), new Color(255, 255, 255)};

    final RiskMap map;
    final Point[] positions;
    RiskGameState state;

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
    }

    public void update(RiskGameState state) {
        this.state = state;
    }

    static Color playerColour(int player) {
        return RiskPlayersView.PLAYER_COLOURS[player % RiskPlayersView.PLAYER_COLOURS.length];
    }

    static Color continentColour(RiskContinent continent) {
        return CONTINENT_COLOURS[continent.index() % CONTINENT_COLOURS.length];
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(SEA);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        if (state == null) return;

        g.setColor(LINK);
        g.setStroke(new BasicStroke(1.5f));
        for (RiskTerritory a : map.territories())
            for (RiskTerritory b : map.neighbours(a))
                if (b.index() > a.index())
                    drawLink(g, positions[a.index()], positions[b.index()]);

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
            if (owner >= 0) {
                g.setFont(armyFont);
                g.setColor(RiskPlayersView.textOn(playerColour(owner)));
                drawCentred(g, String.valueOf(state.getArmies(t)), p.x, p.y + 5);
            }
            g.setFont(nameFont);
            g.setColor(Color.black);
            drawCentred(g, t.name(), p.x, p.y + RADIUS + 15);
        }

        // the continents and their bonuses
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        int y = HEIGHT - 12 - 16 * (map.continents().size() - 1);
        for (RiskContinent c : map.continents()) {
            g.setColor(continentColour(c));
            g.fillRect(10, y - 10, 12, 12);
            g.setColor(Color.black);
            g.drawString(c.name() + " +" + c.bonus(), 28, y);
            y += 16;
        }
    }

    private static void drawLink(Graphics2D g, Point a, Point b) {
        if (Math.abs(a.x - b.x) <= WIDTH / 2) {
            g.drawLine(a.x, a.y, b.x, b.y);
            return;
        }
        // round the back of the world: from each end out through the nearer side edge
        Point left = a.x < b.x ? a : b, right = a.x < b.x ? b : a;
        int midY = (left.y + right.y) / 2;
        g.drawLine(left.x, left.y, 0, midY);
        g.drawLine(right.x, right.y, WIDTH, midY);
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
