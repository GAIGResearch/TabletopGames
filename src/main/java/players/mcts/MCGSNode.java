package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.*;

public class MCGSNode extends SingleTreeNode {

    private final Map<String, MCGSNode> transpositionMap = new HashMap<>();
    public List<String> trajectory = new ArrayList<>();

    protected MCGSNode() {
    }

    @Override
    protected void instantiate(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state) {
        super.instantiate(parent, actionToReach, state);
        // the only additional instantiation we need to do is to add the state to the transposition table
        addToTranspositionTable(this, state);
    }

    private void addToTranspositionTable(MCGSNode node, AbstractGameState keyState) {
        String key = params.MCGSStateKey.getKey(keyState);
        MCGSNode graphRoot = (MCGSNode) root;
        if (graphRoot.transpositionMap.containsKey(key)) {
            throw new AssertionError("Unexpected?");
        }
        graphRoot.transpositionMap.put(key, node);
     //   System.out.println("Adding to transposition table: " + key);
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
        String key = params.MCGSStateKey.getKey(nextState);
        if (graphRoot.transpositionMap.containsKey(key)) {
            if (params.MCGSExpandAfterClash) {
                throw new AssertionError("Unexpected?");
            } else {
                MCGSNode retValue =  graphRoot.transpositionMap.get(key);
                retValue.setActionsFromOpenLoopState(openLoopState);
                return retValue;
            }
        }
        return createChildNode(actionCopy, nextState);
    }

    @Override
    protected SingleTreeNode nextNodeInTree(AbstractAction actionChosen) {
        // we look up the node in the transposition table using the feature vector for the openLoopState
        String key = params.MCGSStateKey.getKey(openLoopState);
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
            String key = params.MCGSStateKey.getKey(gs);
            mcgsRoot.trajectory.add(key);
//            System.out.println("Adding to trajectory: " + key);
        }
        super.advanceState(gs, act, inRollout);
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

        for (int i = 0; i < nRoot.trajectory.size(); i++) {
            String key = nRoot.trajectory.get(i);
            MCGSNode node = nRoot.transpositionMap.get(key);
            AbstractAction action = nRoot.actionsInTree.get(i).b;
            if (node == null) {
                throw new AssertionError("Node should not be null");
            }
            node.backUpSingleNode(action, result);
        }
        nRoot.trajectory.clear();
    }

    public Map<String, MCGSNode> getTranspositionMap() {
        return new HashMap<>(transpositionMap);
    }

}
