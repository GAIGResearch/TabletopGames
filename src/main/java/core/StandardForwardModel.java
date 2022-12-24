package core;

import core.actions.AbstractAction;
import utilities.Utils;

public abstract class StandardForwardModel extends AbstractForwardModel {

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action){
        if (action != null) {
            action.execute(currentState);
        } else {
            throw new AssertionError("No action selected by current player");
        }
        _afterAction(currentState, action);
    }

    protected abstract void _afterAction(AbstractGameState currentState, AbstractAction actionTaken);

    @Override
    public StandardForwardModel _copy() {
        return this;
    }
}
