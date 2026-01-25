package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MultiTreeNode is really a wrapper for SingleTreeNode when we are using MultiTree MCTS.
 * In this case we maintain a separate tree for each player as a form of opponent modelling.
 * We need to keep track of where we are in each of these distinct trees - and that is what MultiTreeNode does.
 * <p>
 * Only the root node will be a MultiTreeNode
 */
public class MultiTreeNode extends SingleTreeNode {

    boolean debug = false;
    SingleTreeNode[] roots;
    SingleTreeNode[] currentLocation;
    AbstractAction[] lastAction;
    boolean[] nodeExpanded;
    boolean[] maxDepthReached;
    MCTSPlayer mctsPlayer;

    public MultiTreeNode(MCTSPlayer player, AbstractGameState state, Random rnd) {
        this.decisionPlayer = player.getPlayerID();
        this.params = player.getParameters();
        this.forwardModel = player.getForwardModel();
        if (params.information == MCTSEnums.Information.Closed_Loop)
           params.information = MCTSEnums.Information.Open_Loop;
        // Closed Loop is not yet supported for MultiTree search
        // TODO: implement this (not too difficult, but some tricky bits as we shift from tree to rollout and back again)

        this.rnd = rnd;
        mctsPlayer = player;
        // only root node maintains MAST statistics
        MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            MASTStatistics.add(new HashMap<>());
        if (params.useMASTAsActionHeuristic) {
            params.actionHeuristic = new MASTActionHeuristic(params.MASTActionKey, params.MASTDefaultValue);
            ((MASTActionHeuristic) params.actionHeuristic).setMASTStats(MASTStatistics);
        }
        instantiate(null, null, state);

        roots = new SingleTreeNode[state.getNPlayers()];
        roots[this.decisionPlayer] = SingleTreeNode.createRootNode(player, state, rnd, player.getFactory());
        if (params.paranoid)
            roots[this.decisionPlayer].paranoidPlayer = decisionPlayer;
        currentLocation = new SingleTreeNode[state.getNPlayers()];
        currentLocation[this.decisionPlayer] = roots[decisionPlayer];
    }
    /**
     * oneSearchIteration() implements the strategy for tree search (plus expansion, rollouts, backup and so on)
     * Its result is purely stored in the tree generated from root
     */
    @Override
    public void oneSearchIteration() {

        if (debug)
            System.out.printf("%n%n%nStarting iteration...");
        AbstractGameState currentState = this.openLoopState;  // this will have been set correctly before calling this method
        SingleTreeNode currentNode;

        if (!currentState.isNotTerminal())
            return;

        // arrays to store the expansion actions taken, and whether we have expanded - indexed by player
        lastAction = new AbstractAction[currentLocation.length];
        nodeExpanded = new boolean[currentLocation.length];
        maxDepthReached = new boolean[currentLocation.length];
        System.arraycopy(roots, 0, currentLocation, 0, currentLocation.length);

        actionsInTree = new ArrayList<>();
        currentNodeTrajectory = new ArrayList<>();
        actionsInRollout = new ArrayList<>();
        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        do {
            if (debug)
                System.out.printf("P%d%n", currentState.getCurrentPlayer());
            int currentActor = currentState.getCurrentPlayer();
            if (roots[currentActor] == null) {
                // their first action in search; set a root for their tree
                SingleTreeNode pseudoRoot = SingleTreeNode.createRootNode(mctsPlayer, currentState.copy(), rnd, mctsPlayer.getFactory());
                pseudoRoot.decisionPlayer = currentActor;
                if (params.paranoid)
                    pseudoRoot.paranoidPlayer = decisionPlayer;
                roots[currentActor] = pseudoRoot;
                currentLocation[currentActor] = pseudoRoot;
            }
            updateCurrentLocation(currentActor, currentState);

            if (nodeExpanded[currentActor] || maxDepthReached[currentActor]) {
                // all actions after the expansion for a player are rollout actions
                // note that different players will enter rollout at different times, which is why
                // we cannot have a simple rollout() method as in SingleTree search
                AbstractPlayer agent = currentActor == decisionPlayer ? params.getRolloutStrategy() : params.getOpponentModel();
                List<AbstractAction> availableActions = forwardModel.computeAvailableActions(currentState, mctsPlayer.getParameters().actionSpace);
                if (availableActions.isEmpty())
                    throw new AssertionError("We should always have something to choose from");

                AbstractAction chosen = agent.getAction(currentState, availableActions);
                if (debug)
                    System.out.printf("Rollout action chosen for P%d - %s %n", currentActor, chosen);

                advanceState(currentState, chosen, true);
            } else {  // in the tree still for this player
                // currentNode is the last node that this actor was at in their tree
                currentNode = currentLocation[currentActor];
                currentNode.setActionsFromOpenLoopState(currentState);

                AbstractAction chosen = currentNode.treePolicyAction(true);
                lastAction[currentActor] = chosen;

                if (debug)
                    System.out.printf("Tree action chosen for P%d - %s %n", currentActor, chosen);
                advanceState(currentState, chosen, false);
                currentNodeTrajectory.add(currentNode);

                if (currentLocation[currentActor].depth >= params.maxTreeDepth)
                    maxDepthReached[currentActor] = true;
            }
            // we terminate if the game is over, or if we have exceeded our rollout count
        } while (currentState.isNotTerminal() && !finishRollout(currentState));

        // Evaluate final state and return normalised score
        double[] finalValues = new double[state.getNPlayers()];

        for (int i = 0; i < finalValues.length; i++) {
            finalValues[i] = params.heuristic.evaluateState(currentState, i);
        }
        for (int p = 0; p < roots.length; p++) {
            if (currentLocation[p] != null) { // the currentLocation will be null if the player has not acted at all (if, say they have been eliminated)
                // the full actions in tree and rollout are stored on the overall root
                // for each player-specific sub-tree we filter these to just their actions
                if (p != currentLocation[p].decisionPlayer)
                    throw new AssertionError("We should only be backing up for the decision player");

                currentLocation[p].root.actionsInTree = new ArrayList<>();
                currentLocation[p].root.currentNodeTrajectory = new ArrayList<>();
                for (int i = 0; i < actionsInTree.size(); i++) {
                    if (actionsInTree.get(i).a == p) {
                        currentLocation[p].root.actionsInTree.add(actionsInTree.get(i));
                        if (i < currentNodeTrajectory.size())
                            currentLocation[p].root.currentNodeTrajectory.add(currentNodeTrajectory.get(i));
                    }
                }
                currentLocation[p].backUp(finalValues);
            }
        }
        rolloutActionsTaken += actionsInRollout.size();
        root.updateMASTStatistics(actionsInTree, actionsInRollout, finalValues);
    }


    private void updateCurrentLocation(int playerId, AbstractGameState state) {
        if (lastAction[playerId] != null && !nodeExpanded[playerId]) { // we have a previous action and are not yet in rollout
            // nextNodeInTree returns null if this is an expansion node
            SingleTreeNode newLocation = currentLocation[playerId].nextNodeInTree(lastAction[playerId]);
            if (newLocation == null) {
                // currentLocation now stores the last node in the tree for that player..so that we can back-propagate
                nodeExpanded[playerId] = true;
                if (debug)
                    System.out.printf("Node expanded for P%d : %s %n", playerId, lastAction[playerId]);
                newLocation = currentLocation[playerId].expandNode(lastAction[playerId], state);
            }
            currentLocation[playerId] = newLocation;
        }
        lastAction[playerId] = null;
        // we reset this as we have processed the action (required so that when we terminate the loop of
        // tree/rollout policies we know what remains to be cleared up

    }

    @Override
    public AbstractAction bestAction() {
        return roots[decisionPlayer].bestAction();
    }

    public SingleTreeNode getRoot(int player) {
        return roots[player];
    }

    public void resetRoot(int player) {
        roots[player] = null;
    }

}

