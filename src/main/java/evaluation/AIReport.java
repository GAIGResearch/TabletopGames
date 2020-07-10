package evaluation;

import core.AbstractPlayer;
import games.GameType;
import players.RandomPlayer;

import static games.GameType.Pandemic;

public class AIReport {

    /**
     * How fast is the player at making decisions.
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void actSpeed(AbstractPlayer player, GameType game) {

    }

    /**
     * How much the player uses randomness in its thinking process.
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void stochasticity(AbstractPlayer player, GameType game) {

    }

    /**
     * The size of the search trees built by the given player in the given game.
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void searchTreeSize(AbstractPlayer player, GameType game) {

    }

    /**
     * Runs the test with given player
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void run(AbstractPlayer player, GameType game) {
        actSpeed(player, game);
        stochasticity(player, game);
        searchTreeSize(player, game);
    }

    public static void main(String[] args) {
        run(new RandomPlayer(), Pandemic);
    }
}
