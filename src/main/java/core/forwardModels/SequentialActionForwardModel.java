package core.forwardModels;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;

/**
 * This is a forward model that implements a simple (but common) pattern in which every player takes one action in turn.
 * After the last player has taken their turn, play proceeds back to the first player.
 *
 * We count one Round as being a Turn for each player
 *
 * To extend this you then just need to implement:
 * _setup() to initialise the game state
 * _computeAvailableActions to define the list of actions available at any given game state
 *
 */
public abstract class SequentialActionForwardModel extends StandardForwardModel {

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // All we need to do is end the player turn after every action
        endPlayerTurn(currentState);
        if (currentState.getCurrentPlayer() == 0)
            endRound(currentState);
    }
}
