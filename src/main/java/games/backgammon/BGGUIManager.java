package games.backgammon;

import core.*;
import core.actions.AbstractAction;
import games.dotsboxes.AddGridCellEdge;
import games.dotsboxes.DBEdge;
import gui.*;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class BGGUIManager extends AbstractGUIManager {

    BackgammonBoardView view;
    static int backgammonWidth = 800;
    static int backgammonHeight = 600;

    public BGGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        BGGameState state = (BGGameState) game.getGameState();
        view = new BackgammonBoardView((BGForwardModel) game.getForwardModel());

        width = backgammonWidth;
        height = backgammonHeight;

        JPanel infoPanel = createGameStateInfoPanel("Backgammon", state, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
    }

    @Override
    public int getMaxActionSpace() {
        return 10; // Adjust based on the maximum number of actions in Backgammon
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.update((BGGameState) gameState);
        }
    }


    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        BGGameState bgState = (BGGameState) gameState;

        if (view.firstClick != -1 && view.secondClick != -1) {
            int playerId = bgState.getCurrentPlayer();
            int from = playerId == 0 ? view.firstClick : 25 - view.firstClick;
            int to = playerId == 0 ? view.secondClick : 25 - view.secondClick;

            // Handle special cases for the bar (0) and bearing off (25)
            if (view.firstClick == 25) from = 0; // Bar
            if (view.secondClick == 0) to = 0; // Bearing off

            // Check if a valid MovePiece action exists
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(bgState);
            for (AbstractAction action : actions) {
                if (action instanceof MovePiece move) {
                    if (move.from == from - 1 && move.to == to - 1) {
                        ac.addAction(move);
                        break;
                    }
                }
            }

            // If no valid action exists, reset the clicks
            view.firstClick = -1;
            view.secondClick = -1;
        }
    }
}