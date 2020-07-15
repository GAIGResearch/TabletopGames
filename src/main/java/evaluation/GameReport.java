package evaluation;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import evaluation.testplayers.RandomTestPlayer;
import games.GameType;
import players.RandomPlayer;
import utilities.BoxPlot;
import utilities.LineChart;
import utilities.StatSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static games.GameType.*;

public class GameReport {

    static boolean VERBOSE = false;
    static int nRep = 100;

    /**
     * Number of actions available from any one game state.
     * @param game - game to test.
     * @param nPlayers - number of players taking part in this test.
     * @param lc - line chart to add this data to, can be null if only printing required
     */
    public static StatSummary actionSpace(GameType game, int nPlayers, LineChart lc) {
        if (VERBOSE) {
            System.out.println("--------------------\nAction Space Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");
        }

        StatSummary actionSpace = new StatSummary(game.name() + "-" + nPlayers + "p");
        StatSummary actionSpaceOnePerRep = new StatSummary(game.name() + "-" + nPlayers + "p");
        ArrayList<StatSummary> sumData = new ArrayList<>();

        for (int i = 0; i < nRep; i++) {
            StatSummary ss = new StatSummary();

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
                    ss.add(size);

                    if (j >= sumData.size()) {
                        sumData.add(new StatSummary(nPlayers + "-" + j));
                    }
                    sumData.get(j).add(size);
                }
            }

            actionSpaceOnePerRep.add(ss.mean());
        }

        if (actionSpace.n() != 0) {

            if (lc != null) {
                // Add to plot
                double[] yData = new double[sumData.size()];
                for (int i = 0; i < sumData.size(); i++) {
                    yData[i] = sumData.get(i).mean();
                }
                lc.addSeries(yData, game.name() + "-" + nPlayers + "p");
            }

            if (VERBOSE) {
                System.out.println(actionSpace.shortString());
            }
        }

        if (VERBOSE) {
            System.out.println();
        }
        return actionSpaceOnePerRep;
    }

    /**
     * General game tests, available after setup:
     * - State size: size of a game state, i.e. number of components.
     * - Hidden information: Amount of hidden information in the game, i.e. number of hidden components
     * @param game - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static void generalTest(GameType game, int nPlayers) {
        System.out.println("--------------------\nGeneral Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");

        Game g = game.createGameInstance(nPlayers);
        if (g != null) {
            AbstractGameState gs = g.getGameState();
            System.out.println("State size: " + gs.getAllComponents().size());

            StatSummary ss = new StatSummary();
            for (int i = 0; i < nPlayers; i++) {
                ss.add(gs.getUnknownComponentsIds(i).size());
            }
            System.out.println("Hidden information: " + ss.shortString());
        }

        System.out.println();
    }

    /**
     * How fast the game works.
     *  - ForwardModel.setup()
     *  - ForwardModel.next()
     *  - ForwardModel.computeAvailableActions()
     *  - GameState.copy()
     * @param game - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static void gameSpeed(GameType game, int nPlayers) {
        System.out.println("--------------------\nSpeed Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");

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
     * @param nPlayers - number of players taking part in this test.
     */
    public static void gameLength(GameType game, int nPlayers) {
        System.out.println("--------------------\nGame Length Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");

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
     * Several tests involving player playing and gathering statistics about their observations:
     *  - Reward sparsity: How sparse is the reward signal in the scoring function? Calculates granularity of possible
     *  values between -1 and 1.
     *  - Branching factor: Number of distinct states that can be reached from any one game state.
     * @param game - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static void playerObservationTest(GameType game, int nPlayers) {
        System.out.println("--------------------\nPlayer Observation Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");

        StatSummary rs = new StatSummary("Reward Sparsity");
        StatSummary bf = new StatSummary("Branching Factor");
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
                    rs.add(rtp.getScores());
                    bf.add(rtp.getBranchingFactor());
                }
            }
        }

        if (rs.n() != 0) {
            System.out.println(rs.toString());
            System.out.println(bf.toString());
        }

        System.out.println();
    }

    /* Helper methods */

    /**
     * Calculates and plots together all games for each number of players (if the game can support the given number).
     * @param games - games to plot.
     */
    public static void actionSpaceTestAllGames(ArrayList<GameType> games) {
        int min = GameType.getMinPlayersAllGames();
        int max = GameType.getMaxPlayersAllGames();

        for (int p = min; p <= max; p++) {

            LineChart lc = new LineChart("Action Space Size " + p + "p", "game tick", "action space size");
            lc.setVisible(false);
            BoxPlot bp = new BoxPlot("Action Space Size " + p + "p", "game", "action space size");
            bp.setVisible(false);

            for (GameType gt : games) {
                if (gt.getMinPlayers() > p && gt.getMaxPlayers() < p) continue;
                StatSummary ss = actionSpace(gt, p, lc);

                bp.addSeries(ss.getElements(), gt.name());
                lc.setVisible(true);
                bp.setVisible(true);
            }
        }
    }

    /**
     * Calculates and plots together all player numbers for each game, one plot per game.
     * @param games - games to plot.
     */
    public static void actionSpaceTestAllPlayers(ArrayList<GameType> games) {
        for (GameType gt: games) {

            LineChart lc = new LineChart("Action Space Size " + gt.name(), "game tick", "action space size");
            lc.setVisible(false);
            BoxPlot bp = new BoxPlot("Action Space Size " + gt.name(), "game", "action space size");
            bp.setVisible(false);

            for (int p = gt.getMinPlayers(); p <= gt.getMaxPlayers(); p++) {
                StatSummary ss = actionSpace(gt, p, lc);

                bp.addSeries(ss.getElements(), p + "p");
                lc.setVisible(true);
                bp.setVisible(true);
            }
        }
    }

    /**
     * Main method to run this class, with various options/examples given
     * @param args - program arguments, ignored
     */
    public static void main(String[] args) {

        // 1. Action space tests, plots per game with all player numbers, or per player number with all games
        ArrayList<GameType> games = new ArrayList<>(Arrays.asList(GameType.values()));
        games.remove(GameType.Uno);
        actionSpaceTestAllGames(games);
        actionSpaceTestAllPlayers(games);

        // 2. Run action space test on Pandemic with 2 players, plots only
//        LineChart lc = new LineChart("Action Space Size (Pandemic 2p)", "game tick", "action space size");
//        VERBOSE = false;
//        actionSpace(Pandemic, 2, lc);

        // 3. Run action space test on Pandemic with 2 players, printed report
//        VERBOSE = true;
//        actionSpace(Pandemic, 2, null);

        // 4. Run each test with printed reports on each game
//        int nPlayers = 2;
//        VERBOSE = true;
//        for (GameType gt: GameType.values()) {
//            actionSpace(gt, nPlayers, null);
//            generalTest(gt, nPlayers);
//            gameSpeed(gt, nPlayers);
//            gameLength(gt, nPlayers);
//            playerObservationTest(gt, nPlayers);
//        }
    }
}
