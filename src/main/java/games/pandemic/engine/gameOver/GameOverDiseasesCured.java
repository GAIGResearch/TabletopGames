package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import games.pandemic.Constants;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import static games.pandemic.Constants.GameResult.GAME_ONGOING;
import static games.pandemic.Constants.GameResult.GAME_WIN;

public class GameOverDiseasesCured extends GameOverCondition {
    @Override
    public Constants.GameResult test(AbstractGameState gs) {
        boolean all_cured = true;
        for (String c : Constants.colors) {
            if (((Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease " + c))).getValue() < 1) all_cured = false;
        }
        if (all_cured) {
            System.out.println("WIN!");
            return GAME_WIN;
        }
        return GAME_ONGOING;
    }
}
