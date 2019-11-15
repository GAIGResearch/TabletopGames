package players;

import actions.Action;
import core.AIPlayer;
import core.GameState;

public class RandomPlayer implements AIPlayer {

    @Override
    public Action[] getActions(GameState gameState) {
        return new Action[gameState.nInputActions()];
    }
}
