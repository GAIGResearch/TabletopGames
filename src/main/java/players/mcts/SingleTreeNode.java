package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.interfaces.IStatisticLogger;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.Information.Closed_Loop;
import static players.mcts.MCTSEnums.OpponentTreePolicy.*;
import static players.mcts.MCTSEnums.RolloutTermination.DEFAULT;
import static players.mcts.MCTSEnums.Strategies.MAST;
import static utilities.Utils.entropyOf;
import static utilities.Utils.noise;

public class SingleTreeNode {

    // State in this node (closed loop)
    protected AbstractGameState state;
    // State in this node (open loop - this is updated by onward trajectory....be very careful about using)
    protected AbstractGameState openLoopState;
    List<AbstractAction> actionsFromOpenLoopState = new ArrayList<>();
    Map<AbstractAction, Double> advantagesOfActionsFromOLS = new HashMap<>();
    // Parameters guiding the search
    protected MCTSParams params;
    protected AbstractForwardModel forwardModel;
    protected AbstractPlayer[] opponentModels;
    protected Random rnd;
    protected IStateHeuristic heuristic;
    protected IStateHeuristic opponentHeuristic;
    // Number of FM calls and State copies up until this node
    protected int fmCallsCount;
    protected int copyCount;
    protected int paranoidPlayer = -1;
    // Depth of this node
    int depth;
    // the id of the player who makes the decision at this node
    int decisionPlayer;
    private final Map<AbstractAction, Integer> nValidVisits = new HashMap<>();
    // Action taken to reach this node
    // In vanilla MCTS this will likely be an action taken by some other player (not the decisionPlayer at this node)
    protected AbstractAction actionToReach;
    // The total value of all trajectories through this node (one element per player)
    private double[] totValue;
    private double[] totSquares;
    // Number of visits to this node
    protected int nVisits;
    protected int rolloutActionsTaken;
    double highReward = Double.NEGATIVE_INFINITY;
    double lowReward = Double.POSITIVE_INFINITY;
    // Root node of tree
    SingleTreeNode root;
    // Parent of this node
    SingleTreeNode parent;
    // Children of this node. The value is an Array because we have to cater for the possibility that the next decision
    // could be by any player - each of which would transition to a different Node OpenLoop search. (Closed Loop will
    // only ever have one position in the array populated: and similarly if we are using a SelfOnly tree).
    Map<AbstractAction, SingleTreeNode[]> children = new HashMap<>();
    List<Map<AbstractAction, Pair<Integer, Double>>> MASTStatistics; // a list of one Map per player. Action -> (visits, totValue)
    ToDoubleBiFunction<AbstractAction, AbstractGameState> advantageFunction = (a, s) -> advantagesOfActionsFromOLS.getOrDefault(a, 0.0);
    ToDoubleBiFunction<AbstractAction, AbstractGameState> MASTFunction;
    // Total value of this node


    protected SingleTreeNode() {

    }

    // Called in tree expansion
    public static SingleTreeNode createRootNode(MCTSPlayer player, AbstractGameState state, Random rnd) {
        SingleTreeNode retValue = (player.params.opponentTreePolicy == OMA || player.params.opponentTreePolicy == OMA_All)
                ? new OMATreeNode() : new SingleTreeNode();
        retValue.decisionPlayer = state.getCurrentPlayer();
        retValue.params = player.params;
        retValue.forwardModel = player.getForwardModel();
        retValue.heuristic = player.heuristic;
        retValue.opponentHeuristic = player.opponentHeuristic;
        retValue.rnd = rnd;
        retValue.opponentModels = new AbstractPlayer[state.getNPlayers()];
        for (int p = 0; p < retValue.opponentModels.length; p++) {
            if (p == retValue.decisionPlayer)
                retValue.opponentModels[p] = player.rolloutStrategy;
            else
                retValue.opponentModels[p] = player.getOpponentModel(p);
        }
        // only root node maintains MAST statistics
        retValue.MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            retValue.MASTStatistics.add(new HashMap<>());
        retValue.MASTFunction = (a, s) -> {
            Map<AbstractAction, Pair<Integer, Double>> MAST = retValue.MASTStatistics.get(retValue.decisionPlayer);
            if (MAST.containsKey(a)) {
                Pair<Integer, Double> stats = MAST.get(a);
                return stats.b / (stats.a + retValue.params.epsilon);
            }
            return 0.0;
        };
        retValue.instantiate(null, null, state);
        return retValue;
    }

    public static SingleTreeNode createChildNode(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state) {
        SingleTreeNode retValue = (parent instanceof OMATreeNode) ? new OMATreeNode() : new SingleTreeNode();
        retValue.instantiate(parent, actionToReach, state);
        return retValue;
    }

    protected void instantiate(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state) {
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.params = root.params;
        this.heuristic = root.heuristic;
        this.opponentModels = root.opponentModels;
        this.forwardModel = root.forwardModel;
        this.rnd = root.rnd;

        decisionPlayer = terminalStateInSelfOnlyTree(state) ? parent.decisionPlayer : state.getCurrentPlayer();
        this.actionToReach = actionToReach;

        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }

        totValue = new double[state.getNPlayers()];
        totSquares = new double[state.getNPlayers()];
        if (params.information != Closed_Loop && (params.maintainMasterState || depth == 0)) {
            // if we're using open loop, then we need to make sure the reference state is never changed
            // however this is only used at the root - and we can switch the copy off for other nodes for performance
            // these master copies *are* required if we want to do something funky with the final tree, and gather
            // features from the nodes - if we are gathering Expert Iteration data or Learning an Advantage function
            root.copyCount++;
            this.state = state.copy();
        } else {
            this.state = state;
        }
        // then set up available actions, and set openLoopState = state
        setActionsFromOpenLoopState(state);

    }

    public AbstractGameState getState() {
        return state;
    }

    private boolean terminalStateInSelfOnlyTree(AbstractGameState state) {
        // we then have some exceptions
        if (params.opponentTreePolicy.selfOnlyTree && parent != null)
            return !state.isNotTerminalForPlayer(parent.decisionPlayer);
        return false;
    }

    protected void setActionsFromOpenLoopState(AbstractGameState actionState) {
        // TODO: Add a check here for the root node (only) that there is not change to the OpenLoopActions
        // TODO: However this is complicated by MultiTree MCTS, for which this invariant only holds for the acting player
        // so check the MCTSParams as well
        openLoopState = actionState;
        if (actionState.getCurrentPlayer() == this.decisionPlayer) {
            actionsFromOpenLoopState = forwardModel.computeAvailableActions(actionState);
            //      System.out.printf("Setting OLS actions for P%d (%d)%n%s%n", decisionPlayer, actionState.getCurrentPlayer(),
//                actionsFromOpenLoopState.stream().map(a -> "\t" + a.toString() + "\n").collect(joining()));
            if (params.expansionPolicy == MAST) {
                advantagesOfActionsFromOLS = actionsFromOpenLoopState.stream()
                        .collect(toMap(a -> a, a -> root.MASTFunction.applyAsDouble(a, actionState)));
            } else {
                if (params.advantageFunction != null)
                    advantagesOfActionsFromOLS = actionsFromOpenLoopState.stream()
                            .collect(toMap(a -> a, a -> params.advantageFunction.evaluateAction(a, actionState)));
            }
            for (AbstractAction action : actionsFromOpenLoopState) {
                if (!children.containsKey(action)) {
                    children.put(action, null); // mark a new node to be expanded
                    // This *does* rely on a good equals method being implemented for Actions
                }
            }
        }
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    public void mctsSearch(IStatisticLogger statsLogger) {

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;
        rolloutActionsTaken = 0;
        boolean stop = false;
        while (!stop) {
            switch (params.information) {
                case Closed_Loop:
                    openLoopState = state;
                    break;
                case Open_Loop:
                    openLoopState = state.copy();
                    copyCount++;
                    break;
                case Information_Set:
                    openLoopState = state.copy(decisionPlayer);
                    copyCount++;
                    break;
            }

            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            oneSearchIteration();

            // Finished iteration
            numIters++;
            //       System.out.printf("MCTS Iteration %d, timeLeft: %d\n", numIters, elapsedTimer.remainingTimeMillis());
            // Check stopping condition
            PlayerConstants budgetType = params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if (budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= params.budget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCallsCount > params.budget || numIters > params.budget;
            } else if (budgetType == BUDGET_COPY_CALLS) {
                stop = copyCount > params.budget || numIters > params.budget;
            } else if (budgetType == BUDGET_FMANDCOPY_CALLS) {
                stop = (copyCount + fmCallsCount) > params.budget || numIters > params.budget;
            }
        }

        if (statsLogger != null) {
            logTreeStatistics(statsLogger, numIters, elapsedTimer.elapsedMillis());
        }
    }

    /**
     * oneSearchIteration() implements the strategy for tree search (plus expansion, rollouts, backup and so on)
     * Its result is purely stored in the tree generated from root
     */
    protected void oneSearchIteration() {
        double[] startingValues = IntStream.range(0, openLoopState.getNPlayers())
                .mapToDouble(i -> heuristic.evaluateState(openLoopState, i)).toArray();

        List<Pair<Integer, AbstractAction>> treeActions = new ArrayList<>();
        SingleTreeNode selected = treePolicy(treeActions);
        // Monte carlo rollout: return value of MC rollout from the newly added node
        List<Pair<Integer, AbstractAction>> rolloutActions = new ArrayList<>();
        int lastActorInTree = treeActions.isEmpty() ? decisionPlayer : treeActions.get(treeActions.size() - 1).a;
        double[] delta = selected.rollOut(rolloutActions, startingValues, decisionPlayer, lastActorInTree);
        // Back up the value of the rollout through the tree
        rolloutActionsTaken += rolloutActions.size();
        selected.backUp(delta);
        updateMASTStatistics(treeActions, rolloutActions, delta);
    }

    protected void updateMASTStatistics(List<Pair<Integer, AbstractAction>> tree, List<Pair<Integer, AbstractAction>> rollout, double[] value) {
        if (params.useMAST) {
            List<Pair<Integer, AbstractAction>> MASTActions = new ArrayList<>();
            switch (params.MAST) {
                case Rollout:
                    MASTActions = rollout;
                    break;
                case Tree:
                    MASTActions = tree;
                    break;
                case Both:
                    MASTActions = rollout;
                    MASTActions.addAll(tree);
                    break;
            }
            root.MASTBackup(MASTActions, value);
        }
    }

    protected void logTreeStatistics(IStatisticLogger statsLogger, int numIters, long timeTaken) {
        Map<String, Object> stats = new LinkedHashMap<>();
        TreeStatistics treeStats = new TreeStatistics(root);
        stats.put("round", state.getTurnOrder().getRoundCounter());
        stats.put("turn", state.getTurnOrder().getTurnCounter());
        stats.put("turnOwner", state.getTurnOrder().getTurnOwner());
        double[] visitProportions = Arrays.stream(actionVisits()).asDoubleStream().map(d -> d / nVisits).toArray();
        stats.put("visitEntropy", entropyOf(visitProportions));
        stats.put("iterations", numIters);
        stats.put("fmCalls", fmCallsCount);
        stats.put("copyCalls", copyCount);
        stats.put("time", timeTaken);
        stats.put("totalNodes", treeStats.totalNodes);
        stats.put("leafNodes", treeStats.totalLeaves);
        stats.put("terminalNodes", treeStats.totalTerminalNodes);
        stats.put("maxDepth", treeStats.depthReached);
        stats.put("nActionsRoot", children.size());
        stats.put("nActionsTree", treeStats.meanActionsAtNode);
        stats.put("maxActionsAtNode", treeStats.maxActionsAtNode);
        OptionalInt maxVisits = Arrays.stream(actionVisits()).max();
        stats.put("maxVisitProportion", (maxVisits.isPresent() ? maxVisits.getAsInt() : 0) / (double) numIters);
        AbstractAction bestAction = bestAction();
        stats.put("bestAction", bestAction);
        stats.put("bestValue", this.actionTotValue(bestAction, decisionPlayer) / this.actionVisits(bestAction));
        stats.put("normalisedBestValue", Utils.normalise(this.actionTotValue(bestAction, decisionPlayer) / this.actionVisits(bestAction), lowReward, highReward));
        stats.put("lowReward", this.lowReward);
        stats.put("highReward", this.highReward);
        stats.put("rolloutActions", this.rolloutActionsTaken / numIters);
        statsLogger.record(stats);
    }

    /**
     * Uses plain java loop instead of streams for performance
     * (this is called often enough it can make a measurable difference)
     */
    protected int actionVisits(AbstractAction action) {
        int retValue = 0;
        SingleTreeNode[] nodes = children.get(action);
        if (nodes != null) {
            for (SingleTreeNode node : nodes) {
                if (node != null)
                    retValue += node.nVisits;
            }
        }
        return retValue;
    }

    private int validVisitsFor(AbstractAction action) {
        if (params.information == Closed_Loop)
            return nVisits;
        return nValidVisits.getOrDefault(action, 1);
    }

    /**
     * Uses plain java loop instead of streams for performance
     * (this is called often enough it can make a measurable difference)
     */
    protected double actionTotValue(AbstractAction action, int playerId) {
        double retValue = 0.0;
        SingleTreeNode[] nodes = children.get(action);
        if (nodes != null) {
            for (SingleTreeNode node : nodes) {
                if (node != null)
                    retValue += node.totValue[playerId];
            }
        }
        return retValue;
    }

    private double actionSquaredValue(AbstractAction action, int playerId) {
        double retValue = 0.0;
        SingleTreeNode[] nodes = children.get(action);
        if (nodes != null) {
            for (SingleTreeNode node : nodes) {
                if (node != null)
                    retValue += node.totSquares[playerId];
            }
        }
        return retValue;
    }

    /**
     * Uses only by TreeStatistics and bestAction() after mctsSearch()
     * For this reason not converted to old-style java loop as there would be no performance gain
     */
    int[] actionVisits() {
        return children.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(arr -> Arrays.stream(arr).filter(Objects::nonNull).mapToInt(n -> n.nVisits).sum())
                .toArray();
    }

    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    protected SingleTreeNode treePolicy(List<Pair<Integer, AbstractAction>> treeActions) {

        SingleTreeNode cur = this;
        int actingPlayer = cur.decisionPlayer;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.openLoopState.isNotTerminalForPlayer(actingPlayer) && cur.depth < params.maxTreeDepth && cur.actionsFromOpenLoopState.size() > 0) {
            List<AbstractAction> unexpanded = cur.unexpandedActions();
            if (!unexpanded.isEmpty()) {
                // We have an unexpanded action
                AbstractAction chosen = cur.expand(unexpanded);
                AbstractGameState nextState = cur.openLoopState;
                if (params.information == Closed_Loop) {
                    root.copyCount++;
                    nextState = nextState.copy();
                    // In Closed Loop why do we do this?
                    // Because OLS = state in this case, so we need to copy it before updating it and
                    // using it to populate a new node.
                }
                cur.advance(nextState, chosen);
                // then create the new node
                return cur.expandNode(chosen, nextState);
            } else {
                // Move to next child given by UCT function
                AbstractAction chosen = cur.treePolicyAction();
                if (params.information != Closed_Loop) {
                    // We do not need to copy the state, as we advance this as we descend the tree.
                    // In open loop we never re-use the state...the only purpose of storing it on the Node is
                    // to pick it up in the next uct() call as we descend the tree
                    cur.advance(cur.openLoopState, chosen);
                }
                cur = cur.nextNodeInTree(chosen);
                // else we keep cur, but will exit immediately
                treeActions.add(new Pair<>(actingPlayer, chosen));
            }
        }
        return cur;
    }

    protected List<AbstractAction> actionsToConsider(List<AbstractAction> allAvailable, int usedElsewhere) {
        if (!allAvailable.isEmpty() && params.progressiveWideningConstant >= 1.0) {
            int actionsToConsider = (int) Math.floor(params.progressiveWideningConstant * Math.pow(nVisits + 1, params.progressiveWideningExponent));
            actionsToConsider = Math.min(actionsToConsider - usedElsewhere, allAvailable.size());
            // takes account of the expanded actions
            if (actionsToConsider <= 0) return new ArrayList<>();
            // sort in advantage order (descending)
            allAvailable.sort(Comparator.comparingDouble(a -> -advantagesOfActionsFromOLS.getOrDefault(a, 0.0)));
            return allAvailable.subList(0, actionsToConsider);
        }
        return allAvailable;
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    protected List<AbstractAction> unexpandedActions() {
        // first cater for an edge case with progressive widening
        // where the expanded children may include available actions not in the current pruning width
        // this can occur where we have different available actions (actionsFromOpenLoopState) on each iteration
        List<AbstractAction> topActions = params.progressiveWideningConstant >= 1.0
                ? actionsToConsider(actionsFromOpenLoopState, 0)
                : actionsFromOpenLoopState;
        List<AbstractAction> allUnexpanded = topActions.stream().filter(a -> children.get(a) == null).collect(toList());
        return actionsToConsider(allUnexpanded, topActions.size() - allUnexpanded.size());
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    protected AbstractAction expand(List<AbstractAction> notChosen) {
        // the expansion order will use the actionValueFunction (if it exists, or the MAST order if specified)
        // else pick a random unchosen action

        Collections.shuffle(notChosen);

        AbstractAction chosen = null;

        ToDoubleBiFunction<AbstractAction, AbstractGameState> valueFunction = params.expansionPolicy == MAST ? MASTFunction : advantageFunction;
        if (valueFunction != null) {
            double bestValue = Double.NEGATIVE_INFINITY;
            for (AbstractAction action : notChosen) {
                double estimate = valueFunction.applyAsDouble(action, openLoopState);
                if (estimate > bestValue) {
                    bestValue = estimate;
                    chosen = action;
                }
            }
        } else {
            chosen = notChosen.get(0);
        }
        if (chosen == null)
            throw new AssertionError("We have somehow failed to pick an action to expand");

        return chosen;
    }

    protected SingleTreeNode expandNode(AbstractAction actionCopy, AbstractGameState nextState) {
        // then instantiate a new node
        int nextPlayer = params.opponentTreePolicy.selfOnlyTree ? decisionPlayer : nextState.getCurrentPlayer();
        SingleTreeNode tn = SingleTreeNode.createChildNode(this, actionCopy, nextState);
        SingleTreeNode[] nodeArray = new SingleTreeNode[nextState.getNPlayers()];
        nodeArray[nextPlayer] = tn; // we store this by id of the player who will take their turn next
        children.put(actionCopy, nodeArray);
        return tn;
    }


    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     * <p>
     * In some case Action is mutable, and will change state when advance() is called - so this method always copies
     * first for safety
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    protected void advance(AbstractGameState gs, AbstractAction act) {
        // we execute a copy(), because this can change the action, so we then don't find the node later!
        forwardModel.next(gs, act.copy());
        root.fmCallsCount++;
        if (params.opponentTreePolicy == SelfOnly && gs.getCurrentPlayer() != decisionPlayer)
            advanceToTurnOfPlayer(gs, decisionPlayer);
    }

    /**
     * Advance the game state to the next point at which it is the turn of the specified player.
     * This is used when we are only tracking our ourselves in the tree.
     *
     * @param id
     */
    protected void advanceToTurnOfPlayer(AbstractGameState gs, int id) {
        // For the moment we only have one opponent model - that of a random player
        while (gs.getCurrentPlayer() != id && gs.isNotTerminalForPlayer(id)) {
            //       AbstractGameState preGS = gs.copy();
            AbstractPlayer oppModel = opponentModels[gs.getCurrentPlayer()];
            List<AbstractAction> availableActions = forwardModel.computeAvailableActions(gs);
            if (availableActions.isEmpty())
                throw new AssertionError("Should always have at least one action possible...");
            AbstractAction action = oppModel.getAction(gs, availableActions);
            forwardModel.next(gs, action);
            root.fmCallsCount++;
        }
    }

    /**
     * Apply relevant policy to choose a child.
     *
     * @return - child node according to the tree policy
     */
    protected AbstractAction treePolicyAction() {

        if (params.opponentTreePolicy == SelfOnly && openLoopState.getCurrentPlayer() != decisionPlayer)
            throw new AssertionError("An error has occurred. SelfOnly should only call uct when we are moving.");

        List<AbstractAction> availableActions = actionsToConsider(actionsFromOpenLoopState, 0);
        if (availableActions.isEmpty())
            throw new AssertionError("We need to have at least one option");
        AbstractAction actionChosen;
        if (availableActions.size() == 1) {
            actionChosen = availableActions.get(0);
        } else {
            switch (params.treePolicy) {
                case UCB:
                case AlphaGo:
                case UCB_Tuned:
                    // These just vary on the form of the exploration term in a UCB algorithm
                    actionChosen = ucb(availableActions);
                    break;
                case EXP3:
                case RegretMatching:
                    // These construct a distribution over possible actions and then sample from it
                    actionChosen = sampleFromDistribution(availableActions);
                    break;
                default:
                    throw new AssertionError("Unknown treepolicy: " + params.treePolicy);
            }
        }

        return actionChosen;
    }

    protected SingleTreeNode nextNodeInTree(AbstractAction actionChosen) {
        // Only advance the state if this is open loop
        SingleTreeNode[] nodeArray = children.get(actionChosen);
        if (params.information == Closed_Loop) {
            // in this case we have determinism...there should just be a single child node in the array...so we get that
            Optional<SingleTreeNode> next = Arrays.stream(nodeArray).filter(Objects::nonNull).findFirst();
            if (next.isPresent()) {
                return next.get();
            } else {
                throw new AssertionError("We have no node to move to...");
            }
        } else {
            int nextPlayer = params.opponentTreePolicy.selfOnlyTree ? decisionPlayer : openLoopState.getCurrentPlayer();
            SingleTreeNode nextNode = nodeArray[nextPlayer];
            if (nextNode == null) {
                // need to create a new node - this is because we have a different player acting than expected
                if (params.opponentTreePolicy.selfOnlyTree)
                    throw new AssertionError("Not sure this should be possible though");
                nodeArray[nextPlayer] = SingleTreeNode.createChildNode(this, actionChosen.copy(), openLoopState);
                nextNode = nodeArray[nextPlayer];
            } else if (params.opponentTreePolicy.selfOnlyTree && nextNode.decisionPlayer != decisionPlayer) {
                nodeArray[nextPlayer] = SingleTreeNode.createChildNode(this, actionChosen.copy(), openLoopState);
                nextNode = nodeArray[nextPlayer];
            } else {
                // pick up the existing one, and set the state
                nextNode.setActionsFromOpenLoopState(openLoopState);
            }
            // we also need to check to see if there are any new actions on this transition
            return nextNode;
        }
    }

    private AbstractAction ucb(List<AbstractAction> availableActions) {
        // Find child with highest UCB value
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;

        double nodeValue = totValue[decisionPlayer] / nVisits;
        // nodeValue is the value of the state, V(s), and is used as a baseline when we use an Advantage function later

        for (AbstractAction action : availableActions) {
            SingleTreeNode[] childArray = children.get(action);
            if (childArray == null)
                throw new AssertionError("Should not be here");

            // Find child value
            double hvVal = actionTotValue(action, decisionPlayer);

            int actionVisits = actionVisits(action);
            double childValue = hvVal / (actionVisits + params.epsilon);

            // consider OMA term
            if (params.opponentTreePolicy == OMA_All || params.opponentTreePolicy == OMA) {
                OMATreeNode oma = ((OMATreeNode) this).OMAParent.orElse(null);
                if (oma != null) {
                    double beta = Math.sqrt(params.omaVisits / (double) (params.omaVisits + 3 * actionVisits));
                    // we need to find the action taken from the OMAParent
                    SingleTreeNode iteratingNode = this;
                    AbstractAction lastActionTaken;
                    do {
                        lastActionTaken = iteratingNode.actionToReach;
                        iteratingNode = iteratingNode.parent;
                        if (iteratingNode == null)
                            throw new AssertionError("Should always find OMA node before root");
                    } while (iteratingNode != oma);
                    OMATreeNode.OMAStats stats = oma.OMAChildren.get(lastActionTaken).get(action);
                    if (stats != null) {
                        double omaValue = stats.OMATotValue / stats.OMAVisits;
                        childValue = (1.0 - beta) * childValue + beta * omaValue;
                    }
                }
            }

            // consider any progressive bias term
            if (params.biasVisits > 0) {
                double beta = Math.sqrt(params.biasVisits / (double) (params.biasVisits + 3 * actionVisits));
                childValue = (1.0 - beta) * childValue + beta * (advantagesOfActionsFromOLS.getOrDefault(action, 0.0) + nodeValue);
            }

            if (params.normaliseRewards) {
                childValue = Utils.normalise(childValue, root.lowReward, root.highReward);
            }

            // default to standard UCB
            int effectiveTotalVisits = validVisitsFor(action) + 1;
            double explorationTerm = params.K * Math.sqrt(Math.log(effectiveTotalVisits) / (actionVisits + params.epsilon));
            // unless we are using a variant
            switch (params.treePolicy) {
                case AlphaGo:
                    explorationTerm = params.K * Math.sqrt(effectiveTotalVisits) / (actionVisits + 1.0);
                    break;
                case UCB_Tuned:
                    double range = root.highReward - root.lowReward;
                    if (range < 1e-6) range = 1e-6;
                    double meanSq = actionSquaredValue(action, decisionPlayer) / (actionVisits + params.epsilon);
                    double standardVar = 0.25;
                    if (params.normaliseRewards) {
                        // we also need to standardise the sum of squares to calculate the variance
                        meanSq = (meanSq
                                + root.lowReward * root.lowReward
                                - 2 * root.lowReward * actionTotValue(action, decisionPlayer) / (actionVisits + params.epsilon)
                        ) / (range * range);
                    } else {
                        // we need to modify the standard variance as it is not on a 0..1 basis (which is where 0.25 comes from)
                        standardVar = Math.sqrt(range / 2.0);
                    }
                    double variance = Math.max(0.0, meanSq - childValue * childValue);
                    double minTerm = Math.min(standardVar, variance + Math.sqrt(2 * Math.log(effectiveTotalVisits) / (actionVisits + params.epsilon)));
                    explorationTerm = params.K * Math.sqrt(Math.log(effectiveTotalVisits) / (actionVisits + params.epsilon) * minTerm);
                    break;
                default:
                    // keep default
            }

            // Find 'UCB' value
            double uctValue = 0;
            // Paranoid/SelfOnly control determines childValue here
            // If we are Paranoid, then the back-propagation will ensure that childValue is minus our score for opponent nodes
            uctValue = childValue + explorationTerm;

            // Apply small noise to break ties randomly
            uctValue = noise(uctValue, params.epsilon, rnd.nextDouble());
            if (Double.isNaN(uctValue))
                throw new AssertionError("Numeric error calculating uctValue");

            // Assign value
            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }
        }

        if (bestAction == null)
            throw new AssertionError("We have a null value in UCT : shouldn't really happen!");

        return bestAction;
    }

    public double exp3Value(AbstractAction action) {
        double actionValue = actionTotValue(action, decisionPlayer);
        int actionVisits = actionVisits(action);
        double meanActionValue = (actionValue / actionVisits);
        if (params.biasVisits > 0) {
            double beta = Math.sqrt(params.biasVisits / (double) (params.biasVisits + 3 * actionVisits));
            meanActionValue = (1.0 - beta) * meanActionValue + beta * advantagesOfActionsFromOLS.getOrDefault(action, 0.0);
        }
        // we then normalise to [0, 1], or we subtract the mean action value to get an advantage (and reduce risk of
        // NaN or Infinities when we exponentiate)
        if (params.normaliseRewards)
            meanActionValue = Utils.normalise(meanActionValue, root.lowReward, root.highReward);
        else
            meanActionValue = meanActionValue - (totValue[decisionPlayer] / nVisits);
        return Math.exp(meanActionValue);
    }

    public double rmValue(AbstractAction action) {
        double actionValue = actionTotValue(action, decisionPlayer);
        int actionVisits = actionVisits(action);
        if (params.biasVisits > 0) {
            double beta = Math.sqrt(params.biasVisits / (double) (params.biasVisits + 3 * actionVisits));
            actionValue = (1.0 - beta) * actionValue + beta * ((totValue[decisionPlayer] / nVisits) + advantagesOfActionsFromOLS.getOrDefault(action, 0.0));
        }
        // potential value is our estimate of our accumulated reward if we had always taken this action
        double potentialValue = actionValue * nVisits / actionVisits;
        double regret = potentialValue - totValue[decisionPlayer];
        return Math.max(0.0, regret);
    }

    private AbstractAction sampleFromDistribution(List<AbstractAction> availableActions) {
        // first we get a value for each of them
        Function<AbstractAction, Double> valueFn;
        switch (params.treePolicy) {
            case EXP3:
                valueFn = this::exp3Value;
                break;
            case RegretMatching:
                valueFn = this::rmValue;
                break;
            default:
                throw new AssertionError("Should not be any other options!");
        }

        Map<AbstractAction, Double> actionToValueMap = availableActions.stream().collect(toMap(Function.identity(), valueFn));

        // then we normalise to a pdf
        actionToValueMap = Utils.normaliseMap(actionToValueMap);
        // we then add on the exploration bonus
        double exploreBonus = params.exploreEpsilon / actionToValueMap.size();
        Map<AbstractAction, Double> probabilityOfSelection = actionToValueMap.entrySet().stream().collect(
                toMap(Map.Entry::getKey, e -> e.getValue() * (1.0 - params.exploreEpsilon) + exploreBonus));

        // then we sample a uniform variable in [0, 1] and ascend the cdf to find the selection
        double cdfSample = rnd.nextDouble();
        double cdf = 0.0;
        for (AbstractAction action : probabilityOfSelection.keySet()) {
            cdf += probabilityOfSelection.get(action);
            if (cdf >= cdfSample)
                return action;
        }
        throw new AssertionError("If we reach here, then something has gone wrong in the above code");
    }

    /**
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    protected double[] rollOut(List<Pair<Integer, AbstractAction>> rolloutActions, double[] startingValues, int decisionPlayer, int lastActor) {
        int rolloutDepth = 0; // counting from end of tree

        int roundAtStartOfRollout = openLoopState.getTurnOrder().getRoundCounter();

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = openLoopState;
        if (params.rolloutLength > 0 || params.rolloutTermination != DEFAULT) {
            // even if rollout length is zero, we may rollout a few actions to reach the end of our turn, or the start of our next turn
            if (params.information == Closed_Loop) {
                // the thinking here is that in openLoop we copy the state right at the root, and then use the forward
                // model at each action. Hence the current state on the node is the one we have been using up to now.
                /// Hence we do not need to copy it.
                rolloutState = state.copy();
                root.copyCount++;
            }

            while (!finishRollout(rolloutState, rolloutDepth, decisionPlayer, lastActor, roundAtStartOfRollout)) {
                List<AbstractAction> availableActions = forwardModel.computeAvailableActions(rolloutState);
                if (availableActions.isEmpty())
                    break;
                AbstractAction next = opponentModels[rolloutState.getCurrentPlayer()].getAction(rolloutState, availableActions);
                lastActor = rolloutState.getCurrentPlayer();
                rolloutActions.add(new Pair<>(lastActor, next));
                int startingFMCalls = root.fmCallsCount;
                advance(rolloutState, next);
                // rollout moves can be tracked by total forward model calls
                // as these may occur for opponent moves, which should count against our budget
                rolloutDepth += (root.fmCallsCount - startingFMCalls);
            }
        }
        // Evaluate final state and return normalised score
        double[] retValue = new double[rolloutState.getNPlayers()];

        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = heuristic.evaluateState(rolloutState, i) - startingValues[i];
        }
        return retValue;
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
     *
     * @param rollerState - current state
     * @param depth       - current depth
     * @return - true if rollout finished, false otherwise
     */
    private boolean finishRollout(AbstractGameState rollerState, int depth, int decisionPlayer, int lastActor, int roundAtStartOfRollout) {
        if (!rollerState.isNotTerminal())
            return true;
        int currentActor = rollerState.getCurrentPlayer();
        if (depth >= params.rolloutLength) {
            switch (params.rolloutTermination) {
                case DEFAULT:
                    return true;
                case END_TURN:
                    return lastActor == decisionPlayer && currentActor != decisionPlayer;
                case START_TURN:
                    return lastActor != decisionPlayer && currentActor == decisionPlayer;
                case END_ROUND:
                    return rollerState.getTurnOrder().getRoundCounter() != roundAtStartOfRollout;
            }
        }
        return false;
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    protected void backUp(double[] result) {
        SingleTreeNode n = this;
        double[] squaredResults = new double[result.length];
        for (int i = 0; i < result.length; i++)
            squaredResults[i] = result[i] * result[i];

        if (params.normaliseRewards || params.treePolicy == MCTSEnums.TreePolicy.UCB_Tuned) {
            DoubleSummaryStatistics stats = Arrays.stream(result).summaryStatistics();
            if (n.root.lowReward > stats.getMin())
                n.root.lowReward = stats.getMin();
            if (n.root.highReward < stats.getMax())
                n.root.highReward = stats.getMax();
        }
        while (n != null) {
            n.nVisits++;
            // Here we look at actionsFromOpenLoopState to see which ones were valid
            // when we passed through, and keep track of valid visits
            if (params.information != Closed_Loop)
                for (AbstractAction action : n.actionsFromOpenLoopState) {
                    if (!n.nValidVisits.containsKey(action))
                        n.nValidVisits.put(action, 1);
                    else
                        n.nValidVisits.put(action, n.nValidVisits.get(action) + 1);
                }
            switch (params.opponentTreePolicy) {
                case SelfOnly:
                    for (int j = 0; j < result.length; j++) {
                        n.totValue[j] += result[root.decisionPlayer];
                        n.totSquares[j] += squaredResults[root.decisionPlayer];
                    }
                    break;
                case Paranoid:
                case MultiTreeParanoid:
                    int paranoid = root.paranoidPlayer == -1 ? root.decisionPlayer : root.paranoidPlayer;
                    for (int j = 0; j < result.length; j++) {
                        if (j == paranoid) {
                            n.totValue[j] += result[paranoid];
                            n.totSquares[j] += squaredResults[paranoid];
                        } else {
                            n.totValue[j] -= result[paranoid];
                            n.totSquares[j] += squaredResults[paranoid];
                        }
                    }
                    break;
                case MaxN:
                case MultiTree:
                case OMA_All:
                case OMA:
                    for (int j = 0; j < result.length; j++) {
                        n.totValue[j] += result[j];
                        n.totSquares[j] += squaredResults[j];
                    }
                    break;
            }
            n = n.parent;
        }
    }


    protected void MASTBackup(List<Pair<Integer, AbstractAction>> rolloutActions, double[] delta) {
        for (Pair<Integer, AbstractAction> pair : rolloutActions) {
            AbstractAction action = pair.b;
            int player = pair.a;
            Pair<Integer, Double> stats = MASTStatistics.get(player).getOrDefault(action, new Pair<>(0, 0.0));
            stats.a++;  // visits
            stats.b += delta[player];   // value
            MASTStatistics.get(player).put(action.copy(), stats);
        }
    }


    /**
     * Calculates the best action from the root according to the selection policy
     *
     * @return - the best AbstractAction
     */
    public AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        MCTSEnums.SelectionPolicy policy = params.selectionPolicy;
        // check to see if all nodes have the same number of visits
        // if they do, then we use average score instead
        if (params.selectionPolicy == MCTSEnums.SelectionPolicy.ROBUST &&
                Arrays.stream(actionVisits()).boxed().collect(toSet()).size() == 1) {
            policy = MCTSEnums.SelectionPolicy.SIMPLE;
        }


        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                double childValue = actionVisits(action); // if ROBUST
                if (policy == MCTSEnums.SelectionPolicy.SIMPLE)
                    childValue = actionTotValue(action, decisionPlayer) / (actionVisits(action) + params.epsilon);

                // Apply small noise to break ties randomly
                childValue = noise(childValue, params.epsilon, rnd.nextDouble());

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

    public int getVisits() {
        return nVisits;
    }

    public int getDepth() {
        return depth;
    }

    /**
     * The returned array has one element per player. If the Tree type supports decisions by other players (MaxN)
     * then the relevant elements will provide this information.
     *
     * @return An array of the value fo the state from the perspective of each player
     */
    public double[] getTotValue() {
        return totValue;
    }

    public Map<AbstractAction, SingleTreeNode[]> getChildren() {
        return children;
    }

    public AbstractAction getActionToReach() {
        return actionToReach;
    }

    public int getActor() {
        return decisionPlayer;
    }

    public AbstractForwardModel getForwardModel() {
        return forwardModel;
    }

    /**
     * This returns a list of all nodes in the tree that do not match the specified Predicate
     *
     * @param allMatch
     * @return
     */
    public List<SingleTreeNode> nonMatchingNodes(Predicate<SingleTreeNode> allMatch) {
        return filterTree( n -> !allMatch.test(n));
    }
    /**
     * This returns a list of all nodes in the tree that match the specified Predicate
     *
     * @param allMatch
     * @return
     */
    public List<SingleTreeNode> filterTree(Predicate<SingleTreeNode> allMatch) {
        return allNodesInTree().stream().filter(allMatch).collect(toList());
    }

    public SingleTreeNode matchingParent(Predicate<SingleTreeNode> match) {
        if (parent == null || match.test(parent))
            return parent;
        return parent.matchingParent(match);
    }

    public SingleTreeNode getParent() {
        return parent;
    }

    public List<SingleTreeNode> allNodesInTree() {
        List<SingleTreeNode> retValue = new ArrayList<>();
        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        nodeQueue.add(this);
        while (!nodeQueue.isEmpty()) {
            SingleTreeNode node = nodeQueue.poll();
            retValue.add(node);
            nodeQueue.addAll(node.getChildren().values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .collect(toList()));
        }
        return retValue;
    }

    @Override
    public String toString() {
        // we return some interesting data on this node
        // child actions
        // visits and values for each
        StringBuilder retValue = new StringBuilder();
        String valueString = String.format("%.2f", totValue[decisionPlayer] / nVisits);
        if (params.opponentTreePolicy == MaxN) {
            valueString = Arrays.stream(totValue)
                    .mapToObj(v -> String.format("%.2f", v / nVisits))
                    .collect(joining(", "));
        }
        retValue.append(String.format("%d total visits, value %s, with %d children, depth %d, FMCalls %d: \n",
                nVisits, valueString, children.size(), depth, fmCallsCount));
        // sort all actions by visit count
        List<AbstractAction> sortedActions = children.keySet().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(a -> -actionVisits(a)))
                .collect(toList());

        for (AbstractAction action : sortedActions) {
            String actionName = action.toString();
            int actionVisits = actionVisits(action);
            int effectiveVisits = validVisitsFor(action);
            if (actionName.length() > 50)
                actionName = actionName.substring(0, 50);
            valueString = String.format("%.2f", actionTotValue(action, decisionPlayer) / actionVisits);
            if (params.opponentTreePolicy == MaxN) {
                valueString = IntStream.range(0, totValue.length)
                        .mapToObj(p -> String.format("%.2f", actionTotValue(action, p) / actionVisits))
                        .collect(joining(", "));
            }
            retValue.append(String.format("\t%-50s  visits: %d (%d)\tvalue %s\n", actionName, actionVisits, effectiveVisits, valueString));
        }

        if (!(root instanceof MultiTreeNode))
            retValue.append(new TreeStatistics(root));
        return retValue.toString();
    }
}
