package updated_core;

import updated_core.actions.IAction;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public abstract class ForwardModel {
    public abstract void setup(AbstractGameState firstState);
    public abstract void next(AbstractGameState currentState, TurnOrder turnOrder, IAction action);
}
