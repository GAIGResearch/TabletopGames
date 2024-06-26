package core.forwardModels;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;


/**
 * This is a forward model that implements a simple (but common) pattern in which every player takes one action in turn.
 * After the last player has taken their turn, we have finished one round, and the firstPlayer shifts round one clockwise.
 *
 * Hence if we have players 0, 1, 2, the sequence of play is:
 * Round 0 - players 0, 1, 2
 * Round 2 - players 1, 2, 0
 * Round 3 - players 2, 0, 1
 * ... and so on
 *
 * To extend this you then just need to implement:
 * _setup() to initialise the game state
 * _computeAvailableActions to define the list of actions available at any given game state
 *
 */
public abstract class RotatingFirstPlayerForwardModel extends StandardForwardModel {
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // All we need to do is end the player turn after every action
        endPlayerTurn(currentState);
        if (currentState.getCurrentPlayer() == currentState.getFirstPlayer())
            endRound(currentState, (currentState.getCurrentPlayer() + 1) % currentState.getNPlayers());
    }
}
