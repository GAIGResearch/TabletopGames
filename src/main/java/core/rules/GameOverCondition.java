package core.rules;

import core.AbstractGameState;
import utilities.Utils;

/**
 * Tests a game over condition, returning the result of the game.
 */
public abstract class GameOverCondition {
    public abstract Utils.GameResult test(AbstractGameState gs);
}
