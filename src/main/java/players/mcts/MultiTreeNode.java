package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utilities.Utils.entropyOf;

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
    AbstractAction[] lastAction;
    boolean[] nodeExpanded;
    boolean[] expansionActionTaken;
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
        MASTFunction = (a, s) -> {
            Map<Object, Pair<Integer, Double>> MAST = MASTStatistics.get(decisionPlayer);
            if (MAST.containsKey(a)) {
                Pair<Integer, Double> stats = MAST.get(a);
                return stats.b / (stats.a + params.epsilon);
            }
            return 0.0;
        };
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

        double[] startingValues = IntStream.range(0, openLoopState.getNPlayers())
                .mapToDouble(i -> heuristic.evaluateState(currentState, i)).toArray();

        if (!currentState.isNotTerminal())
            return;

        // arrays to store the expansion actions taken, and whether we have expanded - indexed by player
        lastAction = new AbstractAction[currentLocation.length];
        nodeExpanded = new boolean[currentLocation.length];
        expansionActionTaken = new boolean[currentLocation.length];
        maxDepthReached = new boolean[currentLocation.length];
        for (int i = 0; i < currentLocation.length; i++)
            currentLocation[i] = roots[i];

        actionsInTree = new ArrayList<>();
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
                AbstractPlayer agent = opponentModels[currentActor];
                List<AbstractAction> availableActions = forwardModel.computeAvailableActions(currentState);
                if (availableActions.isEmpty())
                    throw new AssertionError("We should always have something to choose from");

                AbstractAction chosen = agent.getAction(currentState, availableActions);
                actionsInRollout.add(new Pair<>(currentActor, chosen));
                if (debug)
                    System.out.printf("Rollout action chosen for P%d - %s %n", currentActor, chosen);

                advance(currentState, chosen, true);
            } else {  // in the tree still for this player
                // currentNode is the last node that this actor was at in their tree
                currentNode = currentLocation[currentActor];
                currentNode.setActionsFromOpenLoopState(currentState);
                List<AbstractAction> unexpanded = currentNode.unexpandedActions();
                AbstractAction chosen;
                if (!unexpanded.isEmpty()) {
                    // We have an unexpanded action
                    if (expansionActionTaken[currentActor])
                        throw new AssertionError("We have already picked an expansion action for this player");
                    chosen = currentNode.expand(unexpanded);
                    lastAction[currentActor] = chosen;
                    expansionActionTaken[currentActor] = true;
                    if (debug)
                        System.out.printf("Expansion action chosen for P%d - %s %n", currentActor, chosen);
                    advance(currentState, chosen, false);
                    // we will create the new node once we get back to a point when it is this player's action again
                } else {
                    chosen = currentNode.treePolicyAction(true);
                    lastAction[currentActor] = chosen;
                    if (debug)
                        System.out.printf("Tree action chosen for P%d - %s %n", currentActor, chosen);
                    advance(currentState, chosen, false);
                }
                actionsInTree.add(new Pair<>(currentActor, chosen));
                if (currentLocation[currentActor].depth >= params.maxTreeDepth)
                    maxDepthReached[currentActor] = true;
            }
            // we terminate if the game is over, or if we have exceeded our rollout count AND we have either expanded a node
            // for the decisionPlayer, or they are out of the game (in which case they will never get to expand a node)
        } while (currentState.isNotTerminal() &&
                !(actionsInRollout.size() >= params.rolloutLength &&
                        (maxDepthReached[decisionPlayer] || nodeExpanded[decisionPlayer] || !currentState.isNotTerminalForPlayer(decisionPlayer))));

        for (int i = 0; i < nodeExpanded.length; i++) {
            updateCurrentLocation(i, currentState);
        }

        // Evaluate final state and return normalised score
        double[] finalValues = new double[state.getNPlayers()];

        for (int i = 0; i < finalValues.length; i++) {
            finalValues[i] = heuristic.evaluateState(currentState, i) - (params.nodesStoreScoreDelta ? startingValues[i] : 0);
        }
        for (SingleTreeNode singleTreeNode : currentLocation) {
            if (singleTreeNode != null)
                singleTreeNode.backUp(finalValues);
        }
        rolloutActionsTaken += actionsInRollout.size();
        root.updateMASTStatistics(actionsInTree, actionsInRollout, finalValues);
    }

    private void expandNode(int currentActor, AbstractGameState currentState) {
        // we now expand a node
        currentLocation[currentActor] = currentLocation[currentActor].expandNode(lastAction[currentActor], currentState);
        // currentLocation now stores the last node in the tree for that player..so that we can back-propagate
        nodeExpanded[currentActor] = true;
        if (debug)
            System.out.printf("Node expanded for P%d : %s %n", currentActor, currentLocation[currentActor].unexpandedActions().stream().map(Objects::toString).collect(Collectors.joining()));
    }

    private void updateCurrentLocation(int playerId, AbstractGameState state) {
        if (lastAction[playerId] != null && !nodeExpanded[playerId]) { // we have a previous action and are not yet in rollout
            if (expansionActionTaken[playerId]) {
                expandNode(playerId, state);
            } else {
                currentLocation[playerId] = currentLocation[playerId].nextNodeInTree(lastAction[playerId]);
            }
            lastAction[playerId] = null;
            // we reset this as we have processed the action (required so that when we terminate the loop of
            // tree/rollout policies we know what remains to be cleared up
        }
    }

    @Override
    public AbstractAction bestAction() {
        return roots[decisionPlayer].bestAction();
    }

    public SingleTreeNode getRoot(int player) {
        return roots[player];
    }


    protected void logTreeStatistics(IStatisticLogger statsLogger, int numIters, long timeTaken) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("round", state.getRoundCounter());
        stats.put("turn", state.getTurnCounter());
        stats.put("turnOwner", state.getCurrentPlayer());
        stats.put("iterations", numIters);
        stats.put("rolloutActions", rolloutActionsTaken / numIters);
        stats.put("time", timeTaken);
        int validRoots = (int) Arrays.stream(roots).filter(Objects::nonNull).count();
        for (SingleTreeNode node : roots) {
            if (node == null) continue;
            boolean mainPlayer = node.decisionPlayer == this.decisionPlayer;
            TreeStatistics treeStats = new TreeStatistics(node);

            int copy = node.copyCount + (node == roots[decisionPlayer] ? this.copyCount : 0);
            int fm = node.fmCallsCount + (node == roots[decisionPlayer] ? this.fmCallsCount : 0);
            double multiplier = mainPlayer ? 1 : 1.0 / (validRoots - 1.0);
            String suffix = mainPlayer ? "-main" : "-other";
            BiFunction<Object, Object, Double> addFn = (v1, v2) -> ((double) v1 + ((double) v2));

            stats.merge("copyCalls" + suffix, copy * multiplier, addFn);
            stats.merge("fmCalls" + suffix, fm * multiplier, addFn);
            stats.merge("totalNodes" + suffix, treeStats.totalNodes * multiplier, addFn);
            stats.merge("leafNodes" + suffix, treeStats.totalLeaves * multiplier, addFn);
            stats.merge("terminalNodes" + suffix, treeStats.totalTerminalNodes * multiplier, addFn);
            stats.merge("maxDepth" + suffix, treeStats.depthReached * multiplier, addFn);
            stats.merge("nActionsRoot" + suffix, node.children.size() * multiplier, addFn);
            stats.merge("nActionsTree" + suffix, treeStats.meanActionsAtNode * multiplier, addFn);
            stats.merge("maxActionsAtNode" + suffix, treeStats.maxActionsAtNode * multiplier, addFn);

            OptionalInt maxVisits = Arrays.stream(node.actionVisits()).max();
            stats.merge("maxVisitProportion" + suffix, (maxVisits.isPresent() ? maxVisits.getAsInt() : 0) / (double) numIters * multiplier, addFn);
            double[] visitProportions = Arrays.stream(node.actionVisits()).asDoubleStream().map(d -> d / node.getVisits()).toArray();
            stats.merge("visitEntropy" + suffix, entropyOf(visitProportions) * multiplier, addFn);
            AbstractAction bestAction = node.bestAction();
            stats.put("bestAction" + suffix, bestAction);
            stats.put("bestValue" + suffix, node.actionTotValue(bestAction, node.decisionPlayer) / node.actionVisits(bestAction));
            stats.put("normalisedBestValue" + suffix, Utils.normalise(node.actionTotValue(bestAction, node.decisionPlayer) / node.actionVisits(bestAction), node.lowReward, node.highReward));
            stats.put("lowReward" + suffix, node.lowReward);
            stats.put("highReward" + suffix, node.highReward);
        }
        statsLogger.record(stats);
    }

}

