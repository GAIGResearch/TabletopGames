package core;

import actions.Action;

public interface ForwardModel {
    void setup(GameState firstState, Game game);
    void next(GameState currentState, Action action);
}
