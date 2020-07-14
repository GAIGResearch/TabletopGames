package evaluation;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

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
     * How fast the game works.
     *  - ForwardModel.next()
     *  - ForwardModel.computeAvailableActions()
     *  - GameState.copy()
     * @param game - game to test.
     */
    public static void gameSpeed(GameType game) {
        int nRep = 50;
        int nPlayers = 2;

        double nextT = 0;
        double copyT = 0;
        double actionT = 0;
        double setupT = 0;

        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers; j++) {
                players.add(new RandomPlayer());
            }

            if (g != null) {
                // Setup timer
                AbstractForwardModel fm = g.getForwardModel();
                long s = System.nanoTime();
                fm.setup(g.getGameState());
                setupT += (System.nanoTime() - s);

                // Run timers
                g.reset(players);
                g.run();
                nextT += g.getNextTime();
                copyT += g.getCopyTime();
                actionT += g.getActionComputeTime();

            }
        }

        if (nextT != 0) {
            System.out.println("GS.copy(): " + String.format("%6.3e", 1e+9 / (copyT / nRep)) + " executions/second");
            System.out.println("FM.setup(): " + String.format("%6.3e", 1e+9 / (setupT / nRep)) + " executions/second");
            System.out.println("FM.next(): " + String.format("%6.3e", 1e+9 / (nextT / nRep)) + " executions/second");
            System.out.println("FM.computeAvailableActions(): " + String.format("%6.3e", 1e+9 / (actionT / nRep)) + " executions/second");
        }

        System.out.println();
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
        gameSpeed(game);
        gameLength(game);
        turnLength(game);

        // Other tests
        stochasticity(game);
        rewardSparsity(game);
        skillDepth(game);
    }

    public static void main(String[] args) {
        for (GameType gt: GameType.values()) {
            System.out.println(gt.name());
            run(gt);
        }
//        run(GameType.Pandemic);
    }
}
