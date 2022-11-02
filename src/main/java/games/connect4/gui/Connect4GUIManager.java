package games.connect4.gui;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.connect4.Connect4Constants;
import games.connect4.Connect4GameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.ScreenHighlight;
import players.human.ActionController;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Connect4GUIManager extends AbstractGUIManager {

    Connect4BoardView view;

    public Connect4GUIManager(GamePanel parent, Game game, ActionController ac) {
        super(parent, ac, 1);
        if (game == null) return;

        Connect4GameState gameState = (Connect4GameState) game.getGameState();
        view = new Connect4BoardView(gameState.getGridBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGridBoard().getWidth());
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Connect 4", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new ScreenHighlight[]{view},
                width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * Only shows actions for highlighted cell.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            int start = actions.size();
            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    SetGridValueAction<Token> action = (SetGridValueAction<Token>) abstractAction;
                    if (action.getX() == r.x/defaultItemSize) { // && action.getY() == r.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, "Play " + Connect4Constants.playerMapping.get(player.getPlayerID())
                          + " in column " + (action.getX() + 1));
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
            view.updateComponent(((Connect4GameState)gameState).getGridBoard());
            view.setWinningCells(((Connect4GameState)gameState).getWinningCells());
        }
    }
}
