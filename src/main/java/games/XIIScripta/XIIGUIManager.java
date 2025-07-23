package games.XIIScripta;

import core.*;
import core.actions.AbstractAction;
import games.backgammon.BGGameState;
import gui.*;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

import games.backgammon.MovePiece;

public class XIIGUIManager extends AbstractGUIManager {

    XIIBoardView view;
    static int boardWidth = 900;
    static int boardHeight = 600;

    public XIIGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        BGGameState state = (BGGameState) game.getGameState();
        view = new XIIBoardView((games.backgammon.BGForwardModel) game.getForwardModel());

        width = boardWidth;
        height = boardHeight;

        JPanel infoPanel = createGameStateInfoPanel("XII Scripta", state, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
    }

    @Override
    public int getMaxActionSpace() {
        return 10;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.update((BGGameState) gameState);
        }
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        BGGameState xiiState = (BGGameState) gameState;

        if (view.getFirstClick() != -1 && view.getSecondClick() != -1) {
            int fromGui = view.getFirstClick();
            int toGui = view.getSecondClick();

            // Convert GUI space to game state space
            int from = (fromGui >= 1 && fromGui <= 36) ? 37 - fromGui : 0;
            int to = (toGui >= 1 && toGui <= 36) ? 37 - toGui : -1;

            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(xiiState);
            for (AbstractAction action : actions) {
                if (action instanceof MovePiece move) {
                    if (move.from == from && move.to == to) {
                        ac.addAction(move);
                        break;
                    }
                }
            }
            view.setFirstClick(-1);
            view.setSecondClick(-1);
        }
    }
}
