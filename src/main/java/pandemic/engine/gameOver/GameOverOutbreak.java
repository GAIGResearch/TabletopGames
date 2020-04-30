package pandemic.engine.gameOver;

import core.GameState;

import static pandemic.Constants.*;

public class GameOverOutbreak extends GameOverCondition {
    private int lose_max_outbreak;

    public GameOverOutbreak(int lose_max_outbreak) {
        this.lose_max_outbreak = lose_max_outbreak;
    }

    @Override
    public int test(GameState gs) {
        if (gs.findCounter("Outbreaks").getValue() >= lose_max_outbreak) {
            System.out.println("Too many outbreaks");
            return GAME_LOSE;
        }
        return GAME_ONGOING;
    }
}
