package updated_core;

import updated_core.actions.IAction;
import updated_core.gamestates.GameState;

public abstract class ForwardModel {
    public abstract void setup(GameState firstState);
    public abstract void next(GameState currentState, IAction action);
}
