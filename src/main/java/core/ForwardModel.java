package core;

import core.actions.IAction;

public abstract class ForwardModel {
    public abstract void next(AbstractGameState currentState, IAction action);
}
