package players;

import actions.IAction;
import core.GameState;

public interface AIPlayer {
    IAction getAction(GameState gameState);
}
