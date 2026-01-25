package games.pentegrammai;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class PenteGUIManager extends AbstractGUIManager {

    PenteBoardView boardView;
    static int penteWidth = 600;
    static int penteHeight = 200;

    public PenteGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        boardView = new PenteBoardView((PenteForwardModel) game.getForwardModel());

        width = penteWidth;
        height = penteHeight;

        JPanel infoPanel = createGameStateInfoPanel("Pente Grammai", game.getGameState(), width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(boardView, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
    }

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    @Override
    public int getMaxActionSpace() {
        return 10;
    }

    /**
     * Updates all GUI elements given current game state and player that is currently acting.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            boardView.update((PenteGameState) gameState);
        }
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        PenteGameState state = (PenteGameState) gameState;
        if (boardView.firstClick != -1 && boardView.secondClick != -1) {
            int from = boardView.firstClick;
            int to = boardView.secondClick;

            List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(state);
            for (core.actions.AbstractAction action : actions) {
                if (action instanceof PenteMoveAction move) {
                    if (move.from == from && move.to == to) {
                        ac.addAction(move);
                        break;
                    }
                }
            }
            boardView.firstClick = -1;
            boardView.secondClick = -1;
        }
    }
}
