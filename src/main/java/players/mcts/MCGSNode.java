package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.*;

public class MCGSNode extends SingleTreeNode {

    private Map<Object, MCGSNode> transpositionMap = new HashMap<>();
    public List<Object> trajectory = new ArrayList<>();
    protected List<Object> keysTorRemove = new ArrayList<>();

    protected MCGSNode() {
    }


    @Override
    protected void instantiate(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state) {
        super.instantiate(parent, actionToReach, state);
        // the only additional instantiation we need to do is to add the state to the transposition table
        addToTranspositionTable(this, state);
    }

    private void addToTranspositionTable(MCGSNode node, AbstractGameState keyState) {
        Object key = params.MCGSStateKey.getKey(keyState);
        MCGSNode graphRoot = (MCGSNode) root;
        if (graphRoot.transpositionMap.containsKey(key)) {
            if (graphRoot.transpositionMap.get(key) != node) {
                throw new AssertionError("We have found a clash in the transposition table for key: " + key +
                        ". We are expanding a new node, and this key already exists in the transposition table, but it is not the same node.");
            }
        } else {
            graphRoot.transpositionMap.put(key, node);
        }
    }

    /**
     * Expands the node by creating a new child node for the action taken and adding to the tree.
     *
     * @return - new child node.
     */
    protected SingleTreeNode expandNode(AbstractAction actionCopy, AbstractGameState nextState) {
        // this marks the first time we have taken an action that has not been tried before from this node
        // in the main algorithm it moves us into the 'rollout' phase

        // we create the new node here; so that the backup does not create new nodes (which is in line with the main MCTS algorithm).
        // this enforces (for the moment) the rule that each iteration adds one new node.
        MCGSNode graphRoot = (MCGSNode) root;
        Object key = params.MCGSStateKey.getKey(nextState);
        if (graphRoot.transpositionMap.containsKey(key)) {
            if (params.MCGSExpandAfterClash) {
                throw new AssertionError("Unexpected?");
            } else {
                MCGSNode retValue = graphRoot.transpositionMap.get(key);
                retValue.setActionsFromOpenLoopState(openLoopState);
                return retValue;
            }
        }
        return createChildNode(actionCopy, nextState);
    }

    @Override
    protected SingleTreeNode nextNodeInTree(AbstractAction actionChosen) {
        // we look up the node in the transposition table using the feature vector for the openLoopState
        Object key = params.MCGSStateKey.getKey(openLoopState);
        MCGSNode nextNode = ((MCGSNode) root).transpositionMap.get(key);

        if (nextNode != null) {
            if (actionValues.get(actionChosen).nVisits == 0) {
                root.nodeClash++;
                if (!params.MCGSExpandAfterClash) {
                    // we then return null so we rollout from this point
                    return null;
                }
            }
            nextNode.setActionsFromOpenLoopState(openLoopState);
        }

        return nextNode;
    }

    @Override
    protected void advanceState(AbstractGameState gs, AbstractAction act, boolean inRollout) {
        // This is a convenient point to record the trajectory for use during the backup
        if (!inRollout) {
            // We only track this while in the tree (we could do the rollout as well, but at the overhead
            // of featureVector calculations
            MCGSNode mcgsRoot = (MCGSNode) root;
            Object key = params.MCGSStateKey.getKey(gs);
            // special case at root when we *expect* the key to be different on several iterations through
            // because we are redeterminising from a perspective other than the decisionPlayer
            if (this == mcgsRoot && mcgsRoot.trajectory.isEmpty() && decisionPlayer != redeterminisationPlayer && redeterminisationPlayer != -1) {
                key = params.MCGSStateKey.getKey(gs, redeterminisationPlayer);
            }
            mcgsRoot.trajectory.add(key);
//            System.out.println("Adding to trajectory: " + key);
        }
        super.advanceState(gs, act, inRollout);
    }

    @Override
    protected void resetDepth(SingleTreeNode unusedArgument) {
        int depthDelta = depth;
        root = this;
        keysTorRemove = new ArrayList<>();
        for (Object key : transpositionMap.keySet()) {
            MCGSNode node = transpositionMap.get(key);
            node.depth -= depthDelta;
            if (node.depth < 0) {
                keysTorRemove.add(key);
            }
            node.root = this;
        }
        keysTorRemove.forEach(transpositionMap::remove);
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param delta - value of rollout to backup
     */
    protected void backUp(double[] delta) {
        normaliseRewardsAfterIteration(delta);
        double[] result = processResultsForParanoidOrSelfOnly(delta);
        MCGSNode nRoot = (MCGSNode) root;
        // trajectory is the sequence of state representations that we have passed through
        if (nRoot.trajectory.size() != nRoot.actionsInTree.size()) {
            throw new AssertionError("Trajectory and actionsInTree should be the same size " +
                    nRoot.trajectory.size() + " != " + nRoot.actionsInTree.size());
        }

        for (int i = nRoot.trajectory.size() - 1; i >= 0; i--) {
            Object key = nRoot.trajectory.get(i);
            MCGSNode node = nRoot.transpositionMap.get(key);
            AbstractAction action = nRoot.actionsInTree.get(i).b;
            if (node == null) {
                throw new AssertionError("Node should not be null");
            }
            result = node.backUpSingleNode(action, result);
        }
        nRoot.trajectory.clear();
    }

    public Map<Object, MCGSNode> getTranspositionMap() {
        return transpositionMap;
    }

    public void setTranspositionMap(Map<Object, MCGSNode> transposition) {
        transpositionMap = transposition;
    }

}
