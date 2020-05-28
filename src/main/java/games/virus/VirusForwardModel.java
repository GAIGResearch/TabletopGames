package games.virus;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;
import utilities.Utils;

public class VirusForwardModel extends ForwardModel {
    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        action.execute(gameState);
        ((VirusGameState) gameState).checkWinCondition();
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING)
            ((VirusGameState) gameState).endTurn();
    }
}
