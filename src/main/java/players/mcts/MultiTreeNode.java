package players.mcts;

import core.AbstractGameState;
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

    @Override
    protected void preSearchSetup() {

    }


    /**
     * oneSearchIteration() implements the strategy for tree search (plus expansion, rollouts, backup and so on)
     * Its result is purely stored in the tree generated from root
     */
    protected void oneSearchIteration() {

        AbstractGameState currentState = this.openLoopState;  // this will have been set correctly before calling this method
        SingleTreeNode currentNode;
        boolean maxDepthReached = false;
        if (!currentState.isNotTerminal())
            return;

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

            currentNode = currentLocation[currentActor];
            if (currentNode == null) {
                // TODO rollout
            } else {
                List<AbstractAction> unexpanded = currentNode.unexpandedActions();
                if (!unexpanded.isEmpty()) {
                    // We have an unexpanded action. This line creates the new node, and copies the advanced state into it
                    SingleTreeNode nextNode =  currentNode.expand(unexpanded);
                    currentState = nextNode.openLoopState;
                } else {
                    // Move to next child given by UCT function
                    // TODO : No, this won't work. the nextNode is for the next player
                    // TODO: We need the content of the node to be the state after it has advanced to that player's turn again.
                    // We could either override the state once we know what it is...or hold off on creating the node until we know this
                    // this latter approach is what we do with SelfOnly via the advanceToOurNextAction() method
                    // This suggests that I want to break out the action selection piece from the node creation in SingleTreeNode
                    // and just use the former here!
                    SingleTreeNode nextNode = currentNode.nextNodeInTree();
                    currentState = nextNode.openLoopState;
                }
            }

        } while (currentState.isNotTerminal() && !maxDepthReached);


    }
}

