package explodingkittens.actions;

import core.GameState;

public interface IsNopeable {
    public boolean nopedExecute(GameState gs);
}
