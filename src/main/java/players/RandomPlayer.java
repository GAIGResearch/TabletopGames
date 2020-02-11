package players;

import actions.Action;
import core.AIPlayer;
import core.GameState;

import java.util.Random;

public class RandomPlayer implements AIPlayer {

    @Override
    public Action getAction(GameState gameState) {
        int nActions = gameState.nPossibleActions();
        if (nActions > 0) {
            return gameState.possibleActions().get(new Random().nextInt(nActions));
        } else {
            return null;
        }
    }
}
