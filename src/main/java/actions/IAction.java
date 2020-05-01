package actions;

import core.GameState;

public interface Action {
    boolean execute(GameState gs);
}
