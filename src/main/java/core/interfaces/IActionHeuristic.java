package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface IActionHeuristic {
    /**
     * Returns a score for the action in the context of a state that should be maximised by the player (the bigger, the better).
     * @param state - game state to evaluate and score.
     * @param action - action to evaluate (to be executed always by the current player)
     * @return - value of given action.
     */
    double evaluateState(AbstractGameState state, AbstractAction action);

}
