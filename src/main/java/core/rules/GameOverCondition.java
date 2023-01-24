package core.rules;

import core.AbstractGameState;
import core.CoreConstants;

/**
 * Tests a game over condition, returning the result of the game.
 */
public abstract class GameOverCondition {
    public abstract CoreConstants.GameResult test(AbstractGameState gs);
}
