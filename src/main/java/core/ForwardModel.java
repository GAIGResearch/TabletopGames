package core;

public interface ForwardModel {
    void setup(GameState firstState);
    void next(GameState currentState, int[] actions);
}
