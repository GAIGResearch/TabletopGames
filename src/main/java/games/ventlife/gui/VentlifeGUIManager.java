package games.ventlife.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.VentlifeUtils;
import games.ventlife.actions.*;
import games.ventlife.components.*;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * GUI for Ventlife: the vent field (VentlifeFieldView) beside the players (VentlifePlayersView), with the actions in
 * a scrolling list below, a Rules tab and a How to Play tab.
 * <p>
 * A human player may also choose by clicking: a tile by its Black Smoker's position and then the neighbouring hex for
 * its right terrain; a creature by its hex, and by its species in the key when several fit the hex; a covered
 * creature's destination by its hex; a species to draft in the key. Stopping after Volcano Snails, and the Low-Vent
 * Bonus's second Tube Worm, are chosen with the action buttons.
 */
public class VentlifeGUIManager extends AbstractGUIManager {

    static final Color CLICKABLE = Color.yellow, SELECTED = Color.green;

    VentlifeFieldView fieldView;
    VentlifePlayersView playersView;
    // the state last shown, the hex and species a human player has clicked (null for none), and the hex under the mouse
    VentlifeGameState shown;
    Hex selectedHex, hover;
    Species selectedSpecies;

    public VentlifeGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        VentlifeGameState state = (VentlifeGameState) gameState;
        int nPlayers = state.getNPlayers();
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
        }

        fieldView = new VentlifeFieldView();
        playersView = new VentlifePlayersView(agentNames, this::showHiddenInfo);
        fieldView.update(state);
        playersView.update(state);
        fieldView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fieldClicked(e);
            }
        });
        fieldView.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Hex h = fieldView.hexAt(e.getPoint());
                if (!h.equals(hover)) {
                    hover = h;
                    showClickable();
                }
            }
        });
        fieldView.setToolTips(this::fieldToolTip);
        playersView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                keyClicked(e);
            }
        });
        playersView.setToolTips(this::keyToolTip);

        this.width = VentlifeFieldView.WIDTH + VentlifePlayersView.WIDTH + 20;
        this.height = VentlifeFieldView.HEIGHT + 10;
        // within a 1080-pixel screen: 180 + 610 + 140 + 60
        int actionHeight = 140;

        JTabbedPane tabs = new JTabbedPane();
        JPanel main = new JPanel(new BorderLayout());
        tabs.add("Game", main);
        tabs.add("Rules", new RulesView(rulesHtml(state.getParams()), height + actionHeight + defaultInfoPanelHeight));
        tabs.add("How to Play", new RulesView(howToPlayHtml(), height + actionHeight + defaultInfoPanelHeight));

        JPanel gameArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gameArea.add(fieldView);
        gameArea.add(playersView);

        JPanel infoPanel = createGameStateInfoPanel("Ventlife", gameState, width, defaultInfoPanelHeight);
        // a tile can go in hundreds of places: the panel scrolls
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionHeight);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(gameArea, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + actionHeight + defaultInfoPanelHeight + 50));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // This is called before the game is known, so it cannot read the parameters. The busiest decision is a tile
        // placement: 6 orientations at each position where the Black Smoker may go, which grows with the edge of a
        // field of up to 180 hexes. A creature step offers at most one placement per species in play per hex (more
        // for the Low-Vent Bonus). The most found in 45 random games at 2-4 players, with 5 species, Smokers open
        // to every species but Fish and a Low-Vent Bonus of 2, was 838.
        return 3000;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof VentlifeGameState state)) return;
        shown = state;
        fieldView.update(state);
        playersView.update(state);
        parent.repaint();
    }

    @Override
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions) {
        super.update(player, gameState, showActions);
        // the actions are offered for clicking in updateActionButtons, after _update
        showClickable();
    }

    // ---------------------------------------------------------------- clicks

    private void clearSelection() {
        selectedHex = null;
        selectedSpecies = null;
    }

    private static int direction(Hex from, Hex to) {
        for (int d = 0; d < 6; d++)
            if (from.neighbour(d).equals(to))
                return d;
        return -1;
    }

    /**
     * The tile placements with the Black Smoker at the selected hex and the right terrain at the hex.
     */
    private List<PlaceTile> orientationsTowards(Hex h) {
        if (selectedHex == null) return List.of();
        int d = direction(selectedHex, h);
        return clickable.matching(PlaceTile.class, a -> a.smoker.equals(selectedHex) && a.orientation == d);
    }

    /**
     * The single-creature placements on the hex, of the selected species if one is chosen. The Low-Vent Bonus is
     * left to the action buttons.
     */
    private List<PlaceCreature> creaturesAt(Hex h) {
        return clickable.matching(PlaceCreature.class, a -> a.count == 1 && a.hex.equals(h)
                && (selectedSpecies == null || a.species == selectedSpecies));
    }

    private void fieldClicked(MouseEvent e) {
        if (!clickable.isOffered() || shown == null) return;
        if (e.getButton() != MouseEvent.BUTTON1) {
            clearSelection();
            showClickable();
            return;
        }
        Hex h = fieldView.hexAt(e.getPoint());
        List<Displace> moves = clickable.matching(Displace.class, a -> a.to.equals(h));
        List<PlaceTile> tiles = clickable.matching(PlaceTile.class, a -> a.smoker.equals(h));
        List<PlaceTile> oriented = orientationsTowards(h);
        List<PlaceCreature> creatures = creaturesAt(h);
        if (moves.size() == 1) {
            clickable.submit(moves.get(0));
        } else if (oriented.size() == 1) {
            clickable.submit(oriented.get(0));
        } else if (tiles.size() == 1) {
            clickable.submit(tiles.get(0));
        } else if (!tiles.isEmpty()) {
            selectedHex = h.equals(selectedHex) ? null : h;
        } else if (creatures.size() == 1) {
            clickable.submit(creatures.get(0));
        } else if (!creatures.isEmpty()) {
            selectedHex = h;
        } else {
            clearSelection();
        }
        showClickable();
    }

    private void keyClicked(MouseEvent e) {
        if (!clickable.isOffered() || shown == null) return;
        Species s = playersView.speciesAt(e.getPoint());
        if (e.getButton() != MouseEvent.BUTTON1 || s == null) {
            clearSelection();
            showClickable();
            return;
        }
        List<DraftSpecies> drafts = clickable.matching(DraftSpecies.class, a -> a.species == s);
        List<PlaceCreature> creatures = clickable.matching(PlaceCreature.class, a -> a.count == 1
                && a.species == s && (selectedHex == null || a.hex.equals(selectedHex)));
        if (drafts.size() == 1) {
            clickable.submit(drafts.get(0));
        } else if (creatures.size() == 1) {
            clickable.submit(creatures.get(0));
        } else if (!creatures.isEmpty()) {
            selectedSpecies = s == selectedSpecies ? null : s;
        } else {
            clearSelection();
        }
        showClickable();
    }

    /**
     * Outlines what may be clicked now (yellow) and what has been chosen (green), and shows the tile where it would
     * go when the mouse is over the hex for its right terrain.
     */
    private void showClickable() {
        if (shown == null) return;
        Map<Hex, Color> hexes = new HashMap<>();
        Map<Hex, Terrain> preview = new HashMap<>();
        Map<Species, Color> species = new EnumMap<>(Species.class);
        if (!clickable.isOffered()) {
            clearSelection();
        } else {
            for (Displace a : clickable.matching(Displace.class, a -> true))
                hexes.put(a.to, CLICKABLE);
            if (selectedHex != null && clickable.matching(PlaceTile.class, a -> a.smoker.equals(selectedHex)).isEmpty()
                    && creaturesAt(selectedHex).isEmpty())
                selectedHex = null;
            List<PlaceTile> tiles = clickable.matching(PlaceTile.class, a -> true);
            if (!tiles.isEmpty()) {
                for (PlaceTile a : tiles) {
                    if (selectedHex == null)
                        hexes.put(a.smoker, CLICKABLE);
                    else if (a.smoker.equals(selectedHex))
                        hexes.put(VentlifeUtils.tileHexes(a.smoker, a.orientation).get(2), CLICKABLE);
                }
                if (selectedHex != null) {
                    hexes.put(selectedHex, SELECTED);
                    List<PlaceTile> oriented = hover == null ? List.of() : orientationsTowards(hover);
                    if (!oriented.isEmpty())
                        preview = tileTerrains(oriented.get(0));
                }
            }
            for (PlaceCreature a : clickable.matching(PlaceCreature.class, a -> a.count == 1)) {
                if (selectedSpecies == null || a.species == selectedSpecies)
                    hexes.putIfAbsent(a.hex, CLICKABLE);
                if (selectedHex == null || a.hex.equals(selectedHex))
                    species.put(a.species, CLICKABLE);
            }
            if (selectedHex != null && tiles.isEmpty())
                hexes.put(selectedHex, SELECTED);
            if (selectedSpecies != null)
                species.put(selectedSpecies, SELECTED);
            for (DraftSpecies a : clickable.matching(DraftSpecies.class, a -> true))
                species.put(a.species, CLICKABLE);
        }
        fieldView.setOutlines(hexes, preview);
        playersView.setOutlines(species);
    }

    private static Map<Hex, Terrain> tileTerrains(PlaceTile a) {
        List<Hex> hexes = VentlifeUtils.tileHexes(a.smoker, a.orientation);
        Map<Hex, Terrain> retValue = new HashMap<>();
        retValue.put(hexes.get(0), Terrain.BLACK_SMOKER);
        retValue.put(hexes.get(1), a.tile.left);
        retValue.put(hexes.get(2), a.tile.right);
        return retValue;
    }

    // ---------------------------------------------------------------- tooltips

    private String describe(Hex h) {
        HexCell cell = shown.getCell(h);
        String retValue = cell == null ? "Empty seafloor " + h
                : cell.terrain().label + " " + h + ", level " + cell.level() + " (tile " + (cell.tileId() + 1) + ")";
        Creature c = shown.getCreature(h);
        if (c != null)
            retValue += "<br>Player " + c.owner() + "'s " + (c.count() > 1 ? c.count() + " " : "") + c.species();
        return retValue;
    }

    /**
     * The player's total score if the action were taken now (it changes only the creatures on the field).
     */
    private int scoreAfter(AbstractAction action, int player) {
        VentlifeGameState copy = (VentlifeGameState) shown.copy();
        action.copy().execute(copy);
        return (int) copy.getGameScore(player);
    }

    /**
     * The creatures a tile placement would cover.
     */
    private String covers(PlaceTile a) {
        StringBuilder retValue = new StringBuilder();
        for (Hex h : VentlifeUtils.tileHexes(a.smoker, a.orientation)) {
            Creature c = shown.getCreature(h);
            if (c != null)
                retValue.append(retValue.isEmpty() ? "" : ", ").append("player ").append(c.owner()).append("'s ")
                        .append(c.species()).append(" at ").append(h);
        }
        return retValue.isEmpty() ? "" : "<br>It will cover " + retValue + ".";
    }

    private String fieldToolTip(MouseEvent e) {
        Hex h = fieldView.hexAt(e.getPoint());
        StringBuilder tip = new StringBuilder("<html>").append(describe(h));
        if (clickable.isOffered()) {
            int player = clickable.player();
            List<Displace> moves = clickable.matching(Displace.class, a -> a.to.equals(h));
            List<PlaceTile> oriented = orientationsTowards(h);
            List<PlaceTile> tiles = clickable.matching(PlaceTile.class, a -> a.smoker.equals(h));
            List<PlaceCreature> creatures = creaturesAt(h);
            if (!moves.isEmpty()) {
                Displace m = moves.get(0);
                tip.append("<br><b>Click</b> to move your ").append(m.creature.species()).append(" covered at ")
                        .append(m.from).append(" here.");
            } else if (!oriented.isEmpty()) {
                PlaceTile a = oriented.get(0);
                List<Hex> at = VentlifeUtils.tileHexes(a.smoker, a.orientation);
                tip.append("<br><b>Click</b> to place ").append(a.tile).append(" with its Black Smoker at ")
                        .append(a.smoker).append(", ").append(a.tile.right).append(" here and ").append(a.tile.left)
                        .append(" at ").append(at.get(1)).append(".<br>The tile will be at level ")
                        .append(shown.getLevel(a.smoker) + 1).append(".").append(covers(a));
            } else if (!tiles.isEmpty()) {
                tip.append("<br><b>Click</b> to put the Black Smoker of your tile (").append(tiles.get(0).tile)
                        .append(") here, at level ").append(shown.getLevel(h) + 1).append(". Then click the hex ")
                        .append("next to it for the right terrain (").append(tiles.size()).append(" possible).");
            } else if (!creatures.isEmpty()) {
                int now = (int) shown.getGameScore(player);
                for (PlaceCreature a : creatures)
                    tip.append("<br>Place ").append(a.species).append(" here: your score ").append(now)
                            .append(" would become ").append(scoreAfter(a, player)).append(".");
                tip.append(creatures.size() == 1 ? "<br><b>Click</b> to place it."
                        : "<br><b>Click</b> here and then the species in the key, or the other way round.");
                if (!clickable.matching(PlaceCreature.class, a -> a.count > 1 && a.hex.equals(h)).isEmpty())
                    tip.append("<br>The Low-Vent Bonus (two Tube Worms) is on the action buttons.");
            } else if (selectedHex != null && !clickable.matching(PlaceTile.class, a -> true).isEmpty()) {
                tip.append("<br>Your tile's right terrain cannot go here with its Black Smoker at ")
                        .append(selectedHex).append(".");
            }
        }
        return tip.append("</html>").toString();
    }

    private String keyToolTip(MouseEvent e) {
        Species s = playersView.speciesAt(e.getPoint());
        if (s == null) return null;
        StringBuilder tip = new StringBuilder("<html>").append(s.label);
        if (!shown.getSpeciesInPlay().contains(s) && shown.getGamePhase() != VentlifeGameState.Phase.DRAFT)
            tip.append(" are not in play.");
        if (clickable.isOffered()) {
            int player = clickable.player();
            if (!clickable.matching(DraftSpecies.class, a -> a.species == s).isEmpty())
                tip.append("<br><b>Click</b> to draft ").append(s.label).append(shown.getNPlayers() == 3
                        ? ": they will not be used." : ": they will be used.");
            List<PlaceCreature> creatures = clickable.matching(PlaceCreature.class, a -> a.count == 1
                    && a.species == s && (selectedHex == null || a.hex.equals(selectedHex)));
            if (!creatures.isEmpty()) {
                tip.append("<br>You have ").append(shown.getSupply(player, s)).append(" left.");
                if (creatures.size() == 1)
                    tip.append("<br><b>Click</b> to place one at ").append(creatures.get(0).hex)
                            .append(": your score would become ").append(scoreAfter(creatures.get(0), player))
                            .append(".");
                else
                    tip.append("<br><b>Click</b> to choose them, then click one of the ").append(creatures.size())
                            .append(" hexes outlined.");
            }
        }
        return tip.append("</html>").toString();
    }

    private static String rulesHtml(VentlifeParameters p) {
        String species = switch (p.speciesSelection) {
            case RANDOM -> p.nSpecies + " of the " + Species.values().length + " species are chosen at random.";
            case FIRST_GAME -> "The species are Tube Worms, Blind Vent Shrimp, Volcano Snails and Yeti Crabs.";
            case DRAFT -> "Before the first tile, the players take turns to draft species, starting with player 0. " +
                    "With 2 or 4 players they draft the " + VentlifeUtils.draftPicks(p.nSpecies, 2) + " species " +
                    "that will be used; with 3 players they draft the " + VentlifeUtils.draftPicks(p.nSpecies, 3) +
                    " species that will not be used.";
        };
        String smokers = p.smokersOnlyForWormsAndShrimp
                ? "Only Tube Worms and Shrimp may be placed on, or moved onto, a Black Smoker."
                : "Fish may not be placed on, or moved onto, a Black Smoker. Every other species may.";
        return "<h2>Ventlife</h2>" +
                "<p><a href='#turn'>Turn</a> | <a href='#tiles'>Tiles</a> | <a href='#covering'>Covering</a> | " +
                "<a href='#species'>Species</a> | <a href='#end'>End</a> | <a href='#screen'>Screen</a></p>" +
                "<p>Each player holds one hidden tile. " + species + " Each player has " + p.tokensPerSpecies +
                " tokens of each species in play.</p>" +
                "<h3><a name='turn'>Turn</a></h3><ol>" +
                "<li><b>Place your tile</b> on the seafloor or on top of the field.</li>" +
                "<li><b>Resolve covered creatures</b> (see Covering).</li>" +
                "<li><b>Place creatures of one species</b> from your supply. This is compulsory whenever any " +
                "placement is possible.</li>" +
                "<li><b>Draw a tile</b>, if any are left.</li></ol>" +
                "<h3><a name='tiles'>Tiles</a></h3>" +
                "<p>Each tile has a Black Smoker and two seafloor terrains. The first tile goes with its Black Smoker " +
                "at (0,0).</p><ul>" +
                "<li><b>Seafloor.</b> All three hexes go on empty seafloor, and at least one touches the field " +
                "along a side. The hexes are at level 1.</li>" +
                "<li><b>Plateau.</b> The tile's Black Smoker goes on a Black Smoker, all three hexes lie on hexes of " +
                "the same level, and they rest on at least two different tiles. The hexes are one level higher.</li>" +
                "</ul>" +
                "<h3><a name='covering'>Covering</a></h3>" +
                "<p>Creatures under the new tile move or return to their owner's supply. Your own creatures are " +
                "resolved first, then the other players' clockwise. A creature moves to an empty neighbouring hex " +
                "its species allows. With no such hex it returns; with one it moves there; with several, its owner " +
                "chooses. " + smokers + "</p>" +
                "<h3><a name='species'>Species</a></h3>" +
                "<table border=1 cellpadding=3 cellspacing=0>" +
                "<tr><th>Species</th><th>Placed on</th><th>When covered</th><th>Scores</th></tr>" +
                "<tr><td>Tube Worms (W)</td><td>An empty Black Smoker. On a level-1 Black Smoker you may place " +
                (1 + p.lowVentBonus) + ".</td><td>They climb onto a Black Smoker placed on theirs; otherwise " +
                "they return.</td><td>The level of their hex, each.</td></tr>" +
                "<tr><td>Volcano Snails (Sn)</td><td>Basalt Ridge. You may place up to " + p.maxSnailsPerTurn +
                ", each after the first next to a Snail placed this turn.</td><td>To a lower hex.</td><td>" +
                p.snailPointsPerHeightEdge + " for each neighbouring hex of a different level.</td></tr>" +
                "<tr><td>Blind Vent Shrimp (Sh)</td><td>Any hex but Basalt Ridge.</td><td>To any hex but Basalt " +
                "Ridge.</td><td>" + p.shrimpPointsOneTerrain + " next to Diffuse Vents or Microbial Mat, " +
                p.shrimpPointsBothTerrains + " next to both.</td></tr>" +
                "<tr><td>Eelpout Fish (F)</td><td>A level-1 hex, or a hex next to one of your Fish.</td><td>To a " +
                "hex of the same or a lower level.</td><td>Each shoal of 2 or more connected Fish scores " +
                p.shoalPoints + " for each Fish after the first.</td></tr>" +
                "<tr><td>Yeti Crabs (C)</td><td>A hex of level 2 or more.</td><td>To a hex of the same level." +
                "</td><td>On each plateau (connected hexes of one level, 2 or more), most Crabs score its size, " +
                "second most half (rounded down). Players tied for most all score the size, and no second place " +
                "is scored.</td></tr>" +
                "<tr><td>Deep-Sea Octopus (O)</td><td>An edge hex (next to empty seafloor).</td><td>Its owner " +
                "chooses a direction; it moves in a straight line to the first empty edge hex.</td><td>" +
                p.octopusPointsPerSpecies + " for each other species next to it.</td></tr>" +
                "<tr><td>Vent Sponges (Sp)</td><td>Any hex.</td><td>They return.</td><td>" +
                p.spongePointsPerTerrain + " for each terrain next to them.</td></tr></table>" +
                "<h3><a name='end'>End</a></h3>" +
                "<p>The game ends when every tile has been placed. The highest score wins. Ties are broken by the " +
                "best single species score, then by the Tube Worm score.</p>" +
                "<h3><a name='screen'>Screen</a></h3><ul>" +
                "<li>Each hex shows its terrain colour and its level. Black Smokers are near-black, Basalt Ridge " +
                "grey, Diffuse Vents blue and Microbial Mat white. Thick lines separate tiles, and the last tile " +
                "placed is outlined in orange.</li>" +
                "<li>Creatures are discs in their owner's colour, marked with their species' letters (Wx2 is two " +
                "Tube Worms).</li>" +
                "<li>A creature drawn faded in a dashed ring is covered and waiting for its owner's choice.</li>" +
                "<li>Positions are written (q,r), as the action buttons name them. Empty seafloor next to the " +
                "field is outlined.</li>" +
                "<li>A tile is named left terrain / right terrain. Orientation n puts the right terrain in " +
                "direction n from the Black Smoker and the left terrain in the next direction clockwise. The " +
                "directions are 0 east, 1 south-east, 2 south-west, 3 west, 4 north-west and 5 north-east, so " +
                "orientation 1 is the tile as printed, with the Black Smoker at the top.</li>" +
                "<li>The panel on the right lists the players (the one to act is marked &gt;), their score so far, " +
                "the tokens left of each species, and their tile when you may see it.</li></ul>" +
                "<p>The How to Play tab explains how to choose by clicking on the field and the species key.</p>";
    }

    private static String howToPlayHtml() {
        return "<h2>How to Play</h2>" +
                "<p>On your turn you may choose by clicking on the field and on the species key, as well as with " +
                "the action buttons. The buttons offer the same choices. Yellow outlines what you may click now, " +
                "and green what you have chosen so far. Hold the mouse over a hex or a species for a tooltip that " +
                "gives what a click would do.</p>" +
                "<h3>Placing your tile</h3><ol>" +
                "<li>Click the position for the tile's Black Smoker. Every position where it may go is outlined. A " +
                "position on top of the field is a Black Smoker already there.</li>" +
                "<li>Click the neighbouring hex where the tile's right terrain is to go. The hexes outlined are " +
                "those the rules allow. While the mouse is over one of them, the whole tile is drawn faintly where " +
                "it would go, and the tooltip gives its level and the creatures it would cover.</li></ol>" +
                "<p>To choose another position, click it instead, or click the chosen one again.</p>" +
                "<h3>Placing creatures</h3>" +
                "<p>Click an outlined hex. If more than one species may go there, the species that may are " +
                "outlined in the key on the right: click one of them. You may also click a species in the key " +
                "first, and then one of the hexes outlined for it. The tooltip on a hex gives your score after " +
                "each placement there.</p>" +
                "<p>The Low-Vent Bonus (two Tube Worms on one hex) and Place no more creatures (after a Volcano " +
                "Snail) are chosen with the action buttons.</p>" +
                "<h3>Covered creatures</h3>" +
                "<p>When one of your covered creatures may go to more than one hex, it is drawn faded in a dashed " +
                "ring and the hexes it may go to are outlined. Click one of them.</p>" +
                "<h3>The draft</h3>" +
                "<p>Click a species in the key to draft it.</p>" +
                "<h3>Cancelling</h3>" +
                "<p>Click with the right mouse button, or click a hex that is not outlined, to clear your choice.</p>";
    }
}
