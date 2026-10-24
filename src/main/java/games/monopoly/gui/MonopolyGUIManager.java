package games.monopoly.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.monopoly.MonopolyGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * GUI for Monopoly: the board, and beside it a table of the players with the action list below it.
 */
public class MonopolyGUIManager extends AbstractGUIManager {

    MonopolyBoardView boardView;
    MonopolyPlayersView playersView;

    public MonopolyGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;
        MonopolyGameState state = (MonopolyGameState) gameState;

        String[] agentNames = new String[state.getNPlayers()];
        for (int i = 0; i < agentNames.length; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();
        }
        boardView = new MonopolyBoardView();
        playersView = new MonopolyPlayersView(agentNames);

        // the action list goes in the column beside the board, under the players, to keep the window short enough
        // for a laptop screen
        int sideWidth = MonopolyPlayersView.WIDTH;
        this.width = MonopolyBoardView.SIZE + sideWidth + 20;
        this.height = MonopolyBoardView.SIZE;
        int actionHeight = height - playersView.getPreferredSize().height - 8;

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.add(playersView);
        side.add(Box.createVerticalStrut(8));
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], sideWidth, actionHeight);
        actionPanel.setPreferredSize(new Dimension(sideWidth, actionHeight));
        actionPanel.setMaximumSize(new Dimension(sideWidth, actionHeight));
        side.add(actionPanel);

        JPanel gameArea = new JPanel(new BorderLayout(10, 0));
        gameArea.setOpaque(false);
        gameArea.add(boardView, BorderLayout.CENTER);
        gameArea.add(side, BorderLayout.EAST);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(createGameStateInfoPanel("Monopoly", gameState, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(gameArea, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.Monopoly, state.getGameParameters(), height + defaultInfoPanelHeight - 10);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultInfoPanelHeight + 45));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // MANAGE offers at most two actions for each of the 22 streets (build and sell, or build and mortgage), one
        // for each of the 6 stations and utilities, and the roll or the end of the turn: 51. Raising money offers
        // fewer, an auction at most the bid increments and a pass, and a roll in Jail at most 4
        return 55;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof MonopolyGameState state)) return;
        boardView.update(state);
        playersView.update(state);
        parent.repaint();
    }
}
