package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.ElapsedCpuChessTimer;
import utilities.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.SelectionPolicy.SIMPLE;
import static players.mcts.MCTSEnums.TreePolicy.*;

/**
 * Time management for MCTS, from Baier and Winands, "Time Management for Monte Carlo Tree Search",
 * IEEE TCIAIG 8(3), 2016. Only the domain-independent strategies are implemented.
 * <p>
 * The semi-dynamic strategies set the time for each search from the player's game timer (BUDGET_GAME_SECONDS):
 * <ul>
 * <li>EXP: the time left divided by the expected number of decisions left (expectedDecisions, or decisionTable
 * for EXP-MOVES)</li>
 * <li>OPEN: EXP times timeFactor; this allocates more thinking time to the opening moves</li>
 * <li>MID: EXP with a bonus that peaks at decision midCentre (midHeight, midWidth)</li>
 * </ul>
 * The dynamic strategies act during the search. BEHIND, UNST and CLOSE continue the search for a multiple of the
 * planned budget, up to their number of loops:
 * <ul>
 * <li>BEHIND: while the win rate of the action that would be played is below behindThreshold</li>
 * <li>UNST: while the most-visited action is not the highest-valued</li>
 * <li>CLOSE: while the visits of the two most-visited actions differ by less than closeThreshold of the most-visited</li>
 * <li>STOP: stops the search once the most-visited action cannot be overtaken (relaxed by earlyStopP), with
 * timeFactor moving the time saved to earlier decisions. STOP_A is earlyStopP = 1 and timeFactor = 1; STOP_B is
 * earlyStopP = 1.</li>
 * </ul>
 * These are as in the paper for a ROBUST selectionPolicy. With SIMPLE, CLOSE and STOP instead test whether the
 * highest-valued action stays ahead (see leaderSettled()): CLOSE continues the search while we have less than
 * closeConfidence that it is truly the best, and STOP stops it once we have stopConfidence that it is still ahead
 * after earlyStopP times the remaining iterations.
 * <p>
 * Each allocation is made from the time left on the game timer, so an overrun is made up by later decisions.
 * With BUDGET_SECONDS this calculation is over the game as a whole.
 * With BUDGET_ROUND_SECONDS the timer is for the round, and each round is managed as a game of its own: decisions are
 * counted from the start of the round, and the decision table gives the decisions left in the round.
 * <p>
 * With roundAllocation (and BUDGET_GAME_SECONDS) the allocation has two levels. At the player's first decision in a
 * round, the round is given the time left on the game timer divided by the expected rounds left (expectedRounds, or
 * roundTable), times roundTimeFactor. Each decision is then given the time left in the round's allocation divided by
 * the expected decisions left in the round, as with BUDGET_ROUND_SECONDS. Time a round does not use stays on the game
 * timer, and is shared out over the later rounds.
 * <p>
 * The dynamic strategies will do nothing for any other budget type.
 */
public class MCTSTimeManager {

    private final MCTSParams params;
    public static final String DEFAULT_TABLE = "default";
    public static final String DEFAULT_TABLE_DIRECTORY = "data/timeManagement";
    private double[] decisionTable, roundTable;
    private ElapsedCpuChessTimer gameTimer;
    // in the game, or in the round with BUDGET_ROUND_SECONDS or roundAllocation
    private int decisionsMade;
    private int round;  // of the last decision
    // roundAllocation: the time allocated to the current round, and the game clock at its start (milliseconds)
    private long roundBudget, clockAtRoundStart;
    // moving average of the time charged to a decision outside its search (milliseconds)
    private double overheadMillis;

    // Statistics on the current (or last) decision
    private long plannedBudget;
    private long clockAtStart;
    private int behindDone, unstableDone, closeDone;
    private boolean stoppedEarly;

    public MCTSTimeManager(MCTSParams params) {
        this.params = params;
        validate(params);
        if (!params.decisionTable.isEmpty() && !params.decisionTable.equals(DEFAULT_TABLE))
            decisionTable = loadDecisionTable(params.decisionTable);
        if (!params.roundTable.isEmpty() && !params.roundTable.equals(DEFAULT_TABLE))
            roundTable = loadDecisionTable(params.roundTable);
    }

    private static void validate(MCTSParams params) {
        if (params.numDeterminizations > 1)
            throw new IllegalArgumentException("Time management is not supported with numDeterminizations > 1");
        if (!params.budgetType.isClock())
            throw new IllegalArgumentException("Time management needs a clock budget, not " + params.budgetType);
        if ((params.earlyStopP > 0.0 || params.closeLoops > 0) && (params.treePolicy == EXP3
                || params.treePolicy == RegretMatching || params.treePolicy == NoAveragingRM))
            throw new IllegalArgumentException("STOP and CLOSE need the most-visited (ROBUST) or highest-valued " +
                    "(SIMPLE) action to be played: use a treePolicy that is not EXP3 or regret matching");
        if (!(params.stopConfidence > 0.0 && params.stopConfidence < 1.0
                && params.closeConfidence > 0.0 && params.closeConfidence < 1.0))
            throw new IllegalArgumentException("stopConfidence and closeConfidence must be between 0 and 1");
        if (params.earlyStopInterval < 1)
            throw new IllegalArgumentException("earlyStopInterval must be at least 1");
        if (params.roundAllocation && params.budgetType != BUDGET_GAME_SECONDS)
            throw new IllegalArgumentException("roundAllocation needs BUDGET_GAME_SECONDS");
    }

    /**
     * The file holds the expected number of own decisions left (including the current one), indexed by the number
     * of decisions already made, separated by commas or white space.
     */
    public static double[] loadDecisionTable(String file) {
        try {
            String content = Files.readString(Path.of(file)).trim();
            double[] table = Arrays.stream(content.split("[,\\s]+")).mapToDouble(Double::parseDouble).toArray();
            if (table.length == 0)
                throw new IllegalArgumentException("Empty decision table in " + file);
            return table;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read decision table " + file, e);
        }
    }

    /**
     * Called at the start of each game.
     */
    public void newGame(AbstractGameState state) {
        decisionsMade = 0;
        round = -1;
        // The tables for the game and player count, if there are any; otherwise expectedDecisions and expectedRounds
        // are used
        if (params.decisionTable.equals(DEFAULT_TABLE))
            decisionTable = defaultTable(state, byRound() ? "_round" : "");
        if (params.roundTable.equals(DEFAULT_TABLE))
            roundTable = defaultTable(state, "_rounds");
    }

    private static double[] defaultTable(AbstractGameState state, String suffix) {
        Path file = Path.of(DEFAULT_TABLE_DIRECTORY,
                state.getGameType().name() + "_" + state.getNPlayers() + "p" + suffix + ".txt");
        return Files.exists(file) ? loadDecisionTable(file.toString()) : null;
    }

    /**
     * @return true if decisions are counted from the start of each round
     */
    private boolean byRound() {
        return params.budgetType == BUDGET_ROUND_SECONDS || params.roundAllocation;
    }

    /**
     * @return the number of decisions searched so far in this game (or round, with BUDGET_ROUND_SECONDS or
     * roundAllocation)
     */
    public int decisionsMade() {
        return decisionsMade;
    }

    /**
     * The expected number of own decisions left, including the current one
     */
    public double expectedRemainingDecisions() {
        double m;
        if (decisionTable == null)
            m = params.expectedDecisions;
        else
            m = decisionTable[Math.min(decisionsMade, decisionTable.length - 1)];
        return Math.max(1.0, m);
    }

    /**
     * The expected number of rounds left, including the current one (for roundAllocation)
     */
    public double expectedRemainingRounds() {
        double r;
        if (roundTable == null)
            r = params.expectedRounds;
        else
            r = roundTable[Math.min(Math.max(round, 0), roundTable.length - 1)];
        return Math.max(1.0, r);
    }

    /**
     * The expected number of own decisions left in the game after the current one
     */
    private double expectedLaterDecisions() {
        double later = expectedRemainingDecisions() - 1;
        if (params.roundAllocation)
            later += (expectedRemainingRounds() - 1) * decisionsPerRound();
        return later;
    }

    /**
     * The expected number of own decisions in a round (for roundAllocation)
     */
    private double decisionsPerRound() {
        return decisionTable == null ? params.expectedDecisions : decisionTable[0];
    }

    /**
     * Called before each search.
     *
     * @param timer - the player's game timer. It is running, and elapsedMillis() is the time spent on this
     *              decision so far.
     */
    public void startDecision(AbstractGameState state, ElapsedCpuChessTimer timer) {
        if (timer == null)
            throw new AssertionError(params.budgetType + " needs a game timer");
        gameTimer = timer;
        if (byRound() && state.getRoundCounter() != round) {
            round = state.getRoundCounter();
            decisionsMade = 0;
            if (params.roundAllocation) {
                clockAtRoundStart = gameTimer.remainingTimeMillis() - gameTimer.elapsedMillis();
                roundBudget = allocateRound(clockAtRoundStart);
            }
        }
        behindDone = unstableDone = closeDone = 0;
        stoppedEarly = false;
        clockAtStart = gameTimer.remainingTimeMillis();
        plannedBudget = allocate(clockAtStart - gameTimer.elapsedMillis());
    }

    /**
     * The semi-dynamic strategies (EXP, OPEN and MID).
     *
     * @param remainingMillis - the time left on the game clock
     * @return the search time in milliseconds
     */
    long allocate(long remainingMillis) {
        long available = availableMillis(remainingMillis);
        double share;
        if (params.roundAllocation)
            share = Math.max(0, roundBudget - (clockAtRoundStart - remainingMillis)) / expectedRemainingDecisions();
        else
            share = available / expectedRemainingDecisions();
        share *= params.timeFactor;
        if (params.midHeight != 0.0) {
            // (1 + a.exp(-(x - b)^2 / 2c^2)), where x is 1 for the first decision of the game (or round)
            double x = decisionsMade + 1;
            share *= 1.0 + params.midHeight * Math.exp(-Math.pow(x - params.midCentre, 2)
                    / (2.0 * params.midWidth * params.midWidth));
        }
        // the minimum search is only allowed while the clock above the reserve can pay for it; after that a search
        // has a single iteration
        long minimum = Math.min(params.minMoveTimeMS, remainingMillis - params.clockReserveMS - (long) overheadMillis);
        return (long) Math.max(Math.max(minimum, 0), Math.min(share, available));
    }

    /**
     * The first level of roundAllocation: the time for the round that starts now.
     *
     * @param remainingMillis - the time left on the game clock
     */
    long allocateRound(long remainingMillis) {
        // the minimum search and overhead of each decision in later rounds is kept back
        double laterRounds = (expectedRemainingRounds() - 1) * decisionsPerRound()
                * (params.minMoveTimeMS + overheadMillis);
        double share = (remainingMillis - params.clockReserveMS - laterRounds) / expectedRemainingRounds();
        return (long) Math.max(0, share * params.roundTimeFactor);
    }

    /**
     * The time that can be spent on the current search
     */
    private long availableMillis(long remainingMillis) {
        // keep back the reserve, this decision's overhead, and the minimum search and overhead of each later decision,
        // so that the clock is not overrun when the game lasts as long as expected
        double later = expectedLaterDecisions() * (params.minMoveTimeMS + overheadMillis);
        return (long) (remainingMillis - params.clockReserveMS - overheadMillis - later);
    }

    /**
     * @return the time for the planned search in milliseconds, less the time already spent on the decision (which
     * is on the game timer)
     */
    public long searchBudget() {
        return Math.max(0, plannedBudget - gameTimer.elapsedMillis());
    }

    /**
     * Equation 9 of Baier and Winands: the second-most-visited action cannot catch up with the most-visited one in
     * earlyStopP times the remaining iterations. With earlyStopP = 1 this never changes the action chosen.
     * With SIMPLE, the highest-valued action stays ahead in earlyStopP times the remaining iterations, with
     * stopConfidence.
     */
    public boolean bestActionSettled(SingleTreeNode root, double remainingIterations) {
        SingleTreeNode decisionRoot = root.decisionRoot();
        List<AbstractAction> actions = rootActions(decisionRoot);
        if (params.selectionPolicy == SIMPLE) {
            stoppedEarly = leaderSettled(decisionRoot, actions, params.earlyStopP * remainingIterations,
                    params.stopConfidence);
        } else {
            int[] topTwo = topTwoVisits(decisionRoot, actions);
            stoppedEarly = topTwo[0] - topTwo[1] > params.earlyStopP * remainingIterations;
        }
        return stoppedEarly;
    }

    /**
     * BEHIND, UNST and CLOSE, checked each time the search budget is used up.
     *
     * @return the time of the extension in milliseconds, or 0 for none
     */
    public long extension(SingleTreeNode root) {
        SingleTreeNode decisionRoot = root.decisionRoot();
        List<AbstractAction> actions = rootActions(decisionRoot);
        AbstractAction mostVisited = mostVisited(decisionRoot, actions);
        if (mostVisited == null || actions.size() < 2)
            return 0;
        double factor;
        if (behindDone < params.behindLoops && winRate(decisionRoot, decisionRoot.bestAction()) < params.behindThreshold) {
            behindDone++;
            factor = params.behindFactor;
        } else if (unstableDone < params.unstableLoops && !mostVisited.equals(highestValued(decisionRoot, actions))) {
            unstableDone++;
            factor = params.unstableFactor;
        } else if (closeDone < params.closeLoops && isClose(decisionRoot, actions)) {
            closeDone++;
            factor = params.closeFactor;
        } else {
            return 0;
        }
        long budget = (long) (factor * plannedBudget);
        long available = availableMillis(gameTimer.remainingTimeMillis() - gameTimer.elapsedMillis());
        budget = Math.min(budget, available);
        return budget < params.minMoveTimeMS ? 0 : budget;
    }

    /**
     * @return the root actions that bestAction() chooses between (when the tree policy is not EXP3 or regret matching)
     */
    static List<AbstractAction> rootActions(SingleTreeNode root) {
        return root.actionsToConsider(root.getActionsFromOpenLoopState());
    }

    /**
     * Unlike bestAction(), this is the most-visited action whatever the selection policy, with no noise to break
     * ties, as UNST compares it with the highest-valued action
     *
     * @return the most-visited of the actions (the first of any tied), or null if none has been visited
     */
    static AbstractAction mostVisited(SingleTreeNode root, List<AbstractAction> actions) {
        AbstractAction best = null;
        int bestVisits = 0;
        for (AbstractAction action : actions) {
            int visits = root.actionVisits(action);
            if (visits > bestVisits) {
                bestVisits = visits;
                best = action;
            }
        }
        return best;
    }

    /**
     * @return the visit counts of the most-visited and second-most-visited of the actions (0 if there are none)
     */
    static int[] topTwoVisits(SingleTreeNode root, List<AbstractAction> actions) {
        int first = 0, second = 0;
        for (AbstractAction action : actions) {
            int visits = root.actionVisits(action);
            if (visits > first) {
                second = first;
                first = visits;
            } else if (visits > second) {
                second = visits;
            }
        }
        return new int[]{first, second};
    }

    /**
     * @return the mean value to the decision player of the action, or NaN if it has not been visited
     */
    static double meanValue(SingleTreeNode root, AbstractAction action) {
        ActionStats stats = root.getActionStats(action);
        if (stats == null || stats.nVisits == 0)
            return Double.NaN;
        return stats.totValue[root.decisionPlayer] / stats.nVisits;
    }

    /**
     * The mean value of the action rescaled to [0, 1], so with rewards of -1, 0 and 1 a draw counts as half a win
     */
    double winRate(SingleTreeNode root, AbstractAction action) {
        // the range of rewards seen in the search (as used to normalise UCB), or else the heuristic's range
        double min = root.lowReward, max = root.highReward;
        if (!(max > min)) {
            min = params.heuristic.minValue();
            max = params.heuristic.maxValue();
        }
        return (meanValue(root, action) - min) / (max - min);
    }

    static AbstractAction highestValued(SingleTreeNode root, List<AbstractAction> actions) {
        AbstractAction best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (AbstractAction action : actions) {
            double value = meanValue(root, action);
            if (value > bestValue) {
                bestValue = value;
                best = action;
            }
        }
        return best;
    }

    private boolean isClose(SingleTreeNode root, List<AbstractAction> actions) {
        if (params.selectionPolicy == SIMPLE)
            return !leaderSettled(root, actions, Double.POSITIVE_INFINITY, params.closeConfidence);
        int[] topTwo = topTwoVisits(root, actions);
        return topTwo[0] - topTwo[1] < params.closeThreshold * topTwo[0];
    }

    /**
     * For SIMPLE: whether the highest-valued action stays ahead of every other visited action after horizon more
     * iterations, with the given confidence (1 - alpha). Each action is projected to keep its share of the visits.
     * With an infinite horizon this is the confidence that the highest-valued action is truly the best.
     * <p>
     * Each other action is compared with a z-test on the difference of the means, with the Sidak correction for
     * the number of comparisons. Unvisited actions are not considered. A difference with no variance (for example
     * between two actions with one visit each) is settled if the highest-valued action is strictly ahead.
     */
    static boolean leaderSettled(SingleTreeNode root, List<AbstractAction> actions, double horizon,
                                 double confidence) {
        List<AbstractAction> visited = actions.stream().filter(a -> root.actionVisits(a) > 0).toList();
        if (visited.size() < 2)
            return true;
        AbstractAction leader = highestValued(root, visited);
        int totalVisits = visited.stream().mapToInt(root::actionVisits).sum();
        double z = Utils.standardZScore(1.0 - confidence, visited.size() - 1);
        double leaderMean = meanValue(root, leader);
        double leaderVariance = meanVariance(root, leader, horizon, totalVisits);
        for (AbstractAction action : visited) {
            if (action.equals(leader))
                continue;
            double gap = leaderMean - meanValue(root, action);
            double se = Math.sqrt(leaderVariance + meanVariance(root, action, horizon, totalVisits));
            if (se == 0.0 ? gap <= 0.0 : gap < z * se)
                return false;
        }
        return true;
    }

    /**
     * The variance of the change in a visited action's mean after its share of horizon more iterations, which is
     * s^2.r / (n.(n + r)) for r more visits. With an infinite horizon this is the variance of the mean, s^2 / n.
     */
    static double meanVariance(SingleTreeNode root, AbstractAction action, double horizon, int totalVisits) {
        ActionStats stats = root.getActionStats(action);
        int n = stats.nVisits;
        double mean = stats.totValue[root.decisionPlayer] / n;
        double variance = Math.max(0.0, stats.squaredTotValue[root.decisionPlayer] / n - mean * mean);
        if (Double.isInfinite(horizon))
            return variance / n;
        double r = horizon * n / totalVisits;
        return variance * r / (n * (n + r));
    }

    /**
     * Called at the end of each decision that was searched.
     *
     * @param searchMillis - the time spent in the search
     */
    public void endDecision(double searchMillis) {
        decisionsMade++;
        if (gameTimer != null) {
            double overhead = Math.max(0.0, gameTimer.elapsedMillis() - searchMillis);
            overheadMillis = overheadMillis == 0.0 ? overhead : 0.8 * overheadMillis + 0.2 * overhead;
        }
    }

    /**
     * @return the time allocated to the current round with roundAllocation (milliseconds), otherwise 0
     */
    public long roundBudget() {
        return params.roundAllocation ? roundBudget : 0;
    }

    /**
     * @return the planned search time for the last decision in milliseconds,
     * before any extension
     */
    public long plannedBudget() {
        return plannedBudget;
    }

    /**
     * @return the game clock (in milliseconds) at the start of the last decision
     */
    public long clockAtStart() {
        return clockAtStart;
    }

    /**
     * @return the number of extensions of the last search
     */
    public int extensions() {
        return behindDone + unstableDone + closeDone;
    }

    public boolean stoppedEarly() {
        return stoppedEarly;
    }
}
