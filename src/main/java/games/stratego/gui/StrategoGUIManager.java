package games.stratego.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.stratego.StrategoConstants;
import games.stratego.StrategoGameState;
import games.stratego.actions.Move;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StrategoGUIManager extends AbstractGUIManager {

    StrategoBoardView view;

    public StrategoGUIManager(GamePanel parent, Game game, ActionController ac) {
        super(parent, ac, 1);

        if (game == null) return;

        StrategoGameState gameState = (StrategoGameState) game.getGameState();
        view = new StrategoBoardView(gameState.getGridBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGridBoard().getWidth());
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Stratego", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[]{view.getHighlight()},
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
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    Move action = (Move) abstractAction;
                    if (action.getDestinationCoordinate()[0] == r.x/defaultItemSize && action.getDestinationCoordinate()[1] == r.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, action.getString(gameState));
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
            view.updateComponent(((StrategoGameState)gameState).getGridBoard());
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        parent.repaint();
    }
}
