package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import core.components.Counter;
import games.pandemic.Constants;
import games.pandemic.PandemicGameState;

import static games.pandemic.Constants.GameResult.GAME_LOSE;
import static games.pandemic.Constants.GameResult.GAME_ONGOING;
import static games.pandemic.Constants.outbreaksHash;

public class GameOverOutbreak extends GameOverCondition {
    private int lose_max_outbreak;

    public GameOverOutbreak(int lose_max_outbreak) {
        this.lose_max_outbreak = lose_max_outbreak;
    }

    @Override
    public Constants.GameResult test(AbstractGameState gs) {
        if (((Counter)((PandemicGameState)gs).getComponent(outbreaksHash)).getValue() >= lose_max_outbreak) {
            System.out.println("Too many outbreaks");
            return GAME_LOSE;
        }
        return GAME_ONGOING;
    }
}
