package core;

import core.actions.AbstractAction;

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
        if (action != null) {
            action.execute(currentState);
        } else {
            throw new AssertionError("No action selected by current player");
        }
        _afterAction(currentState, action);
    }

    protected abstract void _afterAction(AbstractGameState currentState, AbstractAction actionTaken);

    @Override
    public void endPlayerTurn(AbstractGameState gs) {
        AbstractGameStateWithTurnOrder state = (AbstractGameStateWithTurnOrder) gs;
        state.getTurnOrder().endPlayerTurn(gs);
    }

    @Override
    public StandardForwardModelWithTurnOrder _copy() {
        return this;
    }
}
