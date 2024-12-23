package games.conquest.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.conquest.actions.ApplyCommand;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;

/**
 * This class is a more lightweight version of BasicTreeNode which doesn't save the full state,
 * but only keeps track of the gameState's hashCode. This saves heap memory, and makes it easier to
 * match up multiple paths to reach the same node.
 */
public class CQTreeNode {
    boolean debug = false;

    CQTreeNode root;
    HashMap<CQTreeNode, AbstractAction> parents;
    CQTreeNode parent = null; // The specific parent node used to access this node in a MCTS run
    Map<AbstractAction, CQTreeNode> children = new HashMap<>();
    Map<AbstractAction, Integer> childVisits = new HashMap();
    int nVisits = 0;
    double totValue = 0.0;

    final int playerId;

    private int fmCallsCount;
    final int depth;
    private AbstractGameState state;

    // for root node: save a list of all states and their nodes
    Map<Integer, CQTreeNode> stateNodeMap = null;

    private CQMCTSPlayer player;
    private Random rnd;
    public AbstractAction selectedAction = null;

    public CQTreeNode(CQMCTSPlayer player, CQTreeNode parent, AbstractGameState gameState, Random rnd) {
        this.player = player;
        this.fmCallsCount = 0;
        this.playerId = gameState.getCurrentPlayer();
        this.rnd = rnd;
        setState(gameState);

        if (parent == null) {
            root = this;
            depth = 0;
            stateNodeMap = new HashMap<>();
            parents = null;
        } else {
            root = parent.root;
            depth = parent.depth + 1;
            parents = new HashMap<>();
        }
        root.stateNodeMap.put(gameState.hashCode(), this);
    }

    /**
     * Check if the current best move will result in at least one full turn completed
     * @return true if you'd finish a turn by repeatedly selecting the best move; false otherwise
     */
    boolean incompleteTurn() {
        CQTreeNode node = this;
        int player = node.playerId;
        while (player == node.playerId) {
            if (node.selectedAction == null) {
                return true;
            }
            node = node.children.get(node.selectedAction);
        }
        // loop finished without returning, so we've reached the opponent move.
        return false;
    }

    void mctsSearch() {
        mctsSearch(true);
    }
    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    void mctsSearch(boolean flexibleBudget) {
        CQMCTSParams params = player.getParameters();

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        int budget = params.budget;
        if (params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;

        while (!stop) {
            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found,
            // add a new node to the tree. This also fills `history` with all actions taken.
            CQTreeNode selected = treePolicy();
            double delta = player.getParameters().getHeuristic().evaluateState(selected.state, player.getPlayerID());
            // Back up the value of the last node through the tree
            selected.backUp(delta);
            // Finished iteration
            numIters++;

            // Check stopping condition
            PlayerConstants budgetType = params.budgetType;
            switch (budgetType) {
                case BUDGET_TIME -> {
                    // Time budget
                    acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                    avgTimeTaken = acumTimeTaken / numIters;
                    remaining = elapsedTimer.remainingTimeMillis();
                    stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
                }
                case BUDGET_ITERATIONS ->
                    // Iteration budget
                        stop = numIters >= params.budget;
                case BUDGET_FM_CALLS -> {
                    // FM calls budget
                    stop = fmCallsCount > params.budget;
                }
            }
            if (stop && flexibleBudget && incompleteTurn()) {
                // Flexible budget: allow for more time if the best move does not reach the end of the turn
                budget += params.budget;
                System.out.println("Increased budget to " + budget);
            }
        }
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    private void backUp(double result) {
        CQTreeNode n = this;
        // TODO: Math.max(result, n.totValue)? Or that, but divide by depth?
        while (n.parent != null) { // Follow trace of parent nodes up to root
            n.nVisits++;
            n.totValue += result;
            // Get the next node up
            CQTreeNode nextN = n.parent;
            // leave an increment for the child node we just came from
            nextN.childVisits.merge(n.parents.get(nextN), 1, Integer::sum);
            // Move to the next level
//            if (nextN.nVisits + 1 != nextN.childVisits.get(n.parents.get(nextN))) {
//                System.out.println("Mismatch... weird?");
//            }
            // clean up and move up a node
            n.parent = null;
            n = nextN;
        }
        // increment root node after backing up all the way
        n.nVisits++;
        n.totValue += result;
    }

    /**
     * UCB run, but instead of checking child.nVisits, use the visits tracked from this node.
     * We use the `childVisits` for keeping track of that.
     * To ensure this works properly, backup needs to increment the correct childVisits.
     * <a href="https://stackoverflow.com/a/50198274/1256925">Inspiration from here</a>
     * @return the selected action using UCB
     */
    private AbstractAction ucb() {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        CQMCTSParams params = player.getParameters();

        for (AbstractAction action : children.keySet()) {
            CQTreeNode child = children.get(action);
            if (child == null)
                throw new AssertionError("Should not be here");
            else if (bestAction == null)
                bestAction = action;

            // Find child value
            double hvVal = child.totValue;
            int visits = child.nVisits;
            double childValue = hvVal / (visits + params.epsilon);

            // default to standard UCB
            double explorationTerm = params.K * Math.sqrt(Math.log(this.nVisits + 1) / (visits + params.epsilon));
            // unless we are using a variant

            // Find 'UCB' value
            // If 'we' are taking a turn we use classic UCB
            // If it is an opponent's turn, then we assume they are trying to minimise our score (with exploration)
            boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
            double uctValue = iAmMoving ? childValue : -childValue;
            uctValue += explorationTerm;

            // Apply small noise to break ties randomly
            uctValue = noise(uctValue,params.epsilon, player.getRnd().nextDouble());

            // Assign value
            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }
        }

        if (bestAction == null)
            throw new AssertionError("We have a null value in UCT : shouldn't really happen!");

        root.fmCallsCount++;  // log one iteration complete
        return bestAction;
    }

    private CQTreeNode enterNextState(AbstractAction action) {
        CQTreeNode next = children.get(action);
        next.parent = this;
        return next;
    }

    /*========= The following functions were taken from BasicTreeNode directly ========*/
    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    private CQTreeNode treePolicy() {
        CQTreeNode cur = this;
        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < player.getParameters().maxTreeDepth) {
            if (!cur.unexpandedActions().isEmpty()) {
                // We have an unexpanded action
                cur = cur.expand();
                break;
            } else {
                // Move to next child given by UCT function
                AbstractAction actionChosen = cur.ucb();
                cur.selectedAction = actionChosen; // keep track of most recently selected action
                cur = cur.enterNextState(actionChosen);
            }
        }
        return cur;
    }

    private void setState(AbstractGameState newState) {
        state = newState;
        if (newState.isNotTerminal())
            for (AbstractAction action : player.getForwardModel().computeAvailableActions(state, player.getParameters().actionSpace)) {
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
    private CQTreeNode expand() {
        // Find random child not already created
        Random r = new Random(player.getParameters().getRandomSeed());
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(r.nextInt(notChosen.size()));
        selectedAction = chosen; // keep track of most recently selected action

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        AbstractGameState nextState = state.copy();

        int nextHash = advance(nextState, chosen.copy());
        // First attempt to look up this hash in the list of previously seen states.
        // If we have encountered it before, we'll just re-use it here
        CQTreeNode tn = root.stateNodeMap.get(nextHash);
        if (tn == null) {
            // In most situations tn==null, so we haven't encountered it; so create a new node.
            tn = new CQTreeNode(player, this, nextState, rnd);
        }
        // then keep track of the links back and forth
        tn.parents.put(this, chosen);
        children.put(chosen, tn);
        tn.parent = this;
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private int advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
        return gs.hashCode();
    }

    /**
     * Calculates the best action from the root according to the most visited node
     * @return - the best AbstractAction
     */
    public AbstractAction bestAction() {
        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        if (children.keySet().size() == 0) {
            System.out.println("No actions? What?");
        }
        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                CQTreeNode child = children.get(action);
                double nodeValue = child.totValue / child.nVisits;
                double epsilon = player.getParameters().epsilon;
                // Apply small noise to break ties randomly
                double childValue = noise(nodeValue, epsilon, player.getRnd().nextDouble());
                // double childValue = noise(nodeVisits, epsilon, player.getRnd().nextDouble());

                // Save best value (highest visit count)
                if (childValue > bestValue) {
                    bestValue = childValue;
                    bestAction = action;
                }
            }
        }
        return bestAction;
    }

    public String toString() {
        return String.format("MTreeNode, visits: %d, totValue/visits: %f, states: %d", nVisits, totValue/nVisits, stateNodeMap == null ? 0 : stateNodeMap.size());
    }
}
