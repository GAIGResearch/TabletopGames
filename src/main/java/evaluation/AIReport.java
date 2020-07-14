package evaluation;

import games.GameType;
import players.utils.PlayerType;

import static games.GameType.Pandemic;

public class AIReport {

    /**
     * How fast is the player at making decisions.
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void actSpeed(PlayerType player, GameType game) {

    }

    /**
     * Runs the test with given player
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void run(PlayerType player, GameType game) {
        actSpeed(player, game);
//        stochasticity(player, game);
//        searchTreeSize(player, game);
    }

    public static void main(String[] args) {
        run(PlayerType.Random, Pandemic);
    }
}
