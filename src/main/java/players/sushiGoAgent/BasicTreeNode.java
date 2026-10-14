package players.sushiGoAgent;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import java.util.*;

import static players.PlayerConstants.*;
import static utilities.Utils.noise;

/**
 * Single-observer information-set MCTS with the same entry points as basicMCTS.BasicTreeNode.
 */
class BasicTreeNode {
    BasicTreeNode root;
    BasicTreeNode parent;
    Map<AbstractAction, BasicTreeNode> children = new HashMap<>();
    final int depth;

    private double totValue;
    private int nVisits;
    private int fmCallsCount;
    private int copyCallsCount;
    private final BasicMCTSPlayer player;
    private final Random rnd;
    private final RandomPlayer randomPlayer;

    // States are transient: a node represents an action history, not one determinization.
    private AbstractGameState state;
    private List<AbstractAction> legalActions;
    private final Map<AbstractAction, Integer> availabilityCounts = new HashMap<>();

    protected BasicTreeNode(BasicMCTSPlayer player, BasicTreeNode parent, AbstractGameState state, Random rnd) {
        this.player = player;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.rnd = rnd;
        randomPlayer = new RandomPlayer(rnd);
        randomPlayer.setForwardModel(player.getForwardModel());
        setState(state);
    }

    /**
     * Re-samples hidden information from the root player's perspective on every iteration.
     */
    void mctsSearch() {
        BasicMCTSParams params = player.getParameters();
        AbstractGameState observation = state;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME)
            elapsedTimer.setMaxTimeMillis(params.budget);

        int numIters = 0;
        double accumulatedTime = 0;
        boolean stop = false;
        while (!stop) {
            ElapsedCpuTimer iterationTimer = new ElapsedCpuTimer();
            int previousFmCalls = root.fmCallsCount;
            setState(observation.copy(player.getPlayerID()));
            root.copyCallsCount++;

            BasicTreeNode selected = treePolicy();
            double delta = selected.rollOut();
            selected.backUp(delta);
            numIters++;

            if (params.budgetType == BUDGET_TIME) {
                accumulatedTime += iterationTimer.elapsedMillis();
                double averageTime = accumulatedTime / numIters;
                long remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * averageTime || remaining <= params.breakMS;
            } else if (params.budgetType == BUDGET_ITERATIONS) {
                stop = numIters >= params.budget;
            } else if (params.budgetType == BUDGET_FM_CALLS) {
                stop = root.fmCallsCount >= params.budget;
                // A terminal or depth-zero search cannot consume any forward-model calls.
                stop |= root.fmCallsCount == previousFmCalls;
            } else if (params.budgetType == BUDGET_COPY_CALLS) {
                stop = root.copyCallsCount >= params.budget;
            } else if (params.budgetType == BUDGET_FMANDCOPY_CALLS) {
                stop = root.fmCallsCount + root.copyCallsCount >= params.budget;
            } else {
                throw new IllegalArgumentException("Unsupported MCTS budget type: " + params.budgetType);
            }
        }
        setState(observation);
    }

    private BasicTreeNode treePolicy() {
        BasicTreeNode cur = this;
        while (cur.state.isNotTerminal() && cur.depth < player.getParameters().maxTreeDepth) {
            if (cur.legalActions.isEmpty())
                throw new AssertionError("Non-terminal state has no available actions");

            for (AbstractAction action : cur.legalActions)
                cur.availabilityCounts.merge(action, 1, Integer::sum);

            if (!cur.unexpandedActions().isEmpty())
                return cur.expand();

            AbstractAction action = cur.ucb();
            AbstractGameState nextState = cur.state;
            cur.advance(nextState, action.copy());
            cur = cur.children.get(action);
            cur.setState(nextState);
        }
        return cur;
    }

    private void setState(AbstractGameState newState) {
        state = newState;
        legalActions = newState.isNotTerminal()
                ? player.getForwardModel().computeAvailableActions(newState, player.getParameters().actionSpace)
                : List.of();
        for (AbstractAction action : legalActions) {
            // Never reset expanded children when a new determinization reaches this node.
            if (!children.containsKey(action))
                children.put(action.copy(), null);
        }
    }

    private List<AbstractAction> unexpandedActions() {
        return legalActions.stream().filter(action -> children.get(action) == null).toList();
    }

    private BasicTreeNode expand() {
        List<AbstractAction> unexpanded = unexpandedActions();
        AbstractAction chosen = unexpanded.get(rnd.nextInt(unexpanded.size()));
        advance(state, chosen.copy());
        BasicTreeNode child = new BasicTreeNode(player, this, state, rnd);
        children.put(chosen.copy(), child);
        return child;
    }

    private void advance(AbstractGameState gs, AbstractAction action) {
        player.getForwardModel().next(gs, action);
        root.fmCallsCount++;
    }

    private AbstractAction ucb() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        BasicMCTSParams params = player.getParameters();
        boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();

        for (AbstractAction action : legalActions) {
            BasicTreeNode child = children.get(action);
            if (child == null)
                throw new AssertionError("UCT requires an expanded child");

            double childValue = child.totValue / (child.nVisits + params.epsilon);
            double exploration = params.K * Math.sqrt(
                    Math.log(availabilityCounts.get(action)) / (child.nVisits + params.epsilon));
            double uctValue = (iAmMoving ? childValue : -childValue) + exploration;
            uctValue = noise(uctValue, params.epsilon, rnd.nextDouble());
            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }
        }
        if (bestAction == null)
            throw new AssertionError("No legal action selected by UCT");
        return bestAction;
    }

    private double rollOut() {
        AbstractGameState rolloutState = state.copy();
        root.copyCallsCount++;
        int rolloutDepth = 0;
        while (!finishRollout(rolloutState, rolloutDepth)) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(
                    rolloutState, player.getParameters().actionSpace);
            if (actions.isEmpty())
                throw new AssertionError("Non-terminal rollout state has no available actions");
            AbstractAction next = randomPlayer.getAction(rolloutState, actions);
            advance(rolloutState, next.copy());
            rolloutDepth++;
        }
        double value = player.getParameters().getStateHeuristic().evaluateState(rolloutState, player.getPlayerID());
        if (!Double.isFinite(value))
            throw new AssertionError("Illegal heuristic value - should be finite");
        return value;
    }

    private boolean finishRollout(AbstractGameState rollerState, int rolloutDepth) {
        return rolloutDepth >= player.getParameters().rolloutLength || !rollerState.isNotTerminal();
    }

    private void backUp(double result) {
        BasicTreeNode node = this;
        while (node != null) {
            node.nVisits++;
            node.totValue += result;
            node = node.parent;
        }
    }

    AbstractAction bestAction() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        for (AbstractAction action : legalActions) {
            BasicTreeNode child = children.get(action);
            if (child != null) {
                double value = noise(child.nVisits, player.getParameters().epsilon, rnd.nextDouble());
                if (value > bestValue) {
                    bestValue = value;
                    bestAction = action;
                }
            }
        }
        if (bestAction == null)
            throw new AssertionError("Unexpected - no selection made.");
        return bestAction;
    }
}