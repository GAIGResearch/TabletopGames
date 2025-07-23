package games.backgammon;

import core.*;
import core.actions.AbstractAction;
import gui.*;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class BGGUIManager extends AbstractGUIManager {

    BGBoardView view;
    static int backgammonWidth = 800;
    static int backgammonHeight = 600;

    public BGGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        BGGameState state = (BGGameState) game.getGameState();
        view = new BGBoardView((BGForwardModel) game.getForwardModel());

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
            int from = view.firstClick;
            int to = view.secondClick;

            // Handle special cases for the bar and bearing off
            if (view.firstClick == 25) from = playerId == 0 ? 0 : -1; // bottom right
            if (view.secondClick == 25) to = playerId == 0 ? 0 : -1;
            if (view.secondClick == 0) to = playerId == 0 ? -1 : 0; // top right
            if (view.firstClick == 0) from = playerId == 0 ? -1 : 0; //

//            System.out.printf("Checking action from %d to %d for player %d%n", from, to, playerId);

            // Check if a valid MovePiece action exists
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(bgState);
            for (AbstractAction action : actions) {
                if (action instanceof MovePiece move) {
                    if (move.from == from && move.to == to) {
                        ac.addAction(move);
                        System.out.printf("Valid action found: MovePiece from %d to %d%n", from, to);
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