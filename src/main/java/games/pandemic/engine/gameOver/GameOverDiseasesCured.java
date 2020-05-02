package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import static utilities.Utils.GameResult.GAME_ONGOING;
import static utilities.Utils.GameResult.GAME_WIN;

public class GameOverDiseasesCured extends GameOverCondition {
    @Override
    public Utils.GameResult test(AbstractGameState gs) {
        boolean all_cured = true;
        for (String c : PandemicConstants.colors) {
            if (((Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease " + c))).getValue() < 1) all_cured = false;
        }
        if (all_cured) {
//            System.out.println("WIN!");
            return GAME_WIN;
        }
        return GAME_ONGOING;
    }
}
