package groupM.players.breadthFirst;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;


import java.util.*;

import static java.util.stream.Collectors.toList;

public class BreadthFirstTreeNode {
    // Root node of tree
    BreadthFirstTreeNode root;
    // Parent of this node
    BreadthFirstTreeNode parent;
    // Children of this node
    Map<AbstractAction, BreadthFirstTreeNode> children = new HashMap<>();

    // Total value of this node
    double totValue;

    // Number of FM calls and State copies up until this node
    private int fmCallsCount;

    private BreadthFirstPlayer player;

    private BreadthFirstPlayer opponent;

    // State in this node (closed loop)
    private AbstractGameState state;

    protected BreadthFirstTreeNode(BreadthFirstPlayer player, BreadthFirstTreeNode parent, AbstractGameState state) {
        this.player = player;
        this.opponent = new BreadthFirstPlayer(player.params);
        this.opponent.setForwardModel(player.getForwardModel());
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        totValue = 0.0;
        setState(state);
    }

    AbstractGameState getState() {
        return state;
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    List<AbstractAction> unexpandedActions() {
        return children.keySet().stream().filter(a -> children.get(a) == null).collect(toList());
    }

    /**
     * Expands the node by creating any necessary child nodes and adding them to the tree,
     * then returns all child nodes.
     *
     * @return - Expanded child nodes.
     */
    Collection<BreadthFirstTreeNode> expand() {
        List<AbstractAction> notChosen = unexpandedActions();// copy the current state and advance it using the chosen action

        for (AbstractAction next : notChosen) {
            // we first copy the action so that the one stored in the node will not have any state changes
            AbstractGameState nextState = state.copy();
            advance(nextState, next.copy());

            // then instantiate a new node
            BreadthFirstTreeNode tn = new BreadthFirstTreeNode(player, this, nextState);
            children.put(next, tn);
        }
        return children.values();
    }

    /**
     * Evaluate the outcome of this node for every possible opponent action from this state
     *
     * @return - value of rollout.
     */
    double rollOut() {
        double value = 0;
        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state.copy();
        AbstractForwardModel forwardModel = opponent.getForwardModel();
        List<AbstractAction> actions = forwardModel.computeAvailableActions(rolloutState, opponent.parameters.actionSpace);
        int count = 0;
        for (AbstractAction next : actions) {
            rolloutState = state.copy();
            advance(rolloutState, next);
            value += player.params.getHeuristic().evaluateState(rolloutState, player.getPlayerID());
            count++;
        }
        // Evaluate final state and return normalised score
        return count > 0 ? value / count : value;
    }

    void setState(AbstractGameState newState) {
        state = newState;
        if (newState.isNotTerminal()) {
            for (AbstractAction action : player.getForwardModel().computeAvailableActions(state, player.params.actionSpace)) {
                children.put(action, null); // mark a new node to be expanded
            }
        }
    }

    void backUp(double result) {
        BreadthFirstTreeNode n = this;
        while (n != null) {
            n.totValue += result;
            n = n.parent;
        }
        player.stateToNodeMap.put(state.copy(),this);
    }

    /**
     * Advance the current game state with the given action and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
    }

    AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                BreadthFirstTreeNode node = children.get(action);
                double childValue = node.totValue;
                // Save best value (highest visit count)
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
}
