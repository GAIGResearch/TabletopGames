package games.hareandtortoise.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.hareandtortoise.HareAndTortoiseGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * GUI for Hare and Tortoise: the board with the runners on it, and beside it a table of the players and the hare
 * cards.
 */
public class HareAndTortoiseGUIManager extends AbstractGUIManager {

    HareAndTortoiseBoardView boardView;
    HareAndTortoisePlayersView playersView;

    public HareAndTortoiseGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gameState;

        String[] agentNames = new String[state.getNPlayers()];
        for (int i = 0; i < agentNames.length; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();
        }
        boardView = new HareAndTortoiseBoardView();
        playersView = new HareAndTortoisePlayersView(agentNames);

        // the action list goes in the column beside the board, under the players, to keep the window short enough
        // for a laptop screen
        Dimension boardSize = boardView.getPreferredSize();
        int sideWidth = HareAndTortoisePlayersView.WIDTH;
        this.width = boardSize.width + sideWidth + 20;
        this.height = boardSize.height;
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
        main.add(createGameStateInfoPanel("Hare and Tortoise", gameState, width, defaultInfoPanelHeight),
                BorderLayout.NORTH);
        main.add(gameArea, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.HareAndTortoise, state.getGameParameters(), height + defaultInfoPanelHeight - 10);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultInfoPanelHeight + 30));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // a turn offers at most a forward move to each of the 63 squares, HOME, the move back to a tortoise square
        // and the two ways of chewing a carrot: 67 actions
        return 70;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof HareAndTortoiseGameState state)) return;
        boardView.update(state);
        playersView.update(state);
        parent.repaint();
    }
}
