package games.tictactoe.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import games.tictactoe.TicTacToeGameState;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TicTacToeGUI extends AbstractGUI {

    TTTBoardView view;
    int width, height;

    public TicTacToeGUI(Game game, ActionController ac) {
        super(ac, 1);
        if (game == null) return;

        TicTacToeGameState gameState = (TicTacToeGameState) game.getGameState();
        view = new TTTBoardView(gameState.getGridBoard());

        // Set width/height of display
        this.width = defaultItemSize * gameState.getGridBoard().getWidth();
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Tic Tac Toe", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[]{view.getHighlight()},
                width, defaultActionPanelHeight);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    /**
     * Only shows actions for highlighted cell.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            int start = actions.size();
            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    SetGridValueAction<Character> action = (SetGridValueAction<Character>) abstractAction;
                    if (action.getX() == r.x/defaultItemSize && action.getY() == r.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, "Play " +
                                ((TicTacToeGameState) gameState).getPlayerMapping().get(player.getPlayerID()));
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
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height + defaultActionPanelHeight*2 + defaultInfoPanelHeight);
    }
}
