package groupM.players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import java.util.*;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;

abstract class TreeNode {
    // Root node of tree
    TreeNode root;
    // Parent of this node
    TreeNode parent;
    // Children of this node
    Map<AbstractAction, TreeNode> children = new HashMap<>();
    // Depth of this node
    final int depth;

    // Number of FM calls and State copies up until this node
    protected int fmCallsCount;
    // Parameters guiding the search
    protected MCTSPlayer player;
    private Random rnd;
    private RandomPlayer randomPlayer = new RandomPlayer();

    // State in this node (closed loop)
    protected AbstractGameState state;

    protected TreeNode(MCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        this.player = player;
        this.fmCallsCount = 0;
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

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    void mctsSearch() {

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = player.params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (player.params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(player.params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;

        while (!stop) {
            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            TreeNode selected = treePolicy();
            // Monte carlo rollout: return value of MC rollout from the newly added node
            double delta = selected.rollOut();
            // Back up the value of the rollout through the tree
            selected.backUp(delta);
            // Finished iteration
            numIters++;

            // Check stopping condition
            PlayerConstants budgetType = player.params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if (budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= player.params.budget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCallsCount > player.params.budget;
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
    private TreeNode treePolicy() {

        TreeNode cur = this;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < player.params.maxTreeDepth) {
            if (!cur.unexpandedActions().isEmpty()) {
                // We have an unexpanded action
                cur = cur.expand();
                return cur;
            } else {
                // Move to next child given by UCT function
                AbstractAction actionChosen = cur.selectAction();
                cur = cur.children.get(actionChosen); 
            }
        }

        return cur;
    }

    private void setState(AbstractGameState newState) {
        state = newState;
        if (newState.isNotTerminal())
            for (AbstractAction action : player.getForwardModel().computeAvailableActions(state, player.params.actionSpace)) {
                children.put(action, null); // mark a new node to be expanded
            }
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    private List<AbstractAction> unexpandedActions() {
        return children.keySet().stream().filter(a -> children.get(a) == null).collect(toList());
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    private TreeNode expand() {
        // Find random child not already created
        Random r = new Random(player.params.getRandomSeed());
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(r.nextInt(notChosen.size()));

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        AbstractGameState nextState = state.copy();
        advance(nextState, chosen.copy());

        // then instantiate a new node
        TreeNode tn = player.params.treeNodeFactory.createNode(player, this, nextState, rnd);
        children.put(chosen, tn);
        return tn;
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

    /**
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    private double rollOut() {
        int rolloutDepth = 0; // counting from end of tree

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state.copy();
        if (player.params.rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                AbstractAction next = randomPlayer.getAction(rolloutState, randomPlayer.getForwardModel().computeAvailableActions(rolloutState, randomPlayer.parameters.actionSpace));
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        double value = player.params.getHeuristic().evaluateState(rolloutState, player.getPlayerID());
        if (Double.isNaN(value))
            throw new AssertionError("Illegal heuristic value - should be a number");
        return value;
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
     *
     * @param rollerState - current state
     * @param depth       - current depth
     * @return - true if rollout finished, false otherwise
     */
    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        if (depth >= player.params.rolloutLength)
            return true;

        // End of game
        return !rollerState.isNotTerminal();
    }

    /**
     * Selects the action to take from the current node.
     * Must handle the explore/exploit dilemma
     * @return the action
     */
    abstract AbstractAction selectAction();

    /**
     * Back up the value of the child through all parents.
     *
     * @param result - value of rollout to backup
     */
    abstract void backUp(double result);

    /**
     * Calculates the best action from the root according to algorithm
     *
     * @return - the best AbstractAction
     */
    abstract AbstractAction bestAction();

}
