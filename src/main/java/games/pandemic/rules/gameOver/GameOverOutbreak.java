package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;
import utilities.Utils;

import static games.pandemic.PandemicConstants.outbreaksHash;
import static utilities.Utils.GameResult.LOSE;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class GameOverOutbreak extends GameOverCondition {
    private final int lose_max_outbreak;

    public GameOverOutbreak(int lose_max_outbreak) {
        this.lose_max_outbreak = lose_max_outbreak;
    }

    @Override
    public Utils.GameResult test(AbstractGameState gs) {
        if (((Counter)((PandemicGameState)gs).getComponent(outbreaksHash)).getValue() >= lose_max_outbreak) {
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("Too many outbreaks");
            }
            return LOSE;
        }
        return GAME_ONGOING;
    }
}
