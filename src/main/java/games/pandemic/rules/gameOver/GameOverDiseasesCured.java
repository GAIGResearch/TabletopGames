package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.rules.GameOverCondition;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import static core.CoreConstants.GameResult.GAME_ONGOING;
import static core.CoreConstants.GameResult.WIN_GAME;

public class GameOverDiseasesCured extends GameOverCondition {
    @Override
    public CoreConstants.GameResult test(AbstractGameState gs) {
        boolean all_cured = true;
        for (String c : PandemicConstants.colors) {
            if (((Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease " + c))).getValue() < 1) all_cured = false;
        }
        if (all_cured) {
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("WIN!");
            }
            return WIN_GAME;
        }
        return GAME_ONGOING;
    }
}
