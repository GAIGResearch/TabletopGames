package games.connect4.gui;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.connect4.Connect4Constants;
import games.connect4.Connect4GameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Connect4GUIManager extends AbstractGUIManager {

    Connect4BoardView view;

    public Connect4GUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        if (game == null) return;

        Connect4GameState gameState = (Connect4GameState) game.getGameState();
        view = new Connect4BoardView(gameState.getGridBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGridBoard().getWidth());
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Connect 4", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{view},
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
            List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            int start = actions.size();
            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    SetGridValueAction action = (SetGridValueAction) abstractAction;
                    if (action.getX() == r.x/defaultItemSize) { // && action.getY() == r.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, "Play " +
                                Connect4Constants.playerMapping.get(player.getPlayerID()).getComponentName()
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
