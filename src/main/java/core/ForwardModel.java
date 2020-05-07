package core;

import core.actions.IAction;

public abstract class ForwardModel {
    public abstract void setup(AbstractGameState firstState);
    public abstract void next(AbstractGameState currentState, IAction action);
}
