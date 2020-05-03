package core;

import actions.IAction;
import turnorder.TurnOrder;

public abstract class ForwardModel {
    public abstract void next(AbstractGameState currentState, TurnOrder turnOrder, IAction IAction);
}
