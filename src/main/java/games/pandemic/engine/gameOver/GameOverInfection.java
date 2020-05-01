package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import static games.pandemic.Constants.*;
import static games.pandemic.Constants.GameResult.GAME_LOSE;
import static games.pandemic.Constants.GameResult.GAME_ONGOING;

public class GameOverInfection extends GameOverCondition {
    @Override
    public GameResult test(AbstractGameState gs) {
        for (String c: colors) {
            if (((Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease Cube " + c))).getValue() < 0) {
                System.out.println("Ran out of disease cubes");
                return GAME_LOSE;
            }
        }
        return GAME_ONGOING;
    }
}
