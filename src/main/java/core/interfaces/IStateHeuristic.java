
package core.interfaces;

import core.AbstractGameState;
import players.heuristics.StateHeuristicType;

public interface IStateHeuristic {

    /**
     * Returns a score for the state that should be maximised by the player (the higher, the better).
     * Ideally bounded between [-1, 1].
     * @param gs - game state to evaluate and score.
     * @param playerId - id of the player we're evaluating the game for.
     * @return - value of given state.
     */
    double evaluateState(AbstractGameState gs, int playerId);

    default double minValue() {
        return -1;
    }
    default double maxValue() {
        return +1;
    }

    default StateHeuristicType getType() {
        return StateHeuristicType.GameSpecificHeuristic;
    };

}