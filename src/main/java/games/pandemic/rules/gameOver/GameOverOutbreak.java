package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.outbreaksHash;
import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.GAME_ONGOING;

public class GameOverOutbreak extends GameOverCondition {
    private final int lose_max_outbreak;

    public GameOverOutbreak(int lose_max_outbreak) {
        this.lose_max_outbreak = lose_max_outbreak;
    }

    @Override
    public CoreConstants.GameResult test(AbstractGameState gs) {
        if (((Counter)((PandemicGameState)gs).getComponent(outbreaksHash)).getValue() >= lose_max_outbreak) {
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("Too many outbreaks");
            }
            return LOSE_GAME;
        }
        return GAME_ONGOING;
    }
}
