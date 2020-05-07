package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import games.pandemic.PandemicConstants;
import utilities.Utils;

public abstract class GameOverCondition {
    public abstract Utils.GameResult test(AbstractGameState gs);
}
