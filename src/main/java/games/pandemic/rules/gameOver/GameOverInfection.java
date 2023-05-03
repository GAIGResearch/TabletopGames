package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.GAME_ONGOING;

public class GameOverInfection extends GameOverCondition {
    @Override
    public CoreConstants.GameResult test(AbstractGameState gs) {
        for (String c: colors) {
            if (((Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease Cube " + c))).getValue() < 0) {
                return LOSE_GAME;
            }
        }
        return GAME_ONGOING;
    }
}
