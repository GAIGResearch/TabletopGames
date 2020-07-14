package evaluation;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.RandomPlayer;
import players.utils.RandomTestPlayer;
import utilities.LineChart;
import utilities.StatSummary;

import java.util.ArrayList;
import java.util.List;

public class GameReport {

    static int nRep = 50;
    static int nPlayers = 2;

    /**
     * Number of actions available from any one game state.
     * @param game - game to test.
     */
    public static void actionSpace(GameType game) {
        System.out.println("--------------------\nAction Space Test: " + game.name() + "\n--------------------");

        StatSummary actionSpace = new StatSummary("All");
        ArrayList<StatSummary> sumData = new ArrayList<>();

        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers; j++) {
                players.add(new RandomPlayer());
            }

            if (g != null) {
                g.reset(players);
                g.run();
                for (int j = 0; j < g.getTick(); j++) {
                    double size = g.getActionSpaceSize().get(j).b;
                    actionSpace.add(size);

                    if (j >= sumData.size()) {
                        sumData.add(new StatSummary("" + j));
                    }
                    sumData.get(j).add(size);
                }
            }
        }

        if (actionSpace.n() != 0) {

            // Average and make plot
            double[] xData = new double[sumData.size()];
            double[] yData = new double[sumData.size()];
            double[] yErr = new double[sumData.size()];
            for (int i = 0; i < sumData.size(); i++) {
                xData[i] = i;
                yData[i] = sumData.get(i).mean();
                yErr[i] = sumData.get(i).stdErr();
            }
            LineChart lc = new LineChart(xData, yData, yErr, game.name() + "Action Space Size",
                    "game tick", "action space size", "Size", false);

            System.out.println(actionSpace.shortString());
        }

        System.out.println();
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
     * Amount of hidden information in the game.
     * @param game - game to test.
     */
    public static void hiddenInformation(GameType game) {
        // Number of hidden components
    }

    /**
     * How fast the game works.
     *  - ForwardModel.setup()
     *  - ForwardModel.next()
     *  - ForwardModel.computeAvailableActions()
     *  - GameState.copy()
     * @param game - game to test.
     */
    public static void gameSpeed(GameType game) {
        System.out.println("--------------------\nSpeed Test: " + game.name() + "\n--------------------");

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
        System.out.println("--------------------\nGame Length Test: " + game.name() + "\n--------------------");

        double nDecisions = 0;
        double nTicks = 0;
        double nRounds = 0;
        double nActionsPerTurn = 0;
        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers; j++) {
                players.add(new RandomPlayer());
            }

            if (g != null) {
                g.reset(players);
                g.run();
                nDecisions += g.getNDecisions();
                nTicks += g.getTick();
                nRounds += g.getGameState().getTurnOrder().getRoundCounter();
                nActionsPerTurn += g.getNActionsPerTurn();
            }
        }

        if (nDecisions != 0) {
            System.out.println("# decisions: " + (nDecisions / nRep));
            System.out.println("# ticks: " + (nTicks / nRep));
            System.out.println("# rounds: " + (nRounds / nRep));
            System.out.println("# actions/turn: " + (nActionsPerTurn / nRep));
        }

        System.out.println();
    }

    /**
     * How sparse is the reward signal in the scoring function? Calculates granularity of possible values between -1 and 1.
     * @param game - game to test.
     */
    public static void rewardSparsity(GameType game) {
        System.out.println("--------------------\nReward Sparsity Test: " + game.name() + "\n--------------------");

        StatSummary stDev = new StatSummary();
        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers; j++) {
                players.add(new RandomTestPlayer());
            }

            if (g != null) {
                g.reset(players);
                g.run();

                for (AbstractPlayer p: players) {
                    RandomTestPlayer rtp = (RandomTestPlayer) p;
                    stDev.add(rtp.getScores());
                }
            }
        }

        if (stDev.n() != 0) {
            System.out.println(stDev.toString());
        }

        System.out.println();
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
        hiddenInformation(game);
//        stateSpaceSize(game); * not implemented

        // Speed tests
//        gameSpeed(game);
//        gameLength(game);

        // Other tests
//        stochasticity(game); * not implemented
//        rewardSparsity(game);
        skillDepth(game);
    }

    public static void main(String[] args) {
        for (GameType gt: GameType.values()) {
            run(gt);
        }
//        run(GameType.Pandemic);
    }
}
