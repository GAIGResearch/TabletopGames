package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.ElapsedCpuTimer;
import java.util.List;
import java.util.Random;

import static players.utils.PlayerConstants.*;
import static utilities.Utils.noise;

class SingleTreeNode
{
    // Root node of tree
    private SingleTreeNode root;
    // Parent of this node
    private SingleTreeNode parent;
    // Children of this node
    private SingleTreeNode[] children;
    // Depth of this node
    private int depth;

    // Total value of this node
    private double totValue;
    // Number of visits
    private int nVisits;
    // Number of FM calls up until this node
    private int fmCallsCount;
    // Parameters guiding the search
    private MCTSPlayer player;

    // State in this node (closed loop)
    private AbstractGameState state;

    // Called from MCTSPlayer
    SingleTreeNode(MCTSPlayer player, int numActionsAvailable) {
        this(player, numActionsAvailable, null, null, null);
    }

    // Called in tree expansion
    private SingleTreeNode(MCTSPlayer player, int numActionsAvailable, SingleTreeNode parent,
                           SingleTreeNode root, AbstractGameState state) {
        this.player = player;
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = root;
        children = new SingleTreeNode[numActionsAvailable];
        totValue = 0.0;
        this.state = state;
        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }

    }

    /**
     * Initializes the root node
     * @param root - root node
     * @param gs - root game state
     */
    void setRootGameState(SingleTreeNode root, AbstractGameState gs)
    {
        this.state = gs;
        this.root = root;
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    void mctsSearch() {

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = 5;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if(player.params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(player.params.timeBudget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;
        while(!stop){
            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            SingleTreeNode selected = treePolicy();
            // Monte carlo rollout: return value of MC rollout from the newly added node
            double delta = selected.rollOut();
            // Back up the value of the rollout through the tree
            selected.backUp(delta);
            // Finished iteration
            numIters++;

            // Check stopping condition
            if(player.params.budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if(player.params.budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= player.params.iterationsBudget;
            } else if(player.params.budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCallsCount > player.params.fmCallsBudget;
            }
        }
    }

    /**
     * Selection + expansion steps.
     *  - Tree is traversed until a node not fully expanded is found.
     *  - A new child of this node is added to the tree.
     * @return - new node added to the tree.
     */
    private SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < player.params.rolloutLength) {
            if (cur.notFullyExpanded()) {
                // Node found! Expand it and return new child
                return cur.expand();
            } else {
                // Move to next child given by UCT function
                cur = cur.uct();
            }
        }

        return cur;
    }

    /**
     * Checks if a node is fully expanded.
     * @return true if node not fully expanded, false otherwise.
     */
    private boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     * @return - new child node.
     */
    private SingleTreeNode expand() {

        int chosenAction = -1;
        double value = -1;

        // Find random child not already created
        Random r = new Random(player.params.getRandomSeed());
        for (int i = 0; i < children.length; i++) {
            double x = r.nextDouble();
            if (x > value && children[i] == null) {
                chosenAction = i;
                value = x;
            }
        }

        // Roll the state, create a new node and assign it.
        AbstractGameState nextState = state.copy();
        List<AbstractAction> availableActions = nextState.getActions();
        List<AbstractAction> nextActions = advance(nextState, availableActions.get(chosenAction));
        SingleTreeNode tn = new SingleTreeNode(player, nextActions.size(), this, depth == 0 ? this : root, nextState);
        children[chosenAction] = tn;
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     * @param gs - current game state
     * @param act - action to apply
     * @return - list of actions available in the next state
     */
    private List<AbstractAction> advance(AbstractGameState gs, AbstractAction act)
    {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
        return player.getForwardModel().computeAvailableActions(gs);
    }

    /**
     * Apply UCB1 equation to choose a child.
     * @return - child node with the highest UCB value.
     */
    private SingleTreeNode uct() {

        SingleTreeNode selected;
        boolean iAmMoving = (state.getCurrentPlayer() == player.getPlayerID());

        double[] values = new double[this.children.length];
        for(int i = 0; i < this.children.length; i++) {
            SingleTreeNode child = children[i];

            // Find child value
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + player.params.epsilon);

            // Find UCB value
            double uctValue = childValue +
                    player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + player.params.epsilon));

            // Apply small noise to break ties randomly
            uctValue = noise(uctValue, player.params.epsilon, player.rnd.nextDouble());

            // Assign value
            values[i] = uctValue;
        }

        // Find child with highest UCB value, maximising for ourselves and minimizing for opponents
        int which = -1;
        double bestValue = iAmMoving ? -Double.MAX_VALUE : Double.MAX_VALUE;
        for(int i = 0; i < values.length; ++i) {
            if ((iAmMoving && values[i] > bestValue) || (!iAmMoving && values[i] < bestValue)){
                which = i;
                bestValue = values[i];
            }
        }
        selected = children[which];

        // No rolling the state. This is closed loop.
        // advance(state, actions.get(selected.childIdx), true);

        // FM call added anyway
        root.fmCallsCount++;

        return selected;
    }

    /**
     * Perform a Monte Carlo rollout from this node.
     * @return - value of rollout.
     */
    private double rollOut()
    {
        if (player.params.rolloutsEnabled) {
            // If rollouts are enabled, select random actions for the rollout
            AbstractGameState rolloutState = state.copy();
            int thisDepth = this.depth;

            while (!finishRollout(rolloutState, thisDepth)) {
                int nActions = rolloutState.getActions().size();
                AbstractAction next = rolloutState.getActions().get(player.rnd.nextInt(nActions));
                advance(rolloutState, next);
                thisDepth++;
            }

            // Evaluate final state and return normalised score
            return rolloutState.getScore(player.getPlayerID());
//            return player.params.gameHeuristic.evaluateState(rolloutState, player.getPlayerID());
        } else {
            // Evaluate the state without doing a rollout of these are disabled, return normalised score
            return state.getScore(player.getPlayerID());
//           return player.params.gameHeuristic.evaluateState(state, player.getPlayerID());
        }
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
     * @param rollerState - current state
     * @param depth - current depth
     * @return - true if rollout finished, false otherwise
     */
    private boolean finishRollout(AbstractGameState rollerState, int depth)
    {
        if (depth >= player.params.rolloutLength)
            return true;

        // End of game
        return !rollerState.isNotTerminal();
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     * @param result - value of rollout to backup
     */
    private void backUp(double result)
    {
        SingleTreeNode n = this;
        while (n != null) {
            n.nVisits++;
            n.totValue += result;
            n = n.parent;
        }
    }

    /**
     * Calculates the most visited child from the root.
     * @return - most visited child index.
     */
    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;

        for (int i=0; i<children.length; i++) {
            if (children[i] != null) {
                double childValue = children[i].nVisits;

                // Apply small noise to break ties randomly
                childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());

                // Save best value (highest visit count)
                if (childValue > bestValue) {

                    if (selected != -1) {
                        // This is higher than another child, not all equal
                        allEqual = false;
                    }

                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1) {
            // Choose first action if none was found
            System.out.println("Unexpected selection: visit count!");
            selected = 0;
        } else if (allEqual) {
            // If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    /**
     * Finds the child from the root with the highest value.
     * @return - child with highest value.
     */
    int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {
            if (children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + player.params.epsilon);

                // Apply small noise to break ties randomly
                childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());

                // Save best value
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection: Q value!");
            selected = 0;
        }

        return selected;
    }
}
