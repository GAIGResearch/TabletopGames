package core;

import actions.Action;

public interface ForwardModel {
    void init();
    void setup(GameState firstState);
    void next(GameState currentState, Action[] actions);
}
