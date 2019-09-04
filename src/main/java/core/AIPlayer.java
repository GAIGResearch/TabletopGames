package core;

import actions.Action;

public interface AIPlayer {
    Action[] getActions(GameState gameState);
}
