package evaluation;

import games.GameType;

public class GameReport {

    /**
     * Number of actions available from any one game state.
     * @param game - game to test.
     */
    public static void actionSpace(GameType game) {
        // Run game with OSLA, count actions, print stats and show plots
    }

    /**
     * Number of distinct states that can be reached from any one game state.
     * @param game - game to test.
     */
    public static void branchingFactor(GameType game) {
        // Run game with OSLA, count unique states from actions, print stats and show plots
    }

    /**
     * Size of a game state, i.e. number of components.
     * @param game - game to test.
     */
    public static void stateSize(GameType game) {
        // Number of components
    }

    /**
     * Total number of game states possible in the game.
     * @param game - game to test.
     */
    public static void stateSpaceSize(GameType game) {
        // Unsure
    }

    /**
     * Amount of hidden information in the game.
     * @param game - game to test.
     */
    public static void hiddenInformation(GameType game) {
        // Number of hidden components
    }

    /**
     * How fast the next function computes the next state.
     * @param game - game to test.
     */
    public static void gameSpeedNext(GameType game) {

    }

    /**
     * How fast the copy function computes the next state.
     * @param game - game to test.
     */
    public static void gameSpeedCopy(GameType game) {

    }

    /**
     * How fast can the game calculate the actions available for the player.
     * @param game - game to test.
     */
    public static void gameSpeedComputeActions(GameType game) {

    }

    /**
     * How many decisions players take in a game from beginning to end. Alternatively, number of rounds.
     * @param game - game to test.
     */
    public static void gameLength(GameType game) {

    }

    /**
     * Number of actions taken by one player in a turn.
     * @param game - game to test.
     */
    public static void turnLength(GameType game) {

    }

    /**
     * How stochastic is a game? Counts number of calls for the random seed.
     * @param game - game to test.
     */
    public static void stochasticity(GameType game) {

    }

    /**
     * How sparse is the reward signal in the scoring function? Calculates granularity of possible values between -1 and 1.
     * @param game - game to test.
     */
    public static void rewardSparsity(GameType game) {

    }

    /**
     * Calculates the difference in performance between skilled and unskilled players. Signals if better agents perform
     * worse, or if random performs better.
     * @param game - game to test.
     */
    public static void skillDepth(GameType game) {

    }

    /**
     * Runs tests for the given game.
     * @param game - game to test.
     */
    public static void run(GameType game) {
        // Action and state tests
        actionSpace(game);
        branchingFactor(game);
        stateSize(game);
        stateSpaceSize(game);
        hiddenInformation(game);

        // Speed tests
        gameSpeedNext(game);
        gameSpeedCopy(game);
        gameSpeedComputeActions(game);
        gameLength(game);
        turnLength(game);

        // Other tests
        stochasticity(game);
        rewardSparsity(game);
        skillDepth(game);
    }

    public static void main(String[] args) {
        run(GameType.Pandemic);
    }
}
