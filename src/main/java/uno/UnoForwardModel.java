package uno;


import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;
import utilities.Utils;

public class UnoForwardModel extends ForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        action.execute(gameState);
        ((UnoGameState) gameState).checkWinCondition();
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING)
            ((UnoGameState) gameState).endTurn();
    }
}

