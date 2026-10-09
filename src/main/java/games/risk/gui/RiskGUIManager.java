package games.risk.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
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
            agentNames[i] = game.getPlayers().get(i).toString();
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
        mapView.update(state);
        playersView.update(state);
        parent.repaint();
    }
}
