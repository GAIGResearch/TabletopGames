package players;

import actions.Action;
import core.AIPlayer;
import core.GameState;

import java.util.Random;

public class RandomPlayer implements AIPlayer {

    /**
     * Random generator for this agent.
     */
    private Random rnd;

    public RandomPlayer(Random rnd)
    {
        this.rnd = rnd;
    }

    @Override
    public Action getAction(GameState gameState) {
        return gameState.possibleActions().get(rnd.nextInt(gameState.nPossibleActions()));
    }
}
