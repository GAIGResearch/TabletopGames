package core;

import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;

/**
 * This is purely for old-style game implementations from before January 2023 that use the now deprecated TurnOrder
 *
 * This has been deprecated because it all too often led to a mixture of logic and state, and ambiguity over where any individual piece
 * of game logic should be implemented.
 * The new standard (See StandardForwardModel) is to have a clean separation of:
 *  - state within something that extends AbstractGameState
 *  - game logic within something that extends AbstractForwardModel (and this has new method hooks to help)
 */
@Deprecated
public abstract class StandardForwardModelWithTurnOrder extends AbstractForwardModel {

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if (action == null) {
            throw new AssertionError("No action selected by current player");
        }
        // The sequence (if any) that this action is a decision for is the one at the top of the stack *before* execution.
        // We can't just register with all items in the Stack, as this may represent some complex dependency
        // For example in Dominion where one can Throne Room a Throne Room, which then Thrones a Smithy
        IExtendedSequence decisionOwner = currentState.isActionInProgress() ? currentState.actionsInProgress.peek() : null;
        action.execute(currentState);
        // We then register the action with that sequence only. Any sequence the action itself started (directly, or via
        // nested actions it executed) must not be told about the action that created it.
        // Anything the owner starts in response goes on top of it, and so is resolved before the owner is removed.
        if (decisionOwner != null && decisionOwner != action) {
            decisionOwner._afterAction(currentState, action);
        }
        _afterAction(currentState, action);
    }

    protected abstract void _afterAction(AbstractGameState currentState, AbstractAction actionTaken);

    @Override
    public void endPlayerTurn(AbstractGameState gs) {
        AbstractGameStateWithTurnOrder state = (AbstractGameStateWithTurnOrder) gs;
        state.getTurnOrder().endPlayerTurn(gs);
    }
}
