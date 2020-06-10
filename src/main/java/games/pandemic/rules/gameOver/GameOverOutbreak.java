package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;
import utilities.Utils;

import static core.CoreConstants.VERBOSE;
import static games.pandemic.PandemicConstants.outbreaksHash;
import static utilities.Utils.GameResult.LOSE;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class GameOverOutbreak extends GameOverCondition {
    private int lose_max_outbreak;

    public GameOverOutbreak(int lose_max_outbreak) {
        this.lose_max_outbreak = lose_max_outbreak;
    }

    @Override
    public Utils.GameResult test(AbstractGameState gs) {
        if (((Counter)((PandemicGameState)gs).getComponent(outbreaksHash)).getValue() >= lose_max_outbreak) {
            if (VERBOSE) {
                System.out.println("Too many outbreaks");
            }
            return LOSE;
        }
        return GAME_ONGOING;
    }
}
