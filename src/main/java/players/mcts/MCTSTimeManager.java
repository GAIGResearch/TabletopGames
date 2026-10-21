package players.mcts;

import core.actions.AbstractAction;
import utilities.ElapsedCpuChessTimer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.TreePolicy.*;

/**
 * Time management for MCTS, following Baier and Winands, "Time Management for Monte Carlo Tree Search",
 * IEEE TCIAIG 8(3), 2016. Only the domain-independent strategies are implemented.
 * <p>
 * Semi-dynamic strategies set the time for a search before it starts, from the player's game timer
 * (budgetType BUDGET_GAME_SECONDS):
 * <ul>
 * <li>EXP: remaining time / m, where m is the expected number of remaining own decisions. m is either the constant
 * expectedDecisions, or (EXP-MOVES) is looked up in decisionTable by the number of decisions already made.</li>
 * <li>OPEN: EXP multiplied by timeFactor (f_opening).</li>
 * <li>MID: EXP multiplied by (1 + a.exp(-(x - b)^2 / 2c^2)), where x is the decision number (1 for the first
 * decision of the game), and a, b, c are midHeight, midCentre and midWidth.</li>
 * </ul>
 * Dynamic strategies act while the search runs. All but STOP continue the search once the planned search is over,
 * for the planned search time (or iterations) times a factor, up to the given number of loops:
 * <ul>
 * <li>BEHIND: if the win rate of the most-visited action is below behindThreshold.</li>
 * <li>UNST: if the most-visited action is not the highest-valued.</li>
 * <li>CLOSE: if the visits of the two most-visited actions differ by less than closeThreshold, relative to the
 * most-visited.</li>
 * <li>STOP: stop as soon as the second-most-visited action can no longer catch up with the most-visited in
 * earlyStopP times the expected remaining iterations (equation 9). The time saved is moved to earlier decisions by
 * timeFactor (f_earlystop). STOP_A is earlyStopP = 1 and timeFactor = 1; STOP_B is earlyStopP = 1.</li>
 * </ul>
 * The dynamic strategies also work with a fixed BUDGET_TIME or BUDGET_ITERATIONS per decision. There is then no
 * clock, so an extension simply spends more than the budget.
 * <p>
 * The game timer is the single source of truth for the time left. It is debited (by AbstractPlayer) with all the
 * time spent in each decision, so any overrun is corrected by later allocations.
 */
public class MCTSTimeManager {

    private final MCTSParams params;
    private final double[] decisionTable;
    private ElapsedCpuChessTimer gameTimer;
    private int decisionsMade;

    // Statistics on the current (or last) decision
    private long plannedBudget;
    private long clockAtStart;
    private int behindDone, unstableDone, closeDone;
    private boolean stoppedEarly;

    public MCTSTimeManager(MCTSParams params) {
        this.params = params;
        validate(params);
        decisionTable = params.decisionTable.isEmpty() ? null : loadDecisionTable(params.decisionTable);
    }

    /**
     * @return true if any time management is needed for these parameters
     */
    public static boolean isUsed(MCTSParams params) {
        return params.budgetType == BUDGET_GAME_SECONDS || params.earlyStopP > 0.0 || params.behindLoops > 0
                || params.unstableLoops > 0 || params.closeLoops > 0;
    }

    private static void validate(MCTSParams params) {
        if (params.numDeterminizations > 1)
            throw new IllegalArgumentException("Time management is not supported with numDeterminizations > 1");
        if (params.budgetType != BUDGET_GAME_SECONDS && params.budgetType != BUDGET_TIME
                && params.budgetType != BUDGET_ITERATIONS)
            throw new IllegalArgumentException("Time management is not supported with " + params.budgetType);
        if ((params.earlyStopP > 0.0 || params.closeLoops > 0) && (params.selectionPolicy != ROBUST
                || params.treePolicy == EXP3 || params.treePolicy == RegretMatching || params.treePolicy == NoAveragingRM))
            throw new IllegalArgumentException("STOP and CLOSE need the most-visited action to be played: use a " +
                    "ROBUST selectionPolicy (and a treePolicy that is not EXP3 or regret matching)");
        if (params.earlyStopInterval < 1)
            throw new IllegalArgumentException("earlyStopInterval must be at least 1");
    }

    /**
     * The file holds the expected number of remaining own decisions (including the current one), indexed by the
     * number of decisions already made in the game. Values are separated by commas or white space.
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

    MCTSParams params() {
        return params;
    }

    /**
     * Called at the start of each game.
     */
    public void newGame() {
        decisionsMade = 0;
    }

    /**
     * @return the number of decisions searched so far in this game
     */
    public int decisionsMade() {
        return decisionsMade;
    }

    /**
     * The expected number of remaining own decisions, including the current one (at least 1).
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
     * Called before each search.
     *
     * @param timer - the player's game timer (null unless budgetType is BUDGET_GAME_SECONDS). It is running, and
     *              elapsedMillis() is the time spent on this decision so far.
     */
    public void startDecision(ElapsedCpuChessTimer timer) {
        if (params.budgetType == BUDGET_GAME_SECONDS && timer == null)
            throw new AssertionError("BUDGET_GAME_SECONDS needs a game timer");
        gameTimer = timer;
        behindDone = unstableDone = closeDone = 0;
        stoppedEarly = false;
        if (params.budgetType == BUDGET_GAME_SECONDS) {
            clockAtStart = gameTimer.remainingTimeMillis();
            plannedBudget = allocate(clockAtStart - gameTimer.elapsedMillis());
        } else {
            clockAtStart = -1;
            plannedBudget = params.budget;
        }
    }

    /**
     * The semi-dynamic strategies (EXP, OPEN and MID).
     *
     * @param remainingMillis - the time left on the game clock
     * @return the search time in milliseconds
     */
    long allocate(long remainingMillis) {
        long available = remainingMillis - params.clockReserveMS;
        double share = available / expectedRemainingDecisions();
        share *= params.timeFactor;
        if (params.midHeight != 0.0) {
            double x = decisionsMade + 1;
            share *= 1.0 + params.midHeight * Math.exp(-Math.pow(x - params.midCentre, 2)
                    / (2.0 * params.midWidth * params.midWidth));
        }
        return (long) Math.max(params.minMoveTimeMS, Math.min(share, available));
    }

    /**
     * @param initialisationTime - milliseconds already spent on the decision before the search starts
     * @return the budget for the planned search (milliseconds for a time budget, iterations otherwise)
     */
    public long searchBudget(long initialisationTime) {
        if (params.budgetType == BUDGET_ITERATIONS)
            return plannedBudget;
        if (params.budgetType == BUDGET_GAME_SECONDS)
            // the initialisation time is already on the game timer
            return Math.max(0, plannedBudget - gameTimer.elapsedMillis());
        return plannedBudget - initialisationTime;
    }

    /**
     * Equation 9 of Baier and Winands: the second-most-visited action cannot catch up with the most-visited one in
     * earlyStopP times the remaining iterations. With earlyStopP = 1 this never changes the action chosen.
     */
    public boolean bestActionSettled(SingleTreeNode root, double remainingIterations) {
        SingleTreeNode decisionRoot = root.decisionRoot();
        int[] topTwo = decisionRoot.topTwoVisits(decisionRoot.rootActionsToConsider());
        stoppedEarly = topTwo[0] - topTwo[1] > params.earlyStopP * remainingIterations;
        return stoppedEarly;
    }

    /**
     * The dynamic strategies that continue the search (BEHIND, UNST and CLOSE), checked once a search segment ends.
     *
     * @return the budget of the extension (milliseconds for a time budget, iterations otherwise), or 0 for none
     */
    public long extension(SingleTreeNode root) {
        SingleTreeNode decisionRoot = root.decisionRoot();
        List<AbstractAction> actions = decisionRoot.rootActionsToConsider();
        AbstractAction mostVisited = decisionRoot.mostVisitedAction(actions);
        if (mostVisited == null || actions.size() < 2)
            return 0;
        double factor;
        if (behindDone < params.behindLoops && winRate(decisionRoot, mostVisited) < params.behindThreshold) {
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
        if (params.budgetType == BUDGET_GAME_SECONDS) {
            long available = gameTimer.remainingTimeMillis() - gameTimer.elapsedMillis() - params.clockReserveMS;
            budget = Math.min(budget, available);
            if (budget < params.minMoveTimeMS)
                return 0;
        }
        return Math.max(budget, 0);
    }

    /**
     * The mean value of the action, rescaled from the heuristic's range to [0, 1] (so with -1 for a loss,
     * 0 for a draw and 1 for a win, a draw counts as half a win).
     */
    double winRate(SingleTreeNode root, AbstractAction action) {
        double min = params.heuristic.minValue(), max = params.heuristic.maxValue();
        return (root.meanActionValue(action) - min) / (max - min);
    }

    private AbstractAction highestValued(SingleTreeNode root, List<AbstractAction> actions) {
        AbstractAction best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (AbstractAction action : actions) {
            double value = root.meanActionValue(action);
            if (value > bestValue) {
                bestValue = value;
                best = action;
            }
        }
        return best;
    }

    private boolean isClose(SingleTreeNode root, List<AbstractAction> actions) {
        int[] topTwo = root.topTwoVisits(actions);
        return topTwo[0] - topTwo[1] < params.closeThreshold * topTwo[0];
    }

    /**
     * Called after each search.
     */
    public void endDecision() {
        decisionsMade++;
    }

    /**
     * @return the planned budget for the last decision (milliseconds for a time budget, iterations otherwise),
     * before any extension
     */
    public long plannedBudget() {
        return plannedBudget;
    }

    /**
     * @return the game clock (in milliseconds) at the start of the last decision, or -1 if there is no game timer
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
