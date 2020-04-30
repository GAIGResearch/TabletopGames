package pandemic.engine.gameOver;

import core.GameState;

public abstract class GameOverCondition {
    public abstract int test(GameState gs);
}
