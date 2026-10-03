package games.risk.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * GUI for Risk: the map (RiskMapView) beside a panel of the players (RiskPlayersView), with the actions in a
 * scrolling list below, and a Rules tab.
 */
public class RiskGUIManager extends AbstractGUIManager {

    RiskMapView mapView;
    RiskPlayersView playersView;

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
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
        }

        mapView = new RiskMapView(params.getMap());
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
        tabs.add("Rules", new RulesView(rulesHtml(params), height + actionHeight + defaultInfoPanelHeight));

        JPanel gameArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gameArea.add(mapView);
        gameArea.add(playersView);

        JPanel infoPanel = createGameStateInfoPanel("Risk", gameState, width, defaultInfoPanelHeight);
        // a vertical, scrolling list: a turn can offer hundreds of attacks or fortifying moves
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionHeight, true);

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
        // connected: a Fortify from each to each of the others, 41 x 40 = 1640, and EndTurn. ATTACK offers at most 4
        // for each of the 83 connections (3 dice counts and a Blitz), 332, and EndAttack.
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
        mapView.update(state);
        playersView.update(state);
        parent.repaint();
    }

    private static String rulesHtml(RiskParameters p) {
        StringBuilder values = new StringBuilder();
        for (int k = 1; k <= 8; k++)
            values.append(k == 1 ? "" : ", ").append(p.tradeValue(k));
        String limit = p.maxArmiesPerTerritory > 0
                ? "<p>No territory may hold more than " + p.maxArmiesPerTerritory + " armies. Armies that " +
                "cannot be placed for this reason are lost.</p>" : "";
        String fortify = p.fortifyAlongPath
                ? "to another territory you hold that is joined to it by a chain of territories you hold"
                : "to a neighbouring territory you hold";
        String setup = p.randomTerritoryDeal || p.secretMission
                ? "<p>The territories are dealt at random, one army on each. "
                : "<p>In turn, each player puts one army on an empty territory until all are claimed. ";
        String winning = p.secretMission
                ? "<p><b>Winning.</b> Each player has a secret mission. The first to complete it wins at once. " +
                "Taking every territory also wins. If another player eliminates the player your mission tells you " +
                "to destroy, you take over the eliminated player's mission. A mission to destroy yourself, or a " +
                "player who is not in the game or is already out, is to occupy " + p.getMap().backupTerritories() +
                " territories instead.</p>"
                : "<p><b>Winning.</b> The player who holds every territory wins.</p>";
        return "<h2>Risk</h2>" +
                "<p><a href='#setup'>Setup</a> | <a href='#turn'>Turn</a> | <a href='#cards'>Cards</a> | " +
                "<a href='#end'>End</a> | <a href='#screen'>Screen</a></p>" +
                "<h3><a name='setup'>Setup</a></h3>" + setup +
                "Then each player places one army at a time on a territory they hold, until all their starting " +
                "armies are on the board.</p>" +
                "<table border=1 cellpadding=3 cellspacing=0><tr><th>Players</th><th>3</th><th>4</th><th>5</th>" +
                "<th>6</th></tr><tr><td>Starting armies</td><td>" + p.startArmies3 + "</td><td>" + p.startArmies4 +
                "</td><td>" + p.startArmies5 + "</td><td>" + p.startArmies6 + "</td></tr></table>" +
                "<h3><a name='turn'>Turn</a></h3><ol>" +
                "<li><b>Reinforce.</b> You receive the number of territories you hold divided by " +
                p.territoriesPerArmy + " (at least " + p.minReinforcements + "), plus the bonus for each " +
                "continent you hold entirely (shown on the map). Place them one at a time. You may trade in " +
                "cards before placing the last.</li>" +
                "<li><b>Attack</b> (optional). Attack from a territory with at least 2 armies to a neighbouring " +
                "enemy territory, rolling up to " + p.maxAttackDice + " dice but fewer than your armies there. " +
                "The defender " + (p.defenderChoosesDice ? "chooses to roll from 1 to " : "rolls ") +
                p.maxDefendDice + " dice, but no more than their armies there" +
                ". The highest dice are compared in pairs, and the lower of each pair loses one army. The " +
                "defender wins a tie. " +
                (p.allowBlitz ? "Blitz attacks again and again with the most dice until the territory is taken " +
                        "or you are down to one army. " : "") +
                "When you take a territory you move in at least as many armies as dice rolled.</li>" +
                "<li><b>Fortify</b> (optional). Move armies once, from one territory " + fortify +
                ", leaving at least one behind. Then your turn ends.</li></ol>" + limit +
                "<h3><a name='cards'>Cards</a></h3>" +
                "<p>If you took at least one territory in your turn, you draw one card when you end your " +
                "attacks. A set is three cards with the same symbol, one of each symbol, or any two with a wild " +
                "card. The sets traded in by all players are worth " + values + (p.linearTradeValues ? ", and so " +
                "on." : ", and then " + p.tradeValueIncrement + " more each.") + " If a card in the set " +
                "shows a territory you hold, " + p.territoryBonus + " extra armies go on it (once a turn). With " +
                p.handLimit + " or more cards you must trade before placing. If you eliminate a player you take " +
                "their cards, and with " + p.eliminationTradeLimit + " or more you must trade at once until you " +
                "hold fewer than " + p.handLimit + ".</p>" +
                "<h3><a name='end'>End</a></h3>" + winning +
                "<p>A player who loses their last territory is out. After " + p.getMaxRounds() + " rounds the " +
                "game ends, and the players still in are ranked by territories, then armies.</p>" +
                "<h3><a name='screen'>Screen</a></h3><ul>" +
                "<li>Each disc is a territory, in its owner's colour with its armies, ringed in its continent's " +
                "colour. Lines join neighbours. Alaska and Kamchatka are joined off the edges of the map.</li>" +
                "<li>A black ring marks the two territories of a move-in or a defence waiting for a choice.</li>" +
                "<li>The panel on the right lists the players (the one to act is marked &gt;), their cards by " +
                "symbol (I Infantry, C Cavalry, A Artillery, W wild) and their mission when you may see them.</li>" +
                "<li>The actions are listed below the map. Attack(from, to, n) rolls n dice once.</li></ul>";
    }
}
