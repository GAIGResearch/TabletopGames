package pandemic.engine.gameOver;

import core.GameState;

import static pandemic.Constants.*;

public class GameOverInfection extends GameOverCondition {
    @Override
    public int test(GameState gs) {
        for (String c: colors) {
            if (gs.findCounter("Disease Cube " + c).getValue() < 0) {
                System.out.println("Ran out of disease cubes");
                return GAME_LOSE;
            }
        }
        return GAME_ONGOING;
    }
}
