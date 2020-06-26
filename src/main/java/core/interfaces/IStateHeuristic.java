
package core.interfaces;

import core.AbstractGameState;

public interface IStateHeuristic {

    /**
     * Returns a score for the state that should be maximised by the player (the bigger, the better).
     * Ideally bounded between [-1, 1].
     * @param gs - game state to evaluate and score.
     * @return - value of given state.
     */
    double evaluateState(AbstractGameState gs);
}