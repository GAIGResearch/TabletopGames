package players;

import actions.Action;
import core.AIPlayer;
import core.GameState;

import java.util.Random;

public class RandomPlayer implements AIPlayer {

    @Override
    public Action getAction(GameState gameState) {
        return gameState.possibleActions(null).get(new Random().nextInt(gameState.nPossibleActions()));
    }
}
