package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * MultiTreeNode is really a wrapper for SingleTreeNode when we are using MultiTree MCTS.
 * In this case we maintain a separate tree for each player as a form of opponent modelling.
 * We need to keep track of where we are in each of these distinct trees - and that is what MultiTreeNode does.
 * <p>
 * Only the root node will be a MultiTreeNode
 */
public class MultiTreeNode extends SingleTreeNode {

    SingleTreeNode[] roots;
    SingleTreeNode[] currentLocation;

    public MultiTreeNode(MCTSPlayer player, SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state, Random rnd) {
        super(player, parent, actionToReach, state, rnd);
        if (parent != null)
            throw new AssertionError("MultiTreeNode should only be instantiated at the root of a tree");
        roots = new SingleTreeNode[state.getNPlayers()];
        roots[this.decisionPlayer] = this;
        currentLocation = new SingleTreeNode[state.getNPlayers()];
        currentLocation[this.decisionPlayer] = this;
    }

    /**
     * oneSearchIteration() implements the strategy for tree search (plus expansion, rollouts, backup and so on)
     * Its result is purely stored in the tree generated from root
     */
    protected void oneSearchIteration() {

        AbstractGameState currentState = this.openLoopState;  // this will have been set correctly before calling this method
        SingleTreeNode currentNode;

        double[] startingValues = IntStream.range(0, openLoopState.getNPlayers())
                .mapToDouble(i -> player.heuristic.evaluateState(currentState, i)).toArray();

        if (!currentState.isNotTerminal())
            return;

        // arrays to store the expansion actions taken, and whether we have expanded - indexed by player
        AbstractAction[] expansionAction = new AbstractAction[currentLocation.length];
        boolean[] nodeExpanded = new boolean[currentLocation.length];
        boolean[] maxDepthReached = new boolean[currentLocation.length];
        int rolloutActions = -1;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        do {

            int currentActor = currentState.getCurrentPlayer();
            if (roots[currentActor] == null) {
                // their first action in search; set a root for their tree (we link to the main root so that
                // we know who the overall player is, and keep this consistent across all nodes)
                SingleTreeNode pseudoRoot = new SingleTreeNode(player, root, null, currentState, rnd);
                roots[currentActor] = pseudoRoot;
                currentLocation[currentActor] = pseudoRoot;
            }
            if (!nodeExpanded[currentActor] && expansionAction[currentActor] != null) {
                // we now expand a node
                currentLocation[currentActor] = expandNode(expansionAction[currentActor], currentState.copy());
                // currentLocation now stores the last node in the tree for that player..so that we can back-propagate
                root.copyCount++;
                nodeExpanded[currentActor] = true;
            }

            // currentNode is the last node that this actor was at in their tree
            currentNode = currentLocation[currentActor];
            AbstractAction chosen;
            if (nodeExpanded[currentActor] || maxDepthReached[currentActor]) {
                // all actions after the expansion for a player are rollout actions
                // note that different players will enter rollout at different times, which is why
                // we cannot have a simple rollout() method as in SingleTree search
                AbstractPlayer agent = currentActor == decisionPlayer ? player.rolloutStrategy : player.getOpponentModel(currentActor);
                List<AbstractAction> availableActions = getForwardModel().computeAvailableActions(currentState);
                if (availableActions.isEmpty())
                    throw new AssertionError("We should always have something to choose from");
                else
                    chosen = agent.getAction(currentState, availableActions);
                if (decisionPlayer == currentActor)
                    rolloutActions = Math.max(1, rolloutActions + 1);

            } else {  // in the tree still for this player
                List<AbstractAction> unexpanded = currentNode.unexpandedActions();
                if (!unexpanded.isEmpty()) {
                    // We have an unexpanded action
                    if (expansionAction[currentActor] != null)
                        throw new AssertionError("We have already picked an expansion action for this player");
                    chosen = currentNode.expand(unexpanded);
                    expansionAction[currentActor] = chosen.copy();
                    // we will create the new node once we get back to a point when it is this player's action again
                } else {
                    chosen = currentNode.treePolicyAction();
                    // we move on to the next node in the tree (which should definitely exist
                    currentLocation[currentActor] = currentNode.nextNodeInTree(chosen);
                }
                // bit of a hack - other players will have their pseudo root at a 'depth' of 1 instead of 0
                if (currentLocation[currentActor].depth >= player.params.maxTreeDepth + (currentActor == decisionPlayer ? 0 : 1))
                    maxDepthReached[currentActor] = true;
            }

            // Closed_Loop is not compatible with MultiTree search
            // This is because given an action we do not know who the next player is until we take the action
            advance(currentState, chosen);

        } while (currentState.isNotTerminalForPlayer(decisionPlayer) && rolloutActions < player.params.rolloutLength);

        // Evaluate final state and return normalised score
        double[] finalValues = new double[state.getNPlayers()];

        for (int i = 0; i < finalValues.length; i++) {
            finalValues[i] = player.heuristic.evaluateState(currentState, i) - startingValues[i];
        }
        for (SingleTreeNode singleTreeNode : currentLocation) {
            if (singleTreeNode != null)
                singleTreeNode.backUp(finalValues);
        }

    }


}

