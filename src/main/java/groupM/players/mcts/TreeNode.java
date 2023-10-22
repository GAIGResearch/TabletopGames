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
    protected int nVisits;
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
    protected GroupMMCTSPlayer player;
    private Random rnd;
    private RandomPlayer randomPlayer = new RandomPlayer();

    // State in this node (closed loop)
    protected AbstractGameState state;

    protected boolean pruned;

    protected TreeNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
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
        pruned = false;
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
            Rollout rollout = selected.rollOut();
            // Back up the value of the rollout through the tree
            rollout.backupRollout(selected);
            // Finished iteration
            numIters++;

            if(player.params.prune) {
                pruneChildren();
            }
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

    abstract Comparator<TreeNode> getPruningComparator();

    List<AbstractAction> unprunedActions() {
        return children.keySet().stream().filter(a->(children.get(a) == null || !children.get(a).pruned)).collect(toList());
    }

    List<TreeNode> unprunedNodes() {
        return children.values().stream().filter(a->!a.pruned).collect(toList());
    }

    private void pruneChildren() {
        long childLog = Math.round(this.player.params.pruneAlpha*Math.log(children.size()));
        long retainedChildren =
                Math.max(childLog,this.player.params.minRetained);
        List<TreeNode> childNodes = unprunedNodes();
        childNodes.sort(getPruningComparator());
        Iterator<TreeNode> childIt = childNodes.iterator();
        while(unprunedActions().size() > retainedChildren) {
            TreeNode node = childIt.next();
            node.pruned = this.getNVisits()>=this.player.params.pruneThreshold;
        }
    }

    /**
     * Returns a new Rollout object that will be used to propagate values back up the tree
     * @return Rollout
     */
    Rollout newRollout(){
        return new Rollout();
    }

    /**
     * Calculates the best action from the root according to algorithm
     *
     * @return - the best AbstractAction
     */
    AbstractAction bestAction(){
        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                TreeNode child = children.get(action);
                double childValue = getChildValue(child, false);

                // Save best value
                if (childValue > bestValue) {
                    bestValue = childValue;
                    bestAction = action;
                }
            }
        }

        if (bestAction == null) {
            throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }
    
    /**
     * Returns the value of the child node according to the tree policy
     * @param child the child node to evaluate
     * @param isExpanding true if evaluating during the expansion step, false if evaluating for the best action
     * @return the value of the node according to the tree policy
     */
    abstract double getChildValue(TreeNode child, boolean isExpanding);

    /**
     * Back up the value of the child through all parents.
     *
     * @param result - value of rollout to backup
     */
    abstract void backUp(double result);

    abstract int getNVisits();


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
    private Rollout rollOut() {
        int rolloutDepth = 0; // counting from end of tree
        Rollout rollout = newRollout();

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state.copy();
        boolean isMyTurn = false;

        if (player.params.rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                AbstractAction next = randomPlayer.getAction(rolloutState, randomPlayer.getForwardModel().computeAvailableActions(rolloutState, randomPlayer.parameters.actionSpace));
                advance(rolloutState, next);
                
                // Only add action taken by us to rollout storage
                if(isMyTurn) rollout.addAction(next);
                rolloutDepth++;
                isMyTurn = !isMyTurn;
            }
        }
        // Evaluate final state and return normalised score
        double value = player.params.getHeuristic().evaluateState(rolloutState, player.getPlayerID());
        if (Double.isNaN(value))
            throw new AssertionError("Illegal heuristic value - should be a number");
        rollout.setResult(value);
        return rollout;
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

    private AbstractAction selectAction() {
     AbstractAction bestAction = null;
     double bestValue = -Double.MAX_VALUE;

     for (AbstractAction action : unprunedActions()) {
        TreeNode child = (TreeNode) children.get(action);
         if (child == null)
             throw new AssertionError("Should not be here");
         else if (bestAction == null)
             bestAction = action;

         double childValue = getChildValue(child, true);

         // Assign value
         if (childValue > bestValue) {
             bestAction = action;
             bestValue = childValue;
         }
     }

     if (bestAction == null)
         throw new AssertionError("No action selected!");

     root.fmCallsCount++;  // log one iteration complete
     return bestAction;
    }
}
