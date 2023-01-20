package core;

import core.actions.AbstractAction;

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
