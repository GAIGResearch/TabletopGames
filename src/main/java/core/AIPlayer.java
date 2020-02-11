package core;

import actions.Action;

public interface AIPlayer {
    Action getAction(GameState gameState);
}
