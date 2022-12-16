package core;

import core.actions.AbstractAction;

public abstract class StandardForwardModel extends AbstractForwardModel {


    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action){
        action.execute(currentState);
        _afterAction(currentState, action);
    }

    protected abstract void _afterAction(AbstractGameState currentState, AbstractAction actionTaken);

    @Override
    public StandardForwardModel _copy() {
        return this;
    }
}
