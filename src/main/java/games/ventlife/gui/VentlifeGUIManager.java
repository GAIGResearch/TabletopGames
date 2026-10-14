package games.ventlife.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.grid.HexCoord;
import games.GameType;
import games.ventlife.VentlifeGameState;
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
    HexCoord selectedHex, hover;
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
        // the views show this state until the first update, and their tooltips read it
        shown = state;
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
                HexCoord h = fieldView.hexAt(e.getPoint());
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
        RulesView.addTabs(tabs, GameType.Ventlife, state.getParams(), height + actionHeight + defaultInfoPanelHeight);

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

    private static int direction(HexCoord from, HexCoord to) {
        for (int d = 0; d < 6; d++)
            if (from.neighbour(d).equals(to))
                return d;
        return -1;
    }

    /**
     * The tile placements with the Black Smoker at the selected hex and the right terrain at the hex.
     */
    private List<PlaceTile> orientationsTowards(HexCoord h) {
        if (selectedHex == null) return List.of();
        int d = direction(selectedHex, h);
        return clickable.matching(PlaceTile.class, a -> a.smoker.equals(selectedHex) && a.orientation == d);
    }

    /**
     * The single-creature placements on the hex, of the selected species if one is chosen. The Low-Vent Bonus is
     * left to the action buttons.
     */
    private List<PlaceCreature> creaturesAt(HexCoord h) {
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
        HexCoord h = fieldView.hexAt(e.getPoint());
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
        Map<HexCoord, Color> hexes = new HashMap<>();
        Map<HexCoord, Terrain> preview = new HashMap<>();
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

    private static Map<HexCoord, Terrain> tileTerrains(PlaceTile a) {
        List<HexCoord> hexes = VentlifeUtils.tileHexes(a.smoker, a.orientation);
        Map<HexCoord, Terrain> retValue = new HashMap<>();
        retValue.put(hexes.get(0), Terrain.BLACK_SMOKER);
        retValue.put(hexes.get(1), a.tile.left);
        retValue.put(hexes.get(2), a.tile.right);
        return retValue;
    }

    // ---------------------------------------------------------------- tooltips

    private String describe(HexCoord h) {
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
        for (HexCoord h : VentlifeUtils.tileHexes(a.smoker, a.orientation)) {
            Creature c = shown.getCreature(h);
            if (c != null)
                retValue.append(retValue.isEmpty() ? "" : ", ").append("player ").append(c.owner()).append("'s ")
                        .append(c.species()).append(" at ").append(h);
        }
        return retValue.isEmpty() ? "" : "<br>It will cover " + retValue + ".";
    }

    private String fieldToolTip(MouseEvent e) {
        HexCoord h = fieldView.hexAt(e.getPoint());
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
                List<HexCoord> at = VentlifeUtils.tileHexes(a.smoker, a.orientation);
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
}
