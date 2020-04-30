package pandemic.engine.gameOver;

import core.GameState;
import pandemic.Constants;

import static pandemic.Constants.*;

public class GameOverDiseasesCured extends GameOverCondition {
    @Override
    public int test(GameState gs) {
        boolean all_cured = true;
        for (String c : Constants.colors) {
            if (gs.findCounter("Disease " + c).getValue() < 1) all_cured = false;
        }
        if (all_cured) {
            System.out.println("WIN!");
            return GAME_WIN;
        }
        return GAME_ONGOING;
    }
}
