package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Pair;

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
    int decisionPlayer;
    SingleTreeNode[] roots;
    SingleTreeNode[] currentLocation;
    AbstractAction[] expansionAction;
    boolean[] nodeExpanded;
    boolean[] maxDepthReached;
    MCTSPlayer mctsPlayer;

    public MultiTreeNode(MCTSPlayer player, AbstractGameState state, Random rnd) {
        if (player.params.information == MCTSEnums.Information.Closed_Loop)
            player.params.information = MCTSEnums.Information.Open_Loop;
        // Closed Loop is not yet supported for MultiTree search
        // TODO: implement this (not too difficult, but some tricky bits as we shift from tree to rollout and back again)
        this.decisionPlayer = state.getCurrentPlayer();
        this.params = player.params;
        this.forwardModel = player.getForwardModel();
        this.heuristic = player.heuristic;
        this.rnd = rnd;
        this.opponentModels = new AbstractPlayer[state.getNPlayers()];
        mctsPlayer = player;
        for (int p = 0; p < opponentModels.length; p++) {
            if (p == decisionPlayer)
                opponentModels[p] = player.rolloutStrategy;
            else
                opponentModels[p] = player.getOpponentModel(p);
        }
        // only root node maintains MAST statistics
        MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            MASTStatistics.add(new HashMap<>());
        instantiate(null, null, state);

        roots = new SingleTreeNode[state.getNPlayers()];
        roots[this.decisionPlayer] = SingleTreeNode.createRootNode(player, state, rnd);
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

        double[] startingValues = IntStream.range(0, openLoopState.getNPlayers())
                .mapToDouble(i -> heuristic.evaluateState(currentState, i)).toArray();

        if (!currentState.isNotTerminal())
            return;

        // arrays to store the expansion actions taken, and whether we have expanded - indexed by player
        expansionAction = new AbstractAction[currentLocation.length];
        nodeExpanded = new boolean[currentLocation.length];
        maxDepthReached = new boolean[currentLocation.length];
        for (int i = 0; i < currentLocation.length; i++)
            currentLocation[i] = roots[i];

        List<Pair<Integer, AbstractAction>> actionsInTree = new ArrayList<>();
        List<Pair<Integer, AbstractAction>> actionsInRollout = new ArrayList<>();

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        do {
            if (debug)
                System.out.printf("P%d%n", currentState.getCurrentPlayer());
            int currentActor = currentState.getCurrentPlayer();
            if (roots[currentActor] == null) {
                // their first action in search; set a root for their tree
                SingleTreeNode pseudoRoot = SingleTreeNode.createRootNode(mctsPlayer, currentState.copy(), rnd);
                pseudoRoot.decisionPlayer = currentActor;
                roots[currentActor] = pseudoRoot;
                currentLocation[currentActor] = pseudoRoot;
            }
            if (!nodeExpanded[currentActor] && expansionAction[currentActor] != null) {
                expandNode(currentActor, currentState);
            }

            // currentNode is the last node that this actor was at in their tree
            currentNode = currentLocation[currentActor];
            currentNode.setActionsFromOpenLoopState(currentState);
            if (nodeExpanded[currentActor] || maxDepthReached[currentActor]) {
                // all actions after the expansion for a player are rollout actions
                // note that different players will enter rollout at different times, which is why
                // we cannot have a simple rollout() method as in SingleTree search
                AbstractPlayer agent = opponentModels[currentActor];
                List<AbstractAction> availableActions = forwardModel.computeAvailableActions(currentState);
                if (availableActions.isEmpty())
                    throw new AssertionError("We should always have something to choose from");

                AbstractAction chosen = agent.getAction(currentState, availableActions);
                actionsInRollout.add(new Pair<>(currentActor, chosen));
                if (debug)
                    System.out.printf("Rollout action chosen for P%d - %s %n", currentActor, chosen);

                advance(currentState, chosen);
            } else {  // in the tree still for this player
                List<AbstractAction> unexpanded = currentNode.unexpandedActions();
                AbstractAction chosen;
                if (!unexpanded.isEmpty()) {
                    // We have an unexpanded action
                    if (expansionAction[currentActor] != null)
                        throw new AssertionError("We have already picked an expansion action for this player");
                    chosen = currentNode.expand(unexpanded);
                    expansionAction[currentActor] = chosen.copy();
                    if (debug)
                        System.out.printf("Expansion action chosen for P%d - %s %n", currentActor, chosen);
                    advance(currentState, chosen);
                    // we will create the new node once we get back to a point when it is this player's action again
                } else {
                    chosen = currentNode.treePolicyAction();
                    // we move on to the next node in the tree (which should definitely exist
                    if (debug)
                        System.out.printf("Tree action chosen for P%d - %s %n", currentActor, chosen);
                    advance(currentState, chosen.copy());
                    // we execute a copy(), because this can change the action, so we then don't find the node!
                    currentLocation[currentActor] = currentNode.nextNodeInTree(chosen);
                }
                actionsInTree.add(new Pair<>(currentActor, chosen));
                if (currentLocation[currentActor].depth >= params.maxTreeDepth)
                    maxDepthReached[currentActor] = true;
            }
        } while (currentState.isNotTerminal() && !(nodeExpanded[decisionPlayer] && actionsInRollout.size() >= params.rolloutLength));

        for (int i = 0; i < nodeExpanded.length; i++) {
            if (!nodeExpanded[i] && expansionAction[i] != null)
                // in this case we will never go back to this player, and never insert the node
                expandNode(i, currentState);
        }

        // Evaluate final state and return normalised score
        double[] finalValues = new double[state.getNPlayers()];

        for (int i = 0; i < finalValues.length; i++) {
            finalValues[i] = heuristic.evaluateState(currentState, i) - startingValues[i];
        }
        for (SingleTreeNode singleTreeNode : currentLocation) {
            if (singleTreeNode != null)
                singleTreeNode.backUp(finalValues);
        }
        root.updateMASTStatistics(actionsInTree, actionsInRollout, finalValues);
    }

    private void expandNode(int currentActor, AbstractGameState currentState) {
        // we now expand a node
        currentLocation[currentActor] = currentLocation[currentActor].expandNode(expansionAction[currentActor], currentState.copy());
        // currentLocation now stores the last node in the tree for that player..so that we can back-propagate
        root.copyCount++;
        nodeExpanded[currentActor] = true;
        if (debug)
            System.out.printf("Node expanded for P%d : %s %n", currentActor, currentLocation[currentActor].unexpandedActions().stream().map(Objects::toString).collect(Collectors.joining()));

    }

    @Override
    public AbstractAction bestAction() {
        return roots[decisionPlayer].bestAction();
    }

    public SingleTreeNode getRoot(int player) {
        return roots[player];
    }
}

