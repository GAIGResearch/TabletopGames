package core;

import actions.Action;

import java.util.Random;

public interface ForwardModel {
    void setup(GameState firstState);
    void next(GameState currentState, Action action);
    ForwardModel copy();
}
