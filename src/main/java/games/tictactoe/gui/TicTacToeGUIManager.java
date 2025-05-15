package games.tictactoe.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import games.tictactoe.TicTacToeConstants;
import games.tictactoe.TicTacToeGameState;
import gui.IScreenHighlight;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TicTacToeGUIManager extends AbstractGUIManager {

    TTTBoardView view;

    public TicTacToeGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        TicTacToeGameState gameState = (TicTacToeGameState) game.getGameState();
        view = new TTTBoardView(gameState.getGridBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGridBoard().getWidth());
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Tic Tac Toe", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{view},
                width, defaultActionPanelHeight, true);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        return 1;
    }

    /**
     * Only shows actions for highlighted cell.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            if (!highlight.isEmpty()) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    SetGridValueAction action = (SetGridValueAction) abstractAction;
                    if (action.getX() == r.x/defaultItemSize && action.getY() == r.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, "Play " +
                                TicTacToeConstants.playerMapping.get(player.getPlayerID()).getComponentName());
                        break;
                    }
                }
            } else {
                actionButtons[0].setVisible(false);
                actionButtons[0].setButtonAction(null, "");
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateComponent(((TicTacToeGameState)gameState).getGridBoard());
        }
    }
}
