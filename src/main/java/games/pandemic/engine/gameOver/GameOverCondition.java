package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import games.pandemic.Constants;

public abstract class GameOverCondition {
    public abstract Constants.GameResult test(AbstractGameState gs);
}
