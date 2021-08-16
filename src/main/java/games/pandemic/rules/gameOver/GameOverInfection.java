package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import static games.pandemic.PandemicConstants.*;
import static utilities.Utils.GameResult.LOSE;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class GameOverInfection extends GameOverCondition {
    @Override
    public Utils.GameResult test(AbstractGameState gs) {
        for (String c: colors) {
            if (((Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease Cube " + c))).getValue() < 0) {
                return LOSE;
            }
        }
        return GAME_ONGOING;
    }
}
