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
    public void stopNeedsRobustSelection() {
        MCTSParams params = params(PlayerConstants.BUDGET_GAME_SECONDS, 20);
        params.setParameterValue("selectionPolicy", SIMPLE);
        params.setParameterValue("earlyStopP", 0.5);
        new MCTSTimeManager(params);
    }

    @Test
    public void noTimeManagerByDefault() {
        MCTSPlayer player = new MCTSPlayer(params(PlayerConstants.BUDGET_ITERATIONS, 100));
        assertNull(player.getTimeManager());
        player.getParameters().setParameterValue("earlyStopP", 1.0);
        assertNotNull(player.getTimeManager());
    }

    /**
     * STOP_A only stops once the most-visited action can no longer be overtaken, so it must choose the same action
     * as the full search, from the same search prefix.
     */
    @Test
    public void stopANeverChangesTheActionChosen() {
        int stopped = 0;
        long fullIterations = 0, stopIterations = 0;
        for (AbstractGameState state : connect4States(60, 77)) {
            MCTSPlayer full = searchedPlayer(params(PlayerConstants.BUDGET_ITERATIONS, 1000), state);
            MCTSParams stopParams = params(PlayerConstants.BUDGET_ITERATIONS, 1000);
            stopParams.setParameterValue("earlyStopP", 1.0);
            MCTSPlayer stop = searchedPlayer(stopParams, state);

            assertEquals(full.getRoot().bestAction(), stop.getRoot().bestAction());
            assertEquals(1000, full.getRootVisits());
            assertTrue(stop.getRootVisits() <= 1000);
            if (stop.getTimeManager().stoppedEarly()) stopped++;
            fullIterations += full.getRootVisits();
            stopIterations += stop.getRootVisits();
        }
        assertTrue("stopped " + stopped, stopped > 10);
        assertTrue(stopIterations < fullIterations);
    }

    @Test
    public void lowerEarlyStopPStopsSooner() {
        long iterationsA = 0, iterations = 0;
        for (AbstractGameState state : connect4States(20, 5)) {
            MCTSParams stopA = params(PlayerConstants.BUDGET_ITERATIONS, 1000);
            stopA.setParameterValue("earlyStopP", 1.0);
            iterationsA += searchedPlayer(stopA, state).getRootVisits();
            MCTSParams stop = params(PlayerConstants.BUDGET_ITERATIONS, 1000);
            stop.setParameterValue("earlyStopP", 0.3);
            iterations += searchedPlayer(stop, state).getRootVisits();
        }
        assertTrue(iterations < iterationsA);
    }

    private static int iterationsWith(String loops, String other, Object otherValue, double factor, int nLoops) {
        MCTSParams params = params(PlayerConstants.BUDGET_ITERATIONS, 400);
        params.setParameterValue(loops, nLoops);
        params.setParameterValue(other, otherValue);
        params.setParameterValue(loops.replace("Loops", "Factor"), factor);
        return searchedPlayer(params, connect4States(1, 3).get(0)).getRootVisits();
    }

    @Test
    public void extensionsAddFactorTimesPlannedBudgetPerLoop() {
        // BEHIND with a threshold above any win rate, and CLOSE with one above any relative visit gap, always extend
        assertEquals(400 + 3 * 200, iterationsWith("behindLoops", "behindThreshold", 1.1, 0.5, 3));
        assertEquals(400 + 2 * 400, iterationsWith("closeLoops", "closeThreshold", 1.01, 1.0, 2));
        // and with a threshold of zero they never do
        assertEquals(400, iterationsWith("behindLoops", "behindThreshold", 0.0, 0.5, 3));
        assertEquals(400, iterationsWith("closeLoops", "closeThreshold", 0.0, 1.0, 2));
    }

    @Test
    public void unstableExtendsOnlyWhileMostVisitedIsNotHighestValued() {
        for (AbstractGameState state : connect4States(10, 11)) {
            MCTSParams params = params(PlayerConstants.BUDGET_ITERATIONS, 300);
            params.setParameterValue("unstableLoops", 2);
            params.setParameterValue("unstableFactor", 0.5);
            MCTSPlayer player = searchedPlayer(params, state);
            int visits = player.getRootVisits();
            int extensions = player.getTimeManager().extensions();
            assertEquals(300 + extensions * 150, visits);
            if (extensions < 2) {
                SingleTreeNode root = player.getRoot();
                List<AbstractAction> actions = root.rootActionsToConsider();
                AbstractAction mostVisited = root.mostVisitedAction(actions);
                for (AbstractAction a : actions)
                    assertTrue(root.meanActionValue(a) <= root.meanActionValue(mostVisited) || Double.isNaN(root.meanActionValue(a)));
            }
        }
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
