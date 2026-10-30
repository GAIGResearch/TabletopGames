package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuChessTimer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.SelectionPolicy.SIMPLE;

public class MCTSTimeManagerTest {

    private static MCTSParams params(PlayerConstants budgetType, int budget) {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", budgetType);
        params.setParameterValue("budget", budget);
        params.setParameterValue("selectionPolicy", ROBUST);
        params.setParameterValue("treePolicy", MCTSEnums.TreePolicy.UCB_Tuned);
        params.setParameterValue("K", 1.3);
        params.setParameterValue("information", MCTSEnums.Information.Open_Loop);
        params.setParameterValue("randomSeed", 1234);
        return params;
    }

    private static MCTSTimeManager manager(MCTSParams params) {
        MCTSTimeManager tm = new MCTSTimeManager(params);
        tm.newGame(GameType.TicTacToe.createGameInstance(2, 1).getGameState());
        return tm;
    }

    /**
     * Connect4 states after a number of random moves, none of them terminal
     */
    private static List<AbstractGameState> connect4States(int n, long seed) {
        Game game = GameType.Connect4.createGameInstance(2, seed);
        AbstractForwardModel fm = game.getForwardModel();
        Random rnd = new Random(seed);
        List<AbstractGameState> states = new ArrayList<>();
        while (states.size() < n) {
            AbstractGameState state = game.getGameState().copy();
            fm.setup(state);
            int moves = rnd.nextInt(20);
            for (int i = 0; i < moves && state.isNotTerminal(); i++) {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
            }
            if (state.isNotTerminal())
                states.add(state);
        }
        return states;
    }

    private static MCTSPlayer searchedPlayer(MCTSParams params, AbstractGameState state) {
        MCTSPlayer player = new MCTSPlayer(params);
        player.setForwardModel(GameType.Connect4.createGameInstance(2, 1).getForwardModel());
        player.setPlayerID(state.getCurrentPlayer());
        player.initializePlayer(state);
        List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(state);
        player.getAction(state.copy(), actions);
        return player;
    }

    @Test
    public void expDividesRemainingTimeByExpectedDecisions() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        MCTSTimeManager tm = manager(params);
        // the reserve, and the minimum search for each of the other 29 decisions, are not shared out
        assertEquals((20_000 - 50 - 29 * 10) / 30, tm.allocate(20_000));

        params.setParameterValue("expectedDecisions", 10.0);
        params.setParameterValue("timeFactor", 2.0);
        assertEquals(2 * (20_000 - 50 - 9 * 10) / 10, manager(params).allocate(20_000));
    }

    @Test
    public void allocationIsBoundedByClockAndMinimum() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("expectedDecisions", 0.5);  // treated as 1
        params.setParameterValue("timeFactor", 5.0);
        MCTSTimeManager tm = manager(params);
        assertEquals(1000 - 50, tm.allocate(1000));
        assertEquals(100 - 50, tm.allocate(100));
        // below the reserve, there is no minimum search
        assertEquals(5, tm.allocate(55));
        assertEquals(0, tm.allocate(30));
        assertEquals(0, tm.allocate(-500));
    }

    @Test
    public void midAddsGaussianBonusByDecisionNumber() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("expectedDecisions", 10.0);
        params.setParameterValue("midHeight", 2.0);
        params.setParameterValue("midCentre", 3.0);
        params.setParameterValue("midWidth", 1.0);
        MCTSTimeManager tm = manager(params);
        // decision 1 is two widths from the centre
        assertEquals((long) (996 * (1 + 2 * Math.exp(-2))), tm.allocate(10_100));
        tm.endDecision(0);
        tm.endDecision(0);
        // decision 3 is at the centre
        assertEquals(3 * 996, tm.allocate(10_100));
    }

    @Test
    public void decisionTableIsIndexedByDecisionsMade() throws IOException {
        Path file = Files.createTempFile("decisions", ".txt");
        Files.writeString(file, "20, 19.5\n4\n2.5");
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("decisionTable", file.toString());
        MCTSTimeManager tm = manager(params);
        double[] expected = {20, 19.5, 4, 2.5, 2.5};
        for (double m : expected) {
            assertEquals(m, tm.expectedRemainingDecisions(), 1e-9);
            tm.endDecision(0);
        }
        tm.newGame(GameType.TicTacToe.createGameInstance(2, 1).getGameState());
        assertEquals(20, tm.expectedRemainingDecisions(), 1e-9);
        Files.delete(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stopNeedsMostVisitedOrHighestValuedSelection() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("treePolicy", MCTSEnums.TreePolicy.EXP3);
        params.setParameterValue("earlyStopP", 0.5);
        new MCTSTimeManager(params);
    }

    @Test
    public void stopAndCloseAllowSimpleSelection() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("selectionPolicy", SIMPLE);
        params.setParameterValue("earlyStopP", 0.5);
        params.setParameterValue("closeLoops", 1);
        new MCTSTimeManager(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void confidenceMustBeBelowOne() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("stopConfidence", 1.0);
        new MCTSTimeManager(params);
    }

    @Test
    public void timeManagerOnlyWithClockBudget() {
        MCTSPlayer player = new MCTSPlayer(params(PlayerConstants.BUDGET_ITERATIONS, 100));
        player.getParameters().setParameterValue("earlyStopP", 1.0);
        assertNull(player.getTimeManager());
        assertNotNull(new MCTSPlayer(params(PlayerConstants.BUDGET_GAME_SECONDS, 20)).getTimeManager());
        assertNotNull(new MCTSPlayer(params(PlayerConstants.BUDGET_ROUND_SECONDS, 20)).getTimeManager());
    }

    @Test(expected = IllegalArgumentException.class)
    public void timeManagementNeedsClockBudget() {
        new MCTSTimeManager(params(PlayerConstants.BUDGET_ITERATIONS, 100));
    }

    /**
     * The dynamic strategies are tested on trees searched with a fixed number of iterations (so that they are
     * deterministic), with a time manager for a game clock that is part way through a decision
     */
    private static MCTSTimeManager clockManager(Object... settings) {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 100);
        params.setParameterValue("decisionTable", "");
        params.setParameterValue("expectedDecisions", 10.0);
        for (int i = 0; i < settings.length; i += 2)
            params.setParameterValue((String) settings[i], settings[i + 1]);
        MCTSTimeManager tm = manager(params);
        tm.startDecision(connect4States(1, 1).get(0), new ElapsedCpuChessTimer(100_000, 0, 0, 0, 0));
        return tm;
    }

    private static SingleTreeNode searchedTree(int iterations, AbstractGameState state) {
        return searchedTree(ROBUST, iterations, state);
    }

    private static SingleTreeNode searchedTree(MCTSEnums.SelectionPolicy policy, int iterations, AbstractGameState state) {
        MCTSParams params = params(PlayerConstants.BUDGET_ITERATIONS, iterations);
        params.setParameterValue("selectionPolicy", policy);
        return searchedPlayer(params, state).getRoot();
    }

    /**
     * STOP_A only stops once the most-visited action can no longer be overtaken, so it must choose the same action
     * as the full search, from the same search prefix.
     */
    @Test
    public void stopANeverChangesTheActionChosen() {
        MCTSTimeManager tm = clockManager("earlyStopP", 1.0);
        int stopped = 0;
        for (AbstractGameState state : connect4States(30, 77)) {
            AbstractAction full = searchedTree(1000, state).bestAction();
            for (int prefix = 100; prefix < 1000; prefix += 100) {
                SingleTreeNode root = searchedTree(prefix, state);
                if (tm.bestActionSettled(root, 1000 - prefix)) {
                    assertEquals(full, root.bestAction());
                    stopped++;
                    break;
                }
            }
        }
        assertTrue("stopped " + stopped, stopped > 5);
    }

    @Test
    public void lowerEarlyStopPStopsSooner() {
        MCTSTimeManager stopA = clockManager("earlyStopP", 1.0);
        MCTSTimeManager stop = clockManager("earlyStopP", 0.3);
        int settledA = 0, settled = 0;
        for (AbstractGameState state : connect4States(10, 5)) {
            for (int prefix = 100; prefix < 1000; prefix += 200) {
                SingleTreeNode root = searchedTree(prefix, state);
                boolean a = stopA.bestActionSettled(root, 1000 - prefix);
                boolean b = stop.bestActionSettled(root, 1000 - prefix);
                assertTrue(!a || b);
                if (a) settledA++;
                if (b) settled++;
            }
        }
        assertTrue(settled + " against " + settledA, settled > settledA);
    }

    private static int extensionsWith(String loops, String threshold, double thresholdValue, double factor, int nLoops) {
        MCTSTimeManager tm = clockManager(loops, nLoops, threshold, thresholdValue,
                loops.replace("Loops", "Factor"), factor);
        SingleTreeNode root = searchedTree(400, connect4States(1, 3).get(0));
        long expected = (long) (factor * tm.plannedBudget());
        int extensions = 0;
        for (long extension = tm.extension(root); extension > 0; extension = tm.extension(root)) {
            assertEquals(expected, extension);
            extensions++;
        }
        assertEquals(extensions, tm.extensions());
        return extensions;
    }

    @Test
    public void extensionsAddFactorTimesPlannedBudgetPerLoop() {
        // BEHIND with a threshold above any win rate, and CLOSE with one above any relative visit gap, always extend
        assertEquals(3, extensionsWith("behindLoops", "behindThreshold", 1.1, 0.5, 3));
        assertEquals(2, extensionsWith("closeLoops", "closeThreshold", 1.01, 1.0, 2));
        // and with a threshold of zero they never do
        assertEquals(0, extensionsWith("behindLoops", "behindThreshold", 0.0, 0.5, 3));
        assertEquals(0, extensionsWith("closeLoops", "closeThreshold", 0.0, 1.0, 2));
    }

    @Test
    public void unstableExtendsOnlyWhileMostVisitedIsNotHighestValued() {
        int unstable = 0;
        for (AbstractGameState state : connect4States(20, 11)) {
            MCTSTimeManager tm = clockManager("unstableLoops", 2, "unstableFactor", 0.5);
            SingleTreeNode root = searchedTree(300, state);
            List<AbstractAction> actions = MCTSTimeManager.rootActions(root);
            boolean expected = !MCTSTimeManager.mostVisited(root, actions)
                    .equals(MCTSTimeManager.highestValued(root, actions));
            assertEquals(expected, tm.extension(root) > 0);
            if (expected) unstable++;
        }
        assertTrue("unstable " + unstable, unstable > 0);
    }

    /**
     * The extensions and early stops are made during a search with a clock budget
     */
    @Test
    public void dynamicStrategiesActDuringAClockSearch() {
        AbstractGameState state = connect4States(1, 3).get(0);
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 2);
        params.setParameterValue("decisionTable", "");
        params.setParameterValue("expectedDecisions", 10.0);
        params.setParameterValue("behindLoops", 2);
        params.setParameterValue("behindThreshold", 1.1);
        MCTSPlayer extended = searchedPlayer(params, state);
        assertEquals(2, extended.getTimeManager().extensions());
        assertTrue(extended.getRoot().timeTaken > extended.getTimeManager().plannedBudget());

        params = params(PlayerConstants.BUDGET_GAME_SECONDS, 2);
        params.setParameterValue("decisionTable", "");
        params.setParameterValue("expectedDecisions", 10.0);
        params.setParameterValue("earlyStopP", 0.05);
        MCTSPlayer stopped = searchedPlayer(params, state);
        assertTrue(stopped.getTimeManager().stoppedEarly());
        assertTrue(stopped.getRoot().timeTaken < stopped.getTimeManager().plannedBudget());

        params.setParameterValue("selectionPolicy", SIMPLE);
        params.setParameterValue("stopConfidence", 0.6);
        MCTSPlayer stoppedSimple = searchedPlayer(params, state);
        assertTrue(stoppedSimple.getTimeManager().stoppedEarly());
        assertTrue(stoppedSimple.getRoot().timeTaken < stoppedSimple.getTimeManager().plannedBudget());
    }

    /**
     * The root of a searched Connect4 tree, with the statistics of its seven actions then set by hand: each is
     * {visits, mean, variance}, and the first is the highest-valued
     */
    private static SingleTreeNode treeWithStats(double[]... stats) {
        SingleTreeNode root = searchedTree(SIMPLE, 50, connect4States(1, 3).get(0));
        List<AbstractAction> actions = MCTSTimeManager.rootActions(root);
        assertEquals(7, actions.size());
        for (int i = 0; i < actions.size(); i++) {
            double[] s = stats[Math.min(i, stats.length - 1)];
            ActionStats as = root.getActionStats(actions.get(i));
            int n = (int) s[0];
            as.nVisits = n;
            as.totValue[root.decisionPlayer] = n * s[1];
            as.squaredTotValue[root.decisionPlayer] = n * (s[2] + s[1] * s[1]);
        }
        return root;
    }

    private static boolean settled(SingleTreeNode root, double horizon, double confidence) {
        return MCTSTimeManager.leaderSettled(root, MCTSTimeManager.rootActions(root), horizon, confidence);
    }

    @Test
    public void meanVarianceShrinksWithShorterHorizon() {
        SingleTreeNode root = treeWithStats(new double[]{100, 0.5, 0.25});
        AbstractAction action = MCTSTimeManager.rootActions(root).get(0);
        // s^2 / n with an infinite horizon; s^2.r / (n.(n + r)) with r = 100 of the 700 more iterations
        assertEquals(0.0025, MCTSTimeManager.meanVariance(root, action, Double.POSITIVE_INFINITY, 700), 1e-12);
        assertEquals(0.00125, MCTSTimeManager.meanVariance(root, action, 700, 700), 1e-12);
        assertEquals(0.0, MCTSTimeManager.meanVariance(root, action, 0, 700), 1e-12);
    }

    @Test
    public void clearLeaderIsSettledAndNearTieIsNot() {
        // a gap of 0.5 is about 7 standard errors, and one of 0.02 is about 0.3
        assertTrue(settled(treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.0, 0.25}),
                Double.POSITIVE_INFINITY, 0.95));
        assertFalse(settled(treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.48, 0.25}),
                Double.POSITIVE_INFINITY, 0.95));
        // and one close challenger is enough to unsettle a leader that is well clear of the others
        assertFalse(settled(treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.48, 0.25},
                new double[]{100, 0.0, 0.25}), Double.POSITIVE_INFINITY, 0.95));
    }

    @Test
    public void higherConfidenceNeedsBiggerGap() {
        // a gap of 0.25 is about 3.5 standard errors: enough for 95% over six comparisons, but not for 99.99%
        SingleTreeNode root = treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.25, 0.25});
        assertTrue(settled(root, Double.POSITIVE_INFINITY, 0.95));
        assertFalse(settled(root, Double.POSITIVE_INFINITY, 0.9999));
    }

    @Test
    public void shorterHorizonSettlesSooner() {
        // a gap of 0.1 is 1.4 standard errors of the true means, but 4.7 of the change in 10 more visits each
        SingleTreeNode root = treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.4, 0.25});
        assertFalse(settled(root, Double.POSITIVE_INFINITY, 0.95));
        assertFalse(settled(root, 7000, 0.95));
        assertTrue(settled(root, 70, 0.95));
        assertTrue(settled(root, 0, 0.95));
    }

    @Test
    public void unvisitedActionsAreNotConsidered() {
        // the near-tie is unvisited (with its value left in place)
        SingleTreeNode root = treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.0, 0.25});
        AbstractAction action = MCTSTimeManager.rootActions(root).get(1);
        root.getActionStats(action).totValue[root.decisionPlayer] = 0.0;
        root.getActionStats(action).nVisits = 0;
        assertTrue(settled(root, Double.POSITIVE_INFINITY, 0.95));
        // and a leader with no visited challenger is settled
        assertTrue(settled(treeWithStats(new double[]{10, 0.5, 0.25}, new double[]{0, 0.0, 0.0}),
                Double.POSITIVE_INFINITY, 0.95));
    }

    @Test
    public void zeroVarianceIsSettledOnlyIfStrictlyAhead() {
        assertTrue(settled(treeWithStats(new double[]{1, 1.0, 0.0}, new double[]{1, -1.0, 0.0}),
                Double.POSITIVE_INFINITY, 0.95));
        assertFalse(settled(treeWithStats(new double[]{1, 1.0, 0.0}), Double.POSITIVE_INFINITY, 0.95));
        // the variance of one action is enough to make it a test
        assertFalse(settled(treeWithStats(new double[]{1, 1.0, 0.0}, new double[]{2, 0.0, 1.0},
                new double[]{1, -1.0, 0.0}), Double.POSITIVE_INFINITY, 0.95));
    }

    @Test
    public void simpleCloseExtendsWhileLeaderIsNotConfidentlyBest() {
        MCTSTimeManager tm = clockManager("selectionPolicy", SIMPLE, "closeLoops", 1, "closeConfidence", 0.95);
        assertTrue(tm.extension(treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.48, 0.25})) > 0);
        tm = clockManager("selectionPolicy", SIMPLE, "closeLoops", 1, "closeConfidence", 0.95);
        assertEquals(0, tm.extension(treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.0, 0.25})));
        // the visit counts are equal, so ROBUST CLOSE extends for both
        tm = clockManager("closeLoops", 1, "closeThreshold", 0.5);
        assertTrue(tm.extension(treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.0, 0.25})) > 0);
    }

    @Test
    public void simpleStopUsesStopConfidenceOverEarlyStopPOfRemainingIterations() {
        SingleTreeNode root = treeWithStats(new double[]{100, 0.5, 0.25}, new double[]{100, 0.4, 0.25});
        assertTrue(clockManager("selectionPolicy", SIMPLE, "earlyStopP", 0.1).bestActionSettled(root, 700));
        assertFalse(clockManager("selectionPolicy", SIMPLE, "earlyStopP", 1.0).bestActionSettled(root, 7000));
        assertFalse(clockManager("selectionPolicy", SIMPLE, "earlyStopP", 0.1, "stopConfidence", 0.999999999)
                .bestActionSettled(root, 700));
    }

    /**
     * There is no guarantee for SIMPLE as there is for STOP_A, but with stopConfidence of 0.95 the action chosen
     * where the search stops should nearly always be the one the full search would choose
     */
    @Test
    public void simpleStopRarelyChangesTheActionChosen() {
        MCTSTimeManager tm = clockManager("selectionPolicy", SIMPLE, "earlyStopP", 1.0);
        int stopped = 0, changed = 0;
        for (AbstractGameState state : connect4States(30, 77)) {
            AbstractAction full = searchedTree(SIMPLE, 1000, state).bestAction();
            for (int prefix = 100; prefix < 1000; prefix += 100) {
                SingleTreeNode root = searchedTree(SIMPLE, prefix, state);
                if (tm.bestActionSettled(root, 1000 - prefix)) {
                    stopped++;
                    if (!full.equals(root.bestAction())) changed++;
                    break;
                }
            }
        }
        assertTrue("stopped " + stopped, stopped > 5);
        assertTrue("changed " + changed + " of " + stopped, changed <= stopped / 10);
    }

    @Test
    public void lowerEarlyStopPSettlesSoonerWithSimple() {
        MCTSTimeManager full = clockManager("selectionPolicy", SIMPLE, "earlyStopP", 1.0);
        MCTSTimeManager stop = clockManager("selectionPolicy", SIMPLE, "earlyStopP", 0.3);
        int settledFull = 0, settled = 0;
        for (AbstractGameState state : connect4States(10, 5)) {
            for (int prefix = 100; prefix < 1000; prefix += 200) {
                SingleTreeNode root = searchedTree(SIMPLE, prefix, state);
                boolean a = full.bestActionSettled(root, 1000 - prefix);
                boolean b = stop.bestActionSettled(root, 1000 - prefix);
                assertTrue(!a || b);
                if (a) settledFull++;
                if (b) settled++;
            }
        }
        assertTrue(settled + " against " + settledFull, settled > settledFull);
    }

    @Test
    public void gameClockIsRespectedAndSpent() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 2);
        params.setParameterValue("expectedDecisions", 10.0);
        params.setParameterValue("earlyStopP", 0.5);
        params.setParameterValue("timeFactor", 2.0);
        MCTSPlayer mcts = new MCTSPlayer(params);
        Game game = GameType.Connect4.createGameInstance(2, 42);
        game.reset(List.of(mcts, new RandomPlayer(new Random(3))));
        game.run();
        long remaining = mcts.getGameTimer().remainingTimeMillis();
        assertTrue("remaining " + remaining, remaining > 0);
        assertTrue("remaining " + remaining, remaining < 2_000);
        assertTrue(mcts.getTimeManager().decisionsMade() > 2);
    }

    /**
     * A Sushi Go state at the start of the given round, after random play
     */
    private static AbstractGameState sushiGoRound(int round) {
        Game game = GameType.SushiGo.createGameInstance(2, 9);
        AbstractForwardModel fm = game.getForwardModel();
        AbstractGameState state = game.getGameState();
        Random rnd = new Random(9);
        while (state.getRoundCounter() < round) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
        }
        return state;
    }

    private static int decisionsMadeAfterNewRound(PlayerConstants budgetType) {
        MCTSTimeManager tm = new MCTSTimeManager(params(budgetType, 20));
        AbstractGameState first = sushiGoRound(0);
        tm.newGame(first);
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(20_000, 0, 0, 0, 0);
        for (int i = 0; i < 2; i++) {
            tm.startDecision(first, timer);
            tm.endDecision(0);
        }
        tm.startDecision(sushiGoRound(1), timer);
        return tm.decisionsMade();
    }

    @Test
    public void roundBudgetCountsDecisionsFromTheStartOfTheRound() {
        assertEquals(0, decisionsMadeAfterNewRound(PlayerConstants.BUDGET_ROUND_SECONDS));
        assertEquals(2, decisionsMadeAfterNewRound(PlayerConstants.BUDGET_GAME_SECONDS));
    }

    @Test
    public void roundClockIsRespectedInEachRound() {
        MCTSParams params = params(PlayerConstants.BUDGET_ROUND_SECONDS, 1);
        params.setParameterValue("decisionTable", "");
        params.setParameterValue("expectedDecisions", 5.0);
        MCTSPlayer mcts = new MCTSPlayer(params);
        Game game = GameType.SushiGo.createGameInstance(2, 42);
        game.reset(List.of(mcts, new RandomPlayer(new Random(3))));
        game.run();
        // the clock is for the last round only; a round has about 9 decisions (more with chopsticks), and a game 27
        long remaining = mcts.getGameTimer().remainingTimeMillis();
        assertTrue("remaining " + remaining, remaining > 0);
        assertTrue("remaining " + remaining, remaining < 1_000);
        int decisions = mcts.getTimeManager().decisionsMade();
        assertTrue("decisions " + decisions, decisions > 2 && decisions < 15);
    }

    private static MCTSParams roundAllocationParams() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("roundAllocation", true);
        params.setParameterValue("decisionTable", "");
        params.setParameterValue("roundTable", "");
        params.setParameterValue("expectedDecisions", 5.0);
        params.setParameterValue("expectedRounds", 4.0);
        return params;
    }

    @Test
    public void roundAllocationSharesClockOverRoundsThenDecisionsInTheRound() {
        MCTSTimeManager tm = new MCTSTimeManager(roundAllocationParams());
        tm.newGame(sushiGoRound(0));
        // the minimum search of each of the 3 * 5 decisions in later rounds is kept back
        assertEquals((20_000 - 50 - 15 * 10) / 4, tm.allocateRound(20_000));

        tm.startDecision(sushiGoRound(1), new ElapsedCpuChessTimer(10_000, 0, 0, 0, 0));
        long roundBudget = (10_000 - 50 - 15 * 10) / 4;
        assertEquals(roundBudget, tm.roundBudget(), 2);
        assertEquals(roundBudget / 5, tm.plannedBudget(), 2);
        // after 1000 ms of the round, the rest of its allocation is shared out over the decisions left in it
        assertEquals((roundBudget - 1000) / 5, tm.allocate(10_000 - 1000), 2);
        // and a round that has used its allocation gets the minimum search
        assertEquals(10, tm.allocate(10_000 - roundBudget - 100), 2);
    }

    @Test
    public void roundTableIsIndexedByRoundsPlayed() throws IOException {
        Path file = Files.createTempFile("rounds", ".txt");
        Files.writeString(file, "6\n5.5\n1.2");
        MCTSParams params = roundAllocationParams();
        params.setParameterValue("roundTable", file.toString());
        MCTSTimeManager tm = new MCTSTimeManager(params);
        tm.newGame(sushiGoRound(0));
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(20_000, 0, 0, 0, 0);
        tm.startDecision(sushiGoRound(0), timer);
        assertEquals(6.0, tm.expectedRemainingRounds(), 1e-9);
        tm.startDecision(sushiGoRound(2), timer);
        assertEquals(1.2, tm.expectedRemainingRounds(), 1e-9);
        Files.delete(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void roundAllocationNeedsGameSeconds() {
        MCTSParams params = params(PlayerConstants.BUDGET_ROUND_SECONDS, 2);
        params.setParameterValue("roundAllocation", true);
        new MCTSTimeManager(params);
    }

    @Test
    public void roundAllocationKeepsToTheGameClock() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 2);
        params.setParameterValue("roundAllocation", true);
        MCTSPlayer mcts = new MCTSPlayer(params);
        Game game = GameType.LoveLetter.createGameInstance(2, 42);
        game.reset(List.of(mcts, new RandomPlayer(new Random(3))));
        game.run();
        long remaining = mcts.getGameTimer().remainingTimeMillis();
        assertTrue("remaining " + remaining, remaining > 0);
        assertTrue("remaining " + remaining, remaining < 2_000);
        assertTrue(game.getGameState().getRoundCounter() > 1);
    }
}
