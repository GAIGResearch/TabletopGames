package players.sushiGoAgent;

import core.AbstractGameState;
import core.actions.AbstractAction;
import org.apache.arrow.flatbuf.Int;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import javax.swing.*;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;

class BasicTreeNode {
    // Root node of tree
    BasicTreeNode root;
    // Parent of this node
    BasicTreeNode parent;
    // Children of this node
    Map<List<AbstractAction>, BasicTreeNode> jointActionChildren = new HashMap<>();
    // Depth of this node
    final int depth;

    // Total value of this node TODO: DELETE THIS
//    private double totValue;
    // Number of visits
    private int nVisits;
    // Number of FM calls and State copies up until this node

    Map<Integer, Map<AbstractAction, ActionStats>> playerStatistics = new HashMap<>();

    List<Integer> playerOrder = new ArrayList<>();

    List<AbstractAction> incomingJointAction;

    private int fmCallsCount;
    // Parameters guiding the search
    private BasicMCTSPlayer player;
    private Random rnd;
    private RandomPlayer randomPlayer = new RandomPlayer();

    // State in this node (closed loop)
    private AbstractGameState state;

    protected BasicTreeNode(BasicMCTSPlayer player, BasicTreeNode parent, AbstractGameState state, Random rnd) {
        this.player = player;
        this.fmCallsCount = 0;
        this.nVisits = 0;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        setState(state);
        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
        this.rnd = rnd;
        randomPlayer.setForwardModel(player.getForwardModel());
    }

    protected BasicTreeNode(BasicMCTSPlayer player, BasicTreeNode parent, AbstractGameState state, Random rnd, List<AbstractAction> incomingJointAction) {
        this(player, parent, state, rnd);
        this.incomingJointAction = incomingJointAction;
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    void mctsSearch() {

        BasicMCTSParams params = player.getParameters();

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;

        while (!stop) {
            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            BasicTreeNode selected = treePolicy();
            // Monte carlo rollout: return value of MC rollout from the newly added node
            double[] delta = selected.rollOut();
            // Back up the value of the rollout through the tree
            selected.backUp(delta);
            // Finished iteration
            numIters++;

            // Check stopping condition
            PlayerConstants budgetType = params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if (budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= params.budget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCallsCount > params.budget;
            }
        }
    }

    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    private BasicTreeNode treePolicy() {
        BasicTreeNode cur = this;

        while (cur.state.isNotTerminal() && cur.depth < player.getParameters().maxTreeDepth) {
            List<AbstractAction> joint = cur.jointDecoupledUCB();
            BasicTreeNode child = cur.jointActionChildren.get(joint);

            if (child == null) {
                return cur.expand(joint);
            }
            cur = child;
        }
        // Reached a terminal state or the depth limit: roll out / evaluate from here
        return cur;
    }


    private void setState(AbstractGameState newState) {
        state = newState;
        // Copy the currentState and make a copy as we just want to initialize the uct tables in here only
        AbstractGameState stateCopy = newState.copy();

        int currentPlayer = stateCopy.getCurrentPlayer();

        // Use stats to ensure that we haven't already initialized this current players
        while (stateCopy.isNotTerminal() && !playerStatistics.containsKey(currentPlayer)) {
            var actions = player.getForwardModel().computeAvailableActions(stateCopy, player.getParameters().actionSpace);
            for (AbstractAction action : actions) {
                playerStatistics.computeIfAbsent(currentPlayer, k -> new LinkedHashMap<>()).put(action, new ActionStats());
            }

            jointActionChildren.putIfAbsent(actions, null);
            playerOrder.add(currentPlayer);
            advance(stateCopy, actions.get(0));
            currentPlayer = stateCopy.getCurrentPlayer();
        }
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    private List<List<AbstractAction>> unexpandedActions() {
        return jointActionChildren.keySet().stream().filter(a -> jointActionChildren.get(a) == null).collect(toList());
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    private BasicTreeNode expand(List<AbstractAction> jointActions) {

        AbstractGameState stateCopy = state.copy();

        for (int i = 0; i< playerOrder.size(); i++) {

            int mover = playerOrder.get(i);
            if (stateCopy.getCurrentPlayer() != mover) {
                throw new RuntimeException("Current player is not the expected player in the order");
            }

            advance(stateCopy, jointActions.get(i).copy());

            while (stateCopy.isNotTerminal() && stateCopy.isActionInProgress()
                    && stateCopy.getCurrentPlayer() == mover) {
                List<AbstractAction> extra = player.getForwardModel()
                        .computeAvailableActions(stateCopy, player.getParameters().actionSpace);
                advance(stateCopy, extra.get(rnd.nextInt(extra.size())).copy());
            }
        }

        BasicTreeNode childNode = new BasicTreeNode(player, this, stateCopy, rnd, jointActions);
        jointActionChildren.put(jointActions, childNode);
        return childNode;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
    }

    private List<AbstractAction> jointDecoupledUCB() {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        BasicMCTSParams params = player.getParameters();

        List<AbstractAction> joint = new ArrayList<>(state.getNPlayers());

        for (Integer playerId : playerOrder) {
            Map<AbstractAction, ActionStats> ucbTable = playerStatistics.get(playerId);
            AbstractAction bestAction = null;
            double bestValue = -Double.MAX_VALUE;

            for (Map.Entry<AbstractAction, ActionStats> entry : ucbTable.entrySet()) {
                AbstractAction action = entry.getKey();
                ActionStats actionStats = entry.getValue();
                double uctValue;
                if (actionStats.visits == 0) {
                    uctValue = 1e9;
                } else {
                    // Find child value
                    double hvVal = actionStats.totValue;
                    double actionValue = hvVal / (actionStats.visits + params.epsilon);
                    double explorationTerm = params.K * Math.sqrt(Math.log(this.nVisits + 1) / (actionStats.visits + params.epsilon));

                    uctValue = actionValue + explorationTerm;
                }

                uctValue = noise(uctValue,params.epsilon, player.getRnd().nextDouble());

                if (uctValue > bestValue) {
                    bestAction = action;
                    bestValue = uctValue;
                }
            }

            joint.add(bestAction);

        }

        return joint;
    }

    /**
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    private double[] rollOut() {
        int rolloutDepth = 0;

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state.copy();
        if (player.getParameters().rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                AbstractAction next = randomPlayer.getAction(rolloutState, randomPlayer.getForwardModel().computeAvailableActions(rolloutState, randomPlayer.parameters.actionSpace));
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        double[] rewardsForEachPlayer = new double[rolloutState.getNPlayers()];
        for (int p = 0; p < rewardsForEachPlayer.length; p++) {
            double value = player.getParameters().getStateHeuristic().evaluateState(rolloutState, p);
            if (Double.isNaN(value))
                throw new AssertionError("Illegal heuristic value - should be a number");
            rewardsForEachPlayer[p] = value;
        }


        return rewardsForEachPlayer;
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
     *
     * @param rollerState - current state
     * @param depth       - current depth
     * @return - true if rollout finished, false otherwise
     */
    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        if (depth >= player.getParameters().rolloutLength)
            return true;

        // End of game
        return !rollerState.isNotTerminal();
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param playerResults - value of rollout to backup
     */

    private void backUp(double[] playerResults) {
        BasicTreeNode n = this;
        n.nVisits += 1;

        while (n.parent != null) {
            BasicTreeNode par = n.parent;
            par.nVisits += 1;

            for (int i = 0; i < par.playerOrder.size(); i++) {
                int playerId = par.playerOrder.get(i);
                AbstractAction actionChosenForPlayer = n.incomingJointAction.get(i);
                var actionStats = par.playerStatistics.get(playerId).get(actionChosenForPlayer);
                actionStats.visits++;
                actionStats.totValue += playerResults[playerId];
            }
            n = par;
        }
    }

    /**
     * Calculates the best action from the root according to the most visited node
     *
     * @return - the best AbstractAction
     */

    AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        Map<AbstractAction, ActionStats> myTable = playerStatistics.get(player.getPlayerID());
        if (myTable == null) {
            throw new AssertionError("Unexpected - no statistics for player " + player.getPlayerID() + " at the root.");
        }

        for (Map.Entry<AbstractAction, ActionStats> entry : myTable.entrySet()) {
            double actionValue = entry.getValue().visits;

            // Apply small noise to break ties randomly
            actionValue = noise(actionValue, player.getParameters().epsilon, player.getRnd().nextDouble());

            // Save best value (highest visit count)
            if (actionValue > bestValue) {
                bestValue = actionValue;
                bestAction = entry.getKey();
            }
        }

        if (bestAction == null) {
            throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }

    class ActionStats {
        double totValue;
        int visits;
    }

}
