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
    protected String getKeyOf(AbstractGameState state) {
        double[] featureVector = params.MCGSStateFeatureVector.featureVector(state, state.getCurrentPlayer());
        return String.format("%d-%s", state.getCurrentPlayer(), Arrays.toString(featureVector));
    }

    private void addToTranspositionTable(MCGSNode node, AbstractGameState keyState) {
        String key = getKeyOf(keyState);
        MCGSNode graphRoot = (MCGSNode) root;
        if (graphRoot.transpositionMap.containsKey(key)) {
            throw new AssertionError("Unexpected?");
        }
        graphRoot.transpositionMap.put(key, node);
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
        String key = getKeyOf(nextState);
        if (graphRoot.transpositionMap.containsKey(key)) {
            MCGSNode newNode =  graphRoot.transpositionMap.get(key);
            newNode.setActionsFromOpenLoopState(nextState);
            return newNode;
        }
        return createChildNode(actionCopy, nextState);
    }

    @Override
    protected SingleTreeNode nextNodeInTree(AbstractAction actionChosen) {
        // we look up the node in the transposition table using the feature vector for the openLoopState
        // if we don;t find one, then we add a new node....this is to satisfy the main algorithm that we're
        // extending...the next action from this new node will inevitably be an expansion (as no actions have yet been
        // tried from it). This just means that we'll often add two nodes to the graph in one iteration.

        double[] featureVector = params.MCGSStateFeatureVector.featureVector(openLoopState, openLoopState.getCurrentPlayer());
        String key = String.format("%d-%s", openLoopState.getCurrentPlayer(), Arrays.toString(featureVector));
        MCGSNode nextNode = ((MCGSNode) root).transpositionMap.get(key);

        if (nextNode == null) {
            nextNode = (MCGSNode) createChildNode(actionChosen.copy(), openLoopState);
            ((MCGSNode) root).transpositionMap.put(key, nextNode);
        }
        nextNode.setActionsFromOpenLoopState(openLoopState);
        return nextNode;
    }

    @Override
    protected void advance(AbstractGameState gs, AbstractAction act, boolean inRollout) {
        // This is a convenient point to record the trajectory for use during the backup
        if (!inRollout) {
            // We only track this while in the tree (we could do the rollout as well, but at the overhead
            // of featureVector calculations
            MCGSNode mcgsRoot = (MCGSNode) root;
            double[] featureVector = params.MCGSStateFeatureVector.featureVector(gs, gs.getCurrentPlayer());
            String key = String.format("%d-%s", openLoopState.getCurrentPlayer(), Arrays.toString(featureVector));
            mcgsRoot.trajectory.add(key);
            // this means we should be adding one state to the trajectory every time we add an action to the rollout
        }
        super.advance(gs, act, inRollout);
    }


    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param baseResult - value of rollout to backup
     */
    protected void backUp(double[] baseResult) {
        MCGSNode nRoot = (MCGSNode) root;
        if (nRoot.trajectory.size() != nRoot.actionsInTree.size()) {
            throw new AssertionError("Trajectory and actionsInTree should be the same size " +
                    nRoot.trajectory.size() + " != " + nRoot.actionsInTree.size());
        }

        normaliseRewardsAfterIteration(baseResult);
        double[] result = processResultsForParanoidOrSelfOnly(baseResult);

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
