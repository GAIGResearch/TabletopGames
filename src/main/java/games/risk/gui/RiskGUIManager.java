package games.risk.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.risk.*;
import games.risk.actions.*;
import gui.*;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GUI for Risk: the map (RiskMapView) beside a panel of the players (RiskPlayersView), with the actions in a
 * scrolling list below, and a Rules tab. The actions are described in words, a battle with its odds (see
 * {@link RiskOdds}), and a territory's tooltip tells what bears on it. A human player may also act on the map: a click
 * on a territory claims it or places armies there; or picks it to attack or fortify from, and then a click on a
 * territory it may act on chooses that (with a menu when there is more than one way); a right click drops the pick.
 * The dice and the armies to move follow on the buttons. Shown in a browser, the page does the same (see
 * {@link #getMapMove}), and a player plans their reinforcements (see {@link RiskPlanner}).
 */
public class RiskGUIManager extends AbstractGUIManager {

    RiskMapView mapView;
    RiskPlayersView playersView;
    RiskPlanner planner;
    String[] names;
    // the state last shown, read by the tooltips
    RiskGameState shown;
    // the territory picked on the map to act from, or null
    RiskTerritory selected;

    public RiskGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        RiskGameState state = (RiskGameState) gameState;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();
        }

        names = agentNames;
        shown = state;
        planner = new RiskPlanner((RiskForwardModel) game.getForwardModel());
        mapView = new RiskMapView(params.getMap());
        mapView.setToolTips(this::toolTip);
        mapView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicked(e);
            }
        });
        playersView = new RiskPlayersView(agentNames, this::visible);
        playersView.update(state);
        mapView.update(state);

        Dimension mapSize = mapView.getPreferredSize();
        Dimension playersSize = playersView.getPreferredSize();
        this.width = mapSize.width + playersSize.width + 20;
        int areaHeight = Math.max(mapSize.height, playersSize.height);
        this.height = areaHeight + 20;
        // within a 1080-pixel screen with six players and their missions: 180 + 618 + 20 + 160 + 50
        int actionHeight = 160;

        JTabbedPane tabs = new JTabbedPane();
        JPanel main = new JPanel(new BorderLayout());
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.Risk, params, height + actionHeight + defaultInfoPanelHeight);

        JPanel gameArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gameArea.add(mapView);
        gameArea.add(playersView);

        JPanel infoPanel = createGameStateInfoPanel("Risk", gameState, width, defaultInfoPanelHeight);
        // a turn can offer hundreds of fortifying moves: the panel scrolls
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
        // This is called before the game is known, so it cannot read the map. The most actions offered at once on the
        // world map is in FORTIFY with fortifyAlongPath, when one player holds all but one of the 42 territories, all
        // connected: a Fortify from each to each of the others, 41 x 40 = 1640, and EndTurn. ATTACK offers a ChooseAttack
        // for each of the 83 connections, and EndAttack; the dice are then a choice of at most 4.
        return 1641;
    }

    /**
     * A player's cards and mission are face up only to a human player who holds them, to the current player if the
     * core parameters allow it, or in full-observability mode.
     */
    private boolean visible(int player) {
        AbstractGameState state = game.getGameState();
        return humanPlayerIds.contains(player)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (player == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof RiskGameState state)) return;
        shown = state;
        mapView.update(state);
        playersView.update(state);
        parent.repaint();
    }

    @Override
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions) {
        super.update(player, gameState, showActions);
        // after the actions are offered (or withdrawn) for this state: a pick no longer offered is dropped
        if (selected != null && fromHere(selected).isEmpty()) select(null);
    }

    /**
     * The offered actions on the map that act from the territory.
     */
    private List<AbstractAction> fromHere(RiskTerritory t) {
        return clickable.matching(a -> getMapMove(a) != null && getMapMove(a).from().equals(t.name()));
    }

    private void select(RiskTerritory t) {
        selected = t;
        Set<RiskTerritory> targets = new HashSet<>();
        if (t != null)
            for (AbstractAction a : fromHere(t))
                targets.add(shown.getMap().territory(getMapMove(a).to()));
        mapView.setSelection(t, targets);
    }

    /**
     * A click on the map: see the class comment.
     */
    private void clicked(MouseEvent e) {
        if (!clickable.isOffered() || shown == null) return;
        if (e.getButton() != MouseEvent.BUTTON1) {
            select(null);
            return;
        }
        RiskTerritory t = mapView.territoryAt(e.getPoint());
        if (t == null) {
            select(null);
            return;
        }
        if (selected != null) {
            List<AbstractAction> here = clickable.matching(a -> getMapMove(a) != null
                    && getMapMove(a).from().equals(selected.name()) && getMapMove(a).to().equals(t.name()));
            if (!here.isEmpty()) {
                choose(here, e);
                return;
            }
        }
        List<AbstractAction> from = fromHere(t);
        if (t == selected || from.isEmpty()) {
            select(null);
            return;
        }
        select(t);
        // claiming or placing: all on the spot
        if (from.stream().allMatch(a -> getMapMove(a).to().equals(t.name())))
            choose(from, e);
    }

    /**
     * Chooses the action, or offers a menu of them where the mouse is.
     */
    private void choose(List<AbstractAction> actions, MouseEvent e) {
        if (actions.size() == 1) {
            submit(actions.get(0));
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        for (AbstractAction a : actions) {
            JMenuItem item = new JMenuItem(actionLabel(a, shown));
            item.addActionListener(x -> submit(a));
            menu.add(item);
        }
        menu.show(mapView, e.getX(), e.getY());
    }

    private void submit(AbstractAction action) {
        select(null);
        if (clickable.submit(action)) resetActionButtons();
    }

    @Override
    public List<MapRegion> getMapRegions() {
        if (mapView == null || !mapView.isShaped()) return List.of();
        List<MapRegion> regions = new ArrayList<>();
        for (RiskTerritory t : shown.getMap().territories())
            regions.add(new MapRegion(t.name(), t.name(), mapView, mapView.territoryShape(t)));
        return regions;
    }

    /**
     * Claiming and placing are on a territory; an attack or a fortifying move goes from one territory to another. The
     * dice and the number of armies to move are follow-on choices, not on the map.
     */
    @Override
    public MapMove getMapMove(AbstractAction action) {
        if (action instanceof ClaimTerritory c) return new MapMove(c.territory.name(), c.territory.name());
        if (action instanceof PlaceArmy p) return new MapMove(p.territory.name(), p.territory.name());
        if (action instanceof ChooseAttack a) return new MapMove(a.from.name(), a.to.name());
        if (action instanceof Fortify f) return new MapMove(f.from.name(), f.to.name());
        return null;
    }

    @Override
    public IMovePlanner getPlanner() {
        return planner;
    }

    /**
     * The actions in words, with the odds of a battle: for an attack, the chance of taking the territory by
     * attacking until it falls; for a roll, the chance of each number of armies lost.
     */
    @Override
    public String actionLabel(AbstractAction action, AbstractGameState gameState) {
        RiskGameState s = (RiskGameState) gameState;
        RiskParameters params = (RiskParameters) s.getGameParameters();
        if (action instanceof PlaceArmy p)
            return "Place " + armies(p.n) + " in " + p.territory;
        if (action instanceof ClaimTerritory c)
            return "Claim " + c.territory;
        if (action instanceof ChooseAttack a)
            return "Attack " + a.to + " (" + s.getArmies(a.to) + ") from " + a.from + " (" + s.getArmies(a.from)
                    + "): taken " + RiskOdds.percent(RiskOdds.capture(s.getArmies(a.from), s.getArmies(a.to),
                    params.maxAttackDice, params.maxDefendDice)) + " of the time by attacking until it falls";
        if (action instanceof Attack a) {
            int defend = Math.min(params.maxDefendDice, s.getArmies(a.to));
            double[] p = RiskOdds.roll(a.nDice, defend);
            StringBuilder text = new StringBuilder("Roll " + a.nDice + (a.nDice == 1 ? " die" : " dice")
                    + (params.defenderChoosesDice ? " (against up to " : " (against ") + defend + "): you lose ");
            for (int k = 0; k < p.length; k++)
                text.append(k == 0 ? "" : ", ").append(k).append(": ").append(RiskOdds.percent(p[k]));
            return text.toString();
        }
        if (action instanceof Blitz b)
            return "Blitz: attack until " + b.to + " falls or 1 army is left, taken "
                    + RiskOdds.percent(RiskOdds.capture(s.getArmies(b.from), s.getArmies(b.to), params.maxAttackDice,
                    params.maxDefendDice));
        if (action instanceof MoveArmies m)
            return "Move " + armies(m.n) + " from " + m.from + " to " + m.to;
        if (action instanceof Fortify f)
            return "Fortify " + f.to + " from " + f.from + " (" + s.getArmies(f.from) + ")";
        if (action instanceof DefendWith d)
            return "Defend " + d.to + " with " + d.nDefendDice + (d.nDefendDice == 1 ? " die" : " dice");
        if (action instanceof EndAttack)
            return "End attacks";
        if (action instanceof EndTurn)
            return "End turn";
        return super.actionLabel(action, gameState);
    }

    private static String armies(int n) {
        return n == 1 ? "1 army" : n + " armies";
    }

    private String player(int p) {
        return p < 0 ? "nobody" : humanPlayerIds.contains(p) ? "you" : "Player " + p + " (" + names[p] + ")";
    }

    /**
     * The territory under the mouse: its continent (with its bonus, and how much of it the territory's owner holds),
     * its owner and armies, and its neighbours with theirs.
     */
    private String toolTip(RiskTerritory t) {
        RiskGameState s = shown;
        if (s == null) return null;
        RiskContinent c = t.continent();
        int owner = s.getOwner(t);
        StringBuilder text = new StringBuilder("<html><b>").append(t.name()).append("</b> (").append(c.name())
                .append(", +").append(c.bonus()).append(" a turn to whoever holds it all)");
        text.append("<br>").append(owner < 0 ? "Unclaimed" : "Held by " + player(owner) + " with "
                + armies(s.getArmies(t)));
        if (owner >= 0) {
            List<RiskTerritory> continent = s.getMap().territories(c);
            long held = continent.stream().filter(x -> s.getOwner(x) == owner).count();
            String who = capitalised(player(owner)) + (humanPlayerIds.contains(owner) ? " hold " : " holds ");
            text.append("<br>").append(held == continent.size() ? who + "all of " + c.name()
                    : who + held + " of its " + continent.size() + " territories");
        }
        text.append("<br>Next to:");
        for (RiskTerritory n : s.getMap().neighbours(t)) {
            int o = s.getOwner(n);
            text.append("<br>&nbsp;&nbsp;").append(n.name()).append(" - ")
                    .append(o < 0 ? "unclaimed" : player(o) + ", " + s.getArmies(n));
        }
        return text.append("</html>").toString();
    }

    private static String capitalised(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
