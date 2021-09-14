package evaluation;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import evaluation.testplayers.RandomTestPlayer;
import games.GameType;
import players.simple.RandomPlayer;
import utilities.BoxPlot;
import utilities.LineChart;
import utilities.Pair;
import utilities.TAGStatSummary;

import java.util.ArrayList;
import java.util.List;

import static games.GameType.*;

public class GameReport {

    static boolean VERBOSE = true;
    static int nRep = 1000;

    /**
     * Number of actions available from any one game state.
     *
     * @param game     - game to test.
     * @param nPlayers - number of players taking part in this test.
     * @param lc       - line chart to add this data to, can be null if only printing required
     */
    public static TAGStatSummary actionSpace(GameType game, int nPlayers, LineChart lc) {
        if (VERBOSE) {
            System.out.println("--------------------\nAction Space Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");
        }

        TAGStatSummary actionSpace = new TAGStatSummary(game.name() + "-" + nPlayers + "p");
        TAGStatSummary actionSpaceOnePerRep = new TAGStatSummary(game.name() + "-" + nPlayers + "p");
        ArrayList<TAGStatSummary> sumData = new ArrayList<>();

        for (int i = 0; i < nRep; i++) {
            TAGStatSummary ss = new TAGStatSummary();

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
                        sumData.add(new TAGStatSummary(nPlayers + "-" + j));
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
     *
     * @param game     - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static Pair<Integer, TAGStatSummary> generalTest(GameType game, int nPlayers) {
        if (VERBOSE) {
            System.out.println("--------------------\nGeneral Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");
        }

        Game g = game.createGameInstance(nPlayers);
        Pair<Integer, TAGStatSummary> ret = null;
        if (g != null) {
            AbstractGameState gs = g.getGameState();
            if (VERBOSE) {
                System.out.println("State size: " + gs.getAllComponents().size());
            }

            // TODO: run games
            TAGStatSummary ss = new TAGStatSummary();
            for (int i = 0; i < nPlayers; i++) {
                ss.add(gs.getUnknownComponentsIds(i).size());
            }
            if (VERBOSE) {
                System.out.println("Hidden information: " + ss.shortString());
            }

            ret = new Pair<>(gs.getAllComponents().size(), ss);
        }

        if (VERBOSE) {
            System.out.println();
        }
        return ret;
    }

    /**
     * How fast the game works.
     * - ForwardModel.setup()
     * - ForwardModel.next()
     * - ForwardModel.computeAvailableActions()
     * - GameState.copy()
     *
     * @param game     - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static TAGStatSummary[] gameSpeed(GameType game, int nPlayers) {
        if (VERBOSE) {
            System.out.println("--------------------\nSpeed Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");
        }

        TAGStatSummary[] ret = new TAGStatSummary[4];
        TAGStatSummary nextT = new TAGStatSummary();
        ret[0] = nextT;
        TAGStatSummary copyT = new TAGStatSummary();
        ret[1] = copyT;
        TAGStatSummary actionT = new TAGStatSummary();
        ret[2] = actionT;
        TAGStatSummary setupT = new TAGStatSummary();
        ret[3] = setupT;

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
                setupT.add(1e+9 / (System.nanoTime() - s));

                // Run timers
                g.reset(players);
                g.run();
                nextT.add(1e+9 / g.getNextTime());
                copyT.add(1e+9 / g.getCopyTime());
                actionT.add(1e+9 / g.getActionComputeTime());

            }
        }

        if (nextT.n() != 0 && VERBOSE) {
            System.out.println("GS.copy(): " + String.format("%6.3e", (copyT.mean())) + " executions/second");
            System.out.println("FM.setup(): " + String.format("%6.3e", (setupT.mean())) + " executions/second");
            System.out.println("FM.next(): " + String.format("%6.3e", (nextT.mean())) + " executions/second");
            System.out.println("FM.computeAvailableActions(): " + String.format("%6.3e", (actionT.mean())) + " executions/second");
        }

        if (VERBOSE) {
            System.out.println();
        }
        return ret;
    }

    /**
     * How many decisions players take in a game from beginning to end. Alternatively, number of rounds.
     *
     * @param game     - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static TAGStatSummary[] gameLength(GameType game, int nPlayers) {
        if (VERBOSE) {
            System.out.println("--------------------\nGame Length Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");
        }

        TAGStatSummary[] ret = new TAGStatSummary[4];
        TAGStatSummary nDecisions = new TAGStatSummary();
        TAGStatSummary nTicks = new TAGStatSummary();
        TAGStatSummary nRounds = new TAGStatSummary();
        TAGStatSummary nActionsPerTurn = new TAGStatSummary();
        ret[0] = nDecisions;
        ret[1] = nTicks;
        ret[2] = nRounds;
        ret[3] = nActionsPerTurn;

        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers; j++) {
                players.add(new RandomPlayer());
            }

            if (g != null) {
                g.reset(players);
                g.run();
                nDecisions.add(g.getNDecisions());
                nTicks.add(g.getTick());
                nRounds.add(g.getGameState().getTurnOrder().getRoundCounter());
                nActionsPerTurn.add(g.getNActionsPerTurn());
            }
        }

        if (nDecisions.n() != 0) {
            if (VERBOSE) {
                System.out.println("# decisions: " + (nDecisions.mean()));
                System.out.println("# ticks: " + (nTicks.mean()));
                System.out.println("# rounds: " + (nRounds.mean()));
                System.out.println("# actions/turn: " + (nActionsPerTurn.mean()));
            }
        }

        if (VERBOSE) {
            System.out.println();
        }
        return ret;
    }

    /**
     * Several tests involving player playing and gathering statistics about their observations:
     * - Reward sparsity: How sparse is the reward signal in the scoring function? Calculates granularity of possible
     * values between -1 and 1.
     * - Branching factor: Number of distinct states that can be reached from any one game state.
     *
     * @param game     - game to test.
     * @param nPlayers - number of players taking part in this test.
     */
    public static Pair<TAGStatSummary, TAGStatSummary> playerObservationTest(GameType game, int nPlayers) {
        if (VERBOSE) {
            System.out.println("--------------------\nPlayer Observation Test: " + game.name() + " [" + nPlayers + " players]\n--------------------");
        }

        TAGStatSummary rs = new TAGStatSummary("Reward Sparsity");
        TAGStatSummary bf = new TAGStatSummary("Branching Factor");
        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers; j++) {
                players.add(new RandomTestPlayer());
            }

            if (g != null) {
                g.reset(players);
                g.run();

                for (AbstractPlayer p : players) {
                    RandomTestPlayer rtp = (RandomTestPlayer) p;
                    rs.add(rtp.getScores());
                    bf.add(rtp.getBranchingFactor());
                }
            }
        }

        if (rs.n() != 0) {
            if (VERBOSE) {
                System.out.println(rs.toString());
                System.out.println(bf.toString());
            }
        }

        if (VERBOSE) {
            System.out.println();
        }
        return new Pair<>(rs, bf);
    }

    /* Helper methods */

    /**
     * Calculates and plots together all games for each number of players (if the game can support the given number).
     *
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
                TAGStatSummary ss = actionSpace(gt, p, lc);

                bp.addSeries(ss.getElements(), gt.name());
                lc.setVisible(true);
                bp.setVisible(true);
            }
        }
    }

    /**
     * Calculates and plots together all player numbers for each game, one plot per game.
     *
     * @param games - games to plot.
     */
    public static void actionSpaceTestAllPlayers(ArrayList<GameType> games) {
        for (GameType gt : games) {

            LineChart lc = new LineChart("Action Space Size " + gt.name(), "game tick", "action space size");
            lc.setVisible(false);
            BoxPlot bp = new BoxPlot("Action Space Size " + gt.name(), "game", "action space size");
            bp.setVisible(false);

            for (int p = gt.getMinPlayers(); p <= gt.getMaxPlayers(); p++) {
                TAGStatSummary ss = actionSpace(gt, p, lc);

                bp.addSeries(ss.getElements(), p + "p");
                lc.setVisible(true);
                bp.setVisible(true);
            }
        }
    }

    /**
     * Main method to run this class, with various options/examples given
     *
     * @param args - program arguments, ignored
     */
    public static void main(String[] args) {

        // 1. Action space tests, plots per game with all player numbers, or per player number with all games
//        ArrayList<GameType> games = new ArrayList<>(Arrays.asList(GameType.values()));
//        games.remove(GameType.Uno);
//        actionSpaceTestAllGames(games);
//        actionSpaceTestAllPlayers(games);

        // 2. Run action space test on Pandemic with 2 players, plots only
//        LineChart lc = new LineChart("Action Space Size (Pandemic 2p)", "game tick", "action space size");
//        VERBOSE = false;
//        actionSpace(Pandemic, 2, lc);

        // 3. Run action space test on Pandemic with 2 players, printed report
//        VERBOSE = true;
//        actionSpace(Pandemic, 2, null);

        // 4. Run each test with printed reports on each game
//        int nPlayers = 2;
        VERBOSE = false;
        for (GameType gt : GameType.values()) {
            if (gt == Pandemic) continue;
            if (gt == Virus) continue;
            if (gt == Uno) continue;
            if (gt == ExplodingKittens) continue;
            if (gt == LoveLetter) continue;
            if (gt == TicTacToe) continue;
            if (gt == ColtExpress) continue;
            //     if (gt == Dominion) continue;

            TAGStatSummary as = new TAGStatSummary("Action space size (" + gt.name() + ")");
            TAGStatSummary ss = new TAGStatSummary("State size (" + gt.name() + ")");
            TAGStatSummary hidi = new TAGStatSummary("Hidden info size (" + gt.name() + ")");
            TAGStatSummary cp = new TAGStatSummary("GS.copy() (" + gt.name() + ") exe/sec");
            TAGStatSummary sp = new TAGStatSummary("FM.setup() (" + gt.name() + ") exe/sec");
            TAGStatSummary ne = new TAGStatSummary("FM.next() (" + gt.name() + ") exe/sec");
            TAGStatSummary ac = new TAGStatSummary("FM.actions() (" + gt.name() + ") exe/sec");
            TAGStatSummary nd = new TAGStatSummary("#decisions (" + gt.name() + ")");
            TAGStatSummary nt = new TAGStatSummary("#ticks (" + gt.name() + ")");
            TAGStatSummary nr = new TAGStatSummary("#rounds (" + gt.name() + ")");
            TAGStatSummary napt = new TAGStatSummary("#apt (" + gt.name() + ")");
            TAGStatSummary rs = new TAGStatSummary("Reward Sparsity (" + gt.name() + ")");
            TAGStatSummary bf = new TAGStatSummary("Branching Factor (" + gt.name() + ")");

            for (int i = gt.getMinPlayers(); i <= gt.getMaxPlayers(); i++) {
                System.out.println(gt.name() + " " + i);
                as.add(actionSpace(gt, i, null));

                Pair<Integer, TAGStatSummary> ret = generalTest(gt, i);
                ss.add(ret.a);
                hidi.add(ret.b);

                TAGStatSummary[] rett = gameSpeed(gt, i);
                cp.add(rett[0]);
                sp.add(rett[1]);
                ne.add(rett[2]);
                ac.add(rett[3]);

                TAGStatSummary[] rettt = gameLength(gt, i);
                nd.add(rettt[0]);
                nt.add(rettt[1]);
                nr.add(rettt[2]);
                napt.add(rettt[3]);

                Pair<TAGStatSummary, TAGStatSummary> retttt = playerObservationTest(gt, i);
                rs.add(retttt.a);
                bf.add(retttt.b);
            }

            System.out.println(as.shortString());
            System.out.println(ss.shortString());
            System.out.println(hidi.shortString());
            System.out.println("Hidden info perc: " + (hidi.mean() * 100.0 / ss.mean()));
            System.out.println(cp.shortString(true));
            System.out.println(sp.shortString(true));
            System.out.println(ne.shortString(true));
            System.out.println(ac.shortString(true));
            System.out.println(nd.shortString());
            System.out.println(nt.shortString());
            System.out.println(nr.shortString());
            System.out.println(napt.shortString());
            System.out.println(rs.shortString());
            System.out.println(bf.shortString());
        }
    }
}
