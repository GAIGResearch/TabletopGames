package players.mcts;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import players.PlayerConstants;
import utilities.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.Information.Closed_Loop;
import static players.mcts.MCTSEnums.OpponentTreePolicy.*;
import static players.mcts.MCTSEnums.RolloutTermination.EXACT;
import static players.mcts.MCTSEnums.SelectionPolicy.*;
import static players.mcts.MCTSEnums.TreePolicy.*;
import static utilities.Utils.*;

public class SingleTreeNode {

    //  private final Map<AbstractAction, Integer> nValidVisits = new HashMap<>();
    // State in this node (closed loop)
    protected AbstractGameState state;
    // State in this node (open loop - this is updated by onward trajectory....be very careful about using)
    protected AbstractGameState openLoopState;
    // Parameters guiding the search
    protected MCTSParams params;
    protected AbstractForwardModel forwardModel;
    protected Random rnd;
    // Number of FM calls and State copies up until this node
    protected int fmCallsCount;
    protected int copyCount;
    protected int paranoidPlayer = -1;
    // Action taken to reach this node
    // In vanilla MCTS this will likely be an action taken by some other player (not the decisionPlayer at this node)
    protected AbstractAction actionToReach;
    // Number of visits to this node
    protected int nVisits, inheritedVisits;
    protected int rolloutActionsTaken;
    // variables to track rollout - these were originally local in rollout(); but
    // having them on the node reduces verbiage in passing to advance() to check rollout termination in some edge cases
    // (specifically when using SelfOnly trees, with START/END_TURN/ROUND rollout termination conditions
    protected int lastActorInRollout, lastTurnInRollout, lastRoundInRollout, turnAtStartOfRollout, roundAtStartOfRollout;
    List<AbstractAction> actionsFromOpenLoopState = new ArrayList<>();
    Map<AbstractAction, Double> actionValueEstimates = new HashMap<>();
    Map<AbstractAction, Double> actionPDFEstimates = new HashMap<>();
    // Depth of this node
    protected int depth;
    // the id of the player who makes the decision at this node
    protected int decisionPlayer;
    protected int redeterminisationPlayer = -1;
    protected int round, turn, turnOwner;
    boolean terminalNode;
    double timeTaken;
    double initialisationTimeTaken;
    protected double highReward = Double.NEGATIVE_INFINITY;
    protected double lowReward = Double.POSITIVE_INFINITY;
    protected Map<AbstractAction, Double> regretMatchingAverage = new HashMap<>();
    protected int nodeClash;
    // Root node of tree
    protected SingleTreeNode root;
    // Parent of this node
    SingleTreeNode parent;
    // Children of this node. The value is an Array because we have to cater for the possibility that the next decision
    // could be by any player - each of which would transition to a different Node OpenLoop search. (Closed Loop will
    // only ever have one position in the array populated: and similarly if we are using a SelfOnly tree).
    Map<AbstractAction, SingleTreeNode[]> children = new LinkedHashMap<>();
    Map<AbstractAction, ActionStats> actionValues = new HashMap<>();
    List<Map<Object, Pair<Integer, Double>>> MASTStatistics; // a list of one Map per player. Action -> (visits, totValue)
    // ToDoubleBiFunction<AbstractAction, AbstractGameState> MASTFunction;
    // The total value of all trajectories through this node (one element per player)
    private Supplier<? extends SingleTreeNode> factory;
    // Total value of this node
    protected List<SingleTreeNode> currentNodeTrajectory;
    protected List<Pair<Integer, AbstractAction>> actionsInTree;
    List<Pair<Integer, AbstractAction>> actionsInRollout;

    protected SingleTreeNode() {
    }

    // Called in tree expansion
    public static SingleTreeNode createRootNode(MCTSPlayer player, AbstractGameState state, Random rnd, Supplier<? extends SingleTreeNode> factory) {
        SingleTreeNode retValue = factory.get();
        retValue.factory = factory;
        retValue.decisionPlayer = state.getCurrentPlayer();
        retValue.params = player.getParameters();
        retValue.forwardModel = player.getForwardModel();
        retValue.rnd = rnd;
        // only root node maintains MAST statistics
        retValue.MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            retValue.MASTStatistics.add(new HashMap<>());
        if (retValue.params.useMASTAsActionHeuristic) {
            retValue.params.actionHeuristic = new MASTActionHeuristic(retValue.params.MASTActionKey, retValue.params.MASTDefaultValue);
            ((MASTActionHeuristic) retValue.params.actionHeuristic).setMASTStats(retValue.MASTStatistics);
        }
        retValue.instantiate(null, null, state);
        return retValue;
    }

    public static SingleTreeNode createChildNode(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state,
                                                 Supplier<? extends SingleTreeNode> factory) {
        SingleTreeNode retValue = factory.get();
        retValue.instantiate(parent, actionToReach, state);
        return retValue;
    }

    protected void instantiate(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState rootState) {
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.params = root.params;
        this.forwardModel = root.forwardModel;
        this.rnd = root.rnd;

        if (params.information != Closed_Loop && (params.maintainMasterState || depth == 0)) {
            // if we're using open loop, then we need to make sure the reference state is never changed
            // however this is only used at the root - and we can switch the copy off for other nodes for performance
            // these master copies *are* required if we want to do something funky with the final tree, and gather
            // features from the nodes - if we are gathering Expert Iteration data or Learning an Advantage function
            root.copyCount++;
            this.state = rootState.copy();
        } else {
            this.state = rootState;
        }

        this.round = state.getRoundCounter();
        this.turn = state.getTurnCounter();
        this.turnOwner = state.getCurrentPlayer();
        this.terminalNode = !state.isNotTerminal();

        this.actionToReach = actionToReach;

        if (parent != null) {
            depth = parent.depth + 1;
            factory = parent.factory;
            decisionPlayer = terminalStateInSelfOnlyTree(state) ? parent.decisionPlayer : state.getCurrentPlayer();
        } else { // this is the root node (possibly reused from previous tree)
            resetDepth(this);
            decisionPlayer = state.getCurrentPlayer();
        }

        // then set up available actions, and set openLoopState
        setActionsFromOpenLoopState(rootState);

    }

    public void rootify(SingleTreeNode template, AbstractGameState newState) {
        // now we need to reset the depth on all the children (recursively)
        if (newState != null)
            instantiate(null, null, newState);
        parent = null;
        actionToReach = null;
        highReward = template.highReward;
        lowReward = template.lowReward;
        inheritedVisits = nVisits;
        MASTStatistics = new ArrayList<>();
        for (int i = 0; i < template.MASTStatistics.size(); i++)
            MASTStatistics.add(new HashMap<>());
    }

    protected void resetDepth(SingleTreeNode newRoot) {
        depth = parent == null ? 0 : parent.depth + 1;
        root = newRoot;
        for (SingleTreeNode[] childArray : children.values()) {
            if (childArray == null) continue;
            for (SingleTreeNode child : childArray) {
                if (child != null) child.resetDepth(newRoot);
            }
        }
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

    /**
     * This is a key method. It is called when the tree search 'moves' to this node.
     * Because we are using Open Loop search, we need to make sure that the state is updated to reflect the
     * state in the current trajectory; each visit to the node may have a different underlying state, and it's
     * perfectly possible for different actions to be available on different visits.
     * This method looks at the actions available this time round, and initialises relevant parts of the
     * node information that will then be used during the rest of the decision-making process from this node.
     *
     * @param actionState
     */
    protected void setActionsFromOpenLoopState(AbstractGameState actionState) {
        openLoopState = actionState;
        if (actionState.getCurrentPlayer() == this.decisionPlayer && actionState.isNotTerminalForPlayer(decisionPlayer)) {
            actionsFromOpenLoopState = forwardModel.computeAvailableActions(actionState, params.actionSpace);
            //      System.out.printf("Setting OLS actions for P%d (%d)%n%s%n", decisionPlayer, actionState.getCurrentPlayer(),
//                actionsFromOpenLoopState.stream().map(a -> "\t" + a.toString() + "\n").collect(joining()));
            if (actionsFromOpenLoopState.size() != actionsFromOpenLoopState.stream().distinct().count())
                throw new AssertionError("Duplicate actions found in action list: " +
                        actionsFromOpenLoopState.stream().map(a -> "\t" + a.toString() + "\n").collect(joining()));
            if ((params.actionHeuristic != IActionHeuristic.nullReturn && nVisits < actionsFromOpenLoopState.size())
                    || params.pUCT || params.progressiveBias > 0 || params.initialiseVisits > 0 || params.progressiveWideningConstant >= 1.0) {
                // We only need to calculate actionValueEstimates if we are going to be using the data in one of these variants
                // If not, then we can save processing time by not calculating them
                // actionHeuristicRecalculationThreshold defines how often we recalculate the action values
                // if the actionHeuristic is fixed, then this should be set to a very high value
                // if, like MAST, the actionHeuristic is dynamic, then this should be set to a lower value as estimates may
                // change over the course of the search. Setting it to 1 will update it on every visit; but possibly
                // at a high additional computational cost.
                if (params.actionHeuristic != IActionHeuristic.nullReturn) {
                    if (actionValueEstimates.isEmpty() || nVisits % params.actionHeuristicRecalculationThreshold == 0) {
                        // in this case we initialise all action values
                        if (params.actionHeuristic == null) {
                            throw new AssertionError("actionHeuristic is null");
                        }
                        double[] actionValues = params.actionHeuristic.evaluateAllActions(actionsFromOpenLoopState, actionState);
                        for (int i = 0; i < actionsFromOpenLoopState.size(); i++) {
                            actionValueEstimates.put(actionsFromOpenLoopState.get(i), actionValues[i]);
                        }
                    } else {
                        // we just initialise the new actions
                        for (AbstractAction action : actionsFromOpenLoopState) {
                            if (!actionValueEstimates.containsKey(action)) {
                                actionValueEstimates.put(action, params.actionHeuristic.evaluateAction(action, actionState, actionsFromOpenLoopState));
                            }
                        }
                    }
                } else {
                    throw new AssertionError("We have no heuristic to evaluate actions, and have pUCT/PB/PW or visitInitialisation set");
                }
            }
            if (params.pUCT) {
                // construct the pdf for the pUCT selection
                // This ignores Progressive widening. This should not be a major issue, but means the pdf is calculated
                // over all possible actions, rather than just the ones we are considering
                // Generally if using pUCT we would expect FPU to also be used to give effective pruning, rather than the
                // explicit pruning of Progressive Widening.
                double[] pdf;
                actionPDFEstimates = new HashMap<>();
                if (params.pUCTTemperature > 0.0) {
                    // in this case we construct a Boltzmann
                    double[] actionValues = actionsFromOpenLoopState.stream().
                            mapToDouble(a -> actionValueEstimates.getOrDefault(a, 0.0)).toArray();
                    pdf = pdf(exponentiatePotentials(actionValues, params.pUCTTemperature));

                } else {
                    // in this case, we first set any negative values to zero, and then construct the pdf directly
                    double[] actionValues = actionsFromOpenLoopState.stream().
                            mapToDouble(a -> Math.max(0.0, actionValueEstimates.getOrDefault(a, 0.0))).toArray();
                    pdf = pdf(actionValues);
                }
                for (int i = 0; i < actionsFromOpenLoopState.size(); i++) {
                    actionPDFEstimates.put(actionsFromOpenLoopState.get(i), pdf[i]);
                }
            }
            for (AbstractAction action : actionsFromOpenLoopState) {
                if (!actionValues.containsKey(action)) {
                    actionValues.put(action, new ActionStats(actionState.getNPlayers()));
                    children.put(action.copy(), null); // mark a new node to be expanded
                    // This *does* rely on a good equals method being implemented for Actions
                    if (!children.containsKey(action))
                        throw new AssertionError("We have an action that does not obey the equals/hashcode contract" + action);
                    // Then we seed the statistics with heuristic biases (if so parameterised)
                    // This assumes that we have had params.initialiseVisits trials of each action before we start
                    if (params.initialiseVisits > 0) {
                        // This also ignores Progressive widening and initialises all possible actions
                        // As with pUCT, this won't cause any major issues, but will mean that the effective node visits
                        // will be higher than the visits of the considered actions.
                        ActionStats stats = actionValues.get(action);
                        double actionEstimate = actionValueEstimates.getOrDefault(action, 0.0);
                        if (params.normaliseRewards) {
                            if (actionEstimate > root.highReward) root.highReward = actionEstimate;
                            if (actionEstimate < root.lowReward) root.lowReward = actionEstimate;
                        }
                        int nActions = Math.max(actionValues.size(), actionsFromOpenLoopState.size());
                        stats.nVisits = params.initialiseVisits;
                        stats.validVisits = params.initialiseVisits * nActions;
                        stats.totValue[decisionPlayer] = actionEstimate * params.initialiseVisits;
                        stats.squaredTotValue[decisionPlayer] = actionEstimate * actionEstimate * params.initialiseVisits;
                        if (params.paranoid) // default to zero for other players, unless we're paranoid
                            for (int i = 0; i < actionState.getNPlayers(); i++)
                                if (i != decisionPlayer)
                                    stats.totValue[i] = -stats.totValue[decisionPlayer];
                        if (nVisits < params.initialiseVisits * nActions) {
                            nVisits = params.initialiseVisits * nActions;
                        }
                    }
                }
            }
        } else if (!params.opponentTreePolicy.selfOnlyTree) {
            throw new AssertionError("Expected?");
            // How have we got to a state in which the decision player is not the active player?
        }
    }

    protected void initialiseRootMetrics() {
        timeTaken = 0.0;
        initialisationTimeTaken = 0.0;
        nodeClash = 0;
        rolloutActionsTaken = 0;
        regretMatchingAverage.clear();
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    public void mctsSearch(long initialisationTime) {
        initialiseRootMetrics();
        initialisationTimeTaken = initialisationTime;
        // Variables for tracking time budget
        double avgTimeTaken;
        long remaining;
        int remainingLimit = params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget - initialisationTime);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;
        boolean stop = false;
        while (!stop) {
            switch (params.information) {
                case Closed_Loop:
                    setActionsFromOpenLoopState(state);
                    break;
                case Open_Loop:
                    setActionsFromOpenLoopState(state.copy());
                    copyCount++;
                    break;
                case Information_Set:
                    if (redeterminisationPlayer == -1)
                        redeterminisationPlayer = decisionPlayer;
                    setActionsFromOpenLoopState(state.copy(redeterminisationPlayer));
                    copyCount++;
                    break;
            }
            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            oneSearchIteration();

            // Finished iteration
            numIters++;
            // Check stopping condition
            PlayerConstants budgetType = params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                remaining = elapsedTimer.remainingTimeMillis();
                avgTimeTaken = (double) elapsedTimer.elapsedMillis() / numIters;
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
        timeTaken = elapsedTimer.elapsedMillis();
    }

    /**
     * oneSearchIteration() implements the strategy for tree search (plus expansion, rollouts, backup and so on)
     * Its result is purely stored in the tree generated from root
     */
    protected void oneSearchIteration() {
        actionsInTree = new ArrayList<>();
        currentNodeTrajectory = new ArrayList<>();
        actionsInRollout = new ArrayList<>();

        SingleTreeNode selected = treePolicy();
        if (selected == this && openLoopState.isNotTerminalForPlayer(decisionPlayer) && nVisits > 3 && !(this instanceof MCGSNode))
            throw new AssertionError("We have not expanded or selected a new node");
        // by this point (and really earlier) we should have expanded a new node.
        // selected == this is a clear sign that we have a problem in the expansion phase
        // although if we have no decisions to make - this is fine

        // Monte Carlo rollout: return value of MC rollout from the newly added node
        int lastActorInTree = actionsInTree.isEmpty() ? decisionPlayer : actionsInTree.get(actionsInTree.size() - 1).a;
        double[] delta = selected.rollout(lastActorInTree);
        // Back up the value of the rollout through the tree
        rolloutActionsTaken += actionsInRollout.size();

        selected.backUp(delta);
        updateMASTStatistics(actionsInTree, actionsInRollout, delta);
    }

    protected void updateMASTStatistics
            (List<Pair<Integer, AbstractAction>> tree, List<Pair<Integer, AbstractAction>> rollout, double[] value) {
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

    /**
     * Uses plain java loop instead of streams for performance
     * (this is called often enough it can make a measurable difference)
     */
    public int actionVisits(AbstractAction action) {
        ActionStats stats = actionValues.get(action);
        return stats == null ? 0 : stats.nVisits;
    }

    private int validVisitsFor(AbstractAction action) {
        if (params.information == Closed_Loop)
            return nVisits;
        ActionStats stats = actionValues.get(action);
        return stats == null ? 1 : stats.validVisits;
    }

    /**
     * Uses plain java loop instead of streams for performance
     * (this is called often enough it can make a measurable difference)
     */
    public double actionTotValue(AbstractAction action, int playerId) {
        ActionStats stats = actionValues.get(action);
        return stats == null ? 0.0 : stats.totValue[playerId];
    }

    public double nodeValue(int playerId) {
        if (nVisits == 0) return 0.0;
        return actionValues.values().stream().mapToDouble(s -> s.totValue[playerId]).sum() / nVisits;
    }

    private double actionSquaredValue(AbstractAction action, int playerId) {
        ActionStats stats = actionValues.get(action);
        return stats == null ? 0.0 : stats.squaredTotValue[playerId];
    }

    /**
     * Uses only by TreeStatistics and bestAction() after mctsSearch()
     * For this reason not converted to old-style java loop as there would be no performance gain
     */
    int[] actionVisits() {
        return actionValues.values().stream()
                .mapToInt(a -> a.nVisits)
                .toArray();
    }

    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    protected SingleTreeNode treePolicy() {

        // Caution - this is only called on the root node.

        SingleTreeNode cur = this;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.openLoopState.isNotTerminalForPlayer(cur.decisionPlayer) &&
                cur.depth < params.maxTreeDepth && !cur.actionsFromOpenLoopState.isEmpty()) {
            // Move to next child given by relevant selection function
            AbstractAction chosen = cur.treePolicyAction(true);

            // In Open_Loop (and all variants other than Closed_Loop), we make a single copy of the state at the start of each iteration
            // this is then updated with all actions (and stored in openLoopState on each node it visits).
            // In Closed_Loop we make a copy of a state only when we expand and add a new node to the tree.
            if (params.information == Closed_Loop) {
                // we do not advance
                // but we do want to track the actions taken (otherwise done in advanceState)
                actionsInTree.add(new Pair<>(cur.openLoopState.getCurrentPlayer(), chosen));
            } else {
                cur.advanceState(cur.openLoopState, chosen, false);
            }
            // add node to trajectory for later backprop
            currentNodeTrajectory.add(cur);
            // then find out where this has taken us
            boolean terminal = !cur.openLoopState.isNotTerminal() ||
                    (params.opponentTreePolicy.selfOnlyTree && !cur.openLoopState.isNotTerminalForPlayer(decisionPlayer));
            if (terminal) return cur;
            SingleTreeNode nextNode = cur.nextNodeInTree(chosen);
            // if and only if we do not find a new node, then we need to expand and create a new node
            if (nextNode == null) {
                return cur.expandNode(chosen, cur.openLoopState);
            }
            cur = nextNode;
        }
        return cur;
    }

    protected List<AbstractAction> actionsToConsider(List<AbstractAction> allAvailable) {
        if (!allAvailable.isEmpty() && params.progressiveWideningConstant >= 1.0) {
            int actionsToConsider = (int) Math.floor(params.progressiveWideningConstant * Math.pow(nVisits + 1, params.progressiveWideningExponent));
            actionsToConsider = Math.min(actionsToConsider, allAvailable.size());
            // takes account of the expanded actions
            if (actionsToConsider <= 0) return new ArrayList<>();
            // sort in advantage order (descending)
            // It is perfectly possible that a previously expanded action falls out of the considered list
            // depending on the advantage heuristic used.
            // However, we do break ties in favour of already expanded actions
            List<AbstractAction> sortedActions = new ArrayList<>(allAvailable);
            sortedActions.sort(Comparator.comparingDouble(a -> -actionValueEstimates.getOrDefault(a, 0.0) -
                    actionValues.getOrDefault(a, new ActionStats(1)).nVisits * 1e-6));
            return new ArrayList<>(sortedActions.subList(0, actionsToConsider));
        }
        return new ArrayList<>(allAvailable);
    }


    protected SingleTreeNode expandNode(AbstractAction actionCopy, AbstractGameState nextState) {
        // then instantiate a new node
        int nextPlayer = params.opponentTreePolicy.selfOnlyTree ? decisionPlayer : nextState.getCurrentPlayer();
        SingleTreeNode tn = createChildNode(actionCopy, nextState);
        // It is possible that we are expanding a node because a different player is the next to act
        SingleTreeNode[] newNodeArray = children.get(actionCopy);
        if (newNodeArray == null)
            newNodeArray = new SingleTreeNode[nextState.getNPlayers()];
        newNodeArray[nextPlayer] = tn; // we store this by id of the player who will take their turn next
        children.put(actionCopy, newNodeArray);
        return tn;
    }

    protected SingleTreeNode createChildNode(AbstractAction actionCopy, AbstractGameState nextState) {
        // then instantiate a new node
        return SingleTreeNode.createChildNode(this, actionCopy, nextState, factory);
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
    protected void advanceState(AbstractGameState gs, AbstractAction act, boolean inRollout) {
        // we execute a copy(), because this can change the action, so we then don't find the node later!
        if (inRollout) {
            lastTurnInRollout = gs.getTurnCounter();
            lastRoundInRollout = gs.getRoundCounter();
            lastActorInRollout = gs.getCurrentPlayer();
            root.actionsInRollout.add(new Pair<>(lastActorInRollout, act));
        } else {
            root.actionsInTree.add(new Pair<>(gs.getCurrentPlayer(), act));
        }
        forwardModel.next(gs, act.copy());
        root.fmCallsCount++;
        if (params.opponentTreePolicy != MultiTree &&
                params.opponentTreePolicy.selfOnlyTree &&
                gs.getCurrentPlayer() != decisionPlayer)
            advanceToTurnOfPlayer(gs, decisionPlayer, inRollout);
    }

    /**
     * Advance the game state to the next point at which it is the turn of the specified player.
     * This is used when we are only tracking our ourselves in the tree.
     *
     * @param id
     */
    protected void advanceToTurnOfPlayer(AbstractGameState gs, int id, boolean inRollout) {
        // For the moment we only have one opponent model - that of a random player
        AbstractAction action = null;
        while (gs.getCurrentPlayer() != id && gs.isNotTerminalForPlayer(id) && !(inRollout && finishRollout(gs))) {
            //       AbstractGameState preGS = gs.copy();
            AbstractPlayer oppModel = params.getOpponentModel();
            List<AbstractAction> availableActions = forwardModel.computeAvailableActions(gs, params.actionSpace);
            if (availableActions.isEmpty())
                throw new AssertionError("Should always have at least one action possible..." + (action != null ? " Last action: " + action : ""));
            action = oppModel.getAction(gs, availableActions);
            if (inRollout) {
                root.actionsInRollout.add(new Pair<>(gs.getCurrentPlayer(), action));
                lastActorInRollout = gs.getCurrentPlayer();
                lastRoundInRollout = gs.getRoundCounter();
                lastTurnInRollout = gs.getTurnCounter();
            }
            forwardModel.next(gs, action);
            root.fmCallsCount++;
        }
    }

    /**
     * Apply relevant policy to choose a child.
     *
     * @return - child node according to the tree policy
     */
    protected AbstractAction treePolicyAction(boolean explore) {
        if (params.opponentTreePolicy == SelfOnly && parent != null && openLoopState != null && openLoopState.getCurrentPlayer() != decisionPlayer)
            throw new AssertionError("An error has occurred. SelfOnly should only call uct when we are moving.");

        // actionsToConsider takes care of any Progressive Widening in play, so we only consider the
        // widened subset
        List<AbstractAction> availableActions = actionsToConsider(actionsFromOpenLoopState);
        if (availableActions.isEmpty())
            throw new AssertionError("We need to have at least one option");

        AbstractAction actionChosen;
        if (availableActions.size() == 1) {
            actionChosen = availableActions.get(0);
        } else {
            // first we shuffle to break ties
            Collections.shuffle(availableActions, rnd);
            // then get the actionValues
            double[] actionValues = actionValues(availableActions);
            // then pick the best one
            actionChosen = switch (params.treePolicy) {
                case Uniform -> availableActions.get(rnd.nextInt(availableActions.size()));
                case Greedy -> {
                    // check exploration first
                    if (explore && rnd.nextDouble() < params.exploreEpsilon) {
                        yield availableActions.get(rnd.nextInt(availableActions.size()));
                    }
                    AbstractAction bestAction = null;
                    double bestValue = -Double.MAX_VALUE;
                    for (int i = 0; i < availableActions.size(); i++) {
                        if (actionValues[i] > bestValue) {
                            bestValue = actionValues[i];
                            bestAction = availableActions.get(i);
                        }
                    }
                    yield bestAction;
                }
                case UCB, AlphaGo, UCB_Tuned -> {
                    // These take the max
                    // Find child with highest UCB value
                    AbstractAction bestAction = null;
                    double bestValue = -Double.MAX_VALUE;
                    for (AbstractAction availableAction : availableActions) {
                        double uctValue = ucbValue(availableAction);
                        if (uctValue > bestValue) {
                            bestValue = uctValue;
                            bestAction = availableAction;
                        }
                    }
                    yield bestAction;
                }
                case RegretMatching, EXP3, NoAveragingRM -> {
                    // check exploration first
                    if (explore && rnd.nextDouble() < params.exploreEpsilon) {
                        yield availableActions.get(rnd.nextInt(availableActions.size()));
                    }
                    double[] pdf = pdf(actionValues);
                    long nonZeroActions = Arrays.stream(actionValues).filter(v -> v > 0.0).count();
                    if (nonZeroActions == 0) {
                        // if we have no non-zero values, then we just pick one at random
                        yield availableActions.get(rnd.nextInt(availableActions.size()));
                    }
                    yield availableActions.get(sampleFrom(pdf, rnd.nextDouble()));
                }
                default -> throw new AssertionError("Unknown treePolicy: " + params.treePolicy);
            };
        }

        return actionChosen;
    }


    /**
     * Returns the next node in the tree after taking the specified action from this one.
     * Returns null if we have left the tree (expansion will then take place in treePolicy().
     * <p>
     * The chosen action will already have been applied to openLoopState when this is called.
     */
    protected SingleTreeNode nextNodeInTree(AbstractAction actionChosen) {
        // We check to see if we have finished. This is true if the gameState is terminal; or if
        // this is a selfOnly tree and it is terminal for the root decision player

        SingleTreeNode[] nodeArray = children.get(actionChosen);
        if (nodeArray == null) return null;
        if (params.information == Closed_Loop) {
            // in this case we have determinism...there should just be a single child node in the array...so we get that
            return Arrays.stream(nodeArray).filter(Objects::nonNull).findFirst().orElse(null);
        } else {
            //  int nextPlayer = params.opponentTreePolicy.selfOnlyTree ? decisionPlayer : openLoopState.getCurrentPlayer();
            SingleTreeNode nextNode = nodeArray[openLoopState.getCurrentPlayer()];
//            if (params.opponentTreePolicy.selfOnlyTree && nextNode.decisionPlayer != decisionPlayer) {
//                nodeArray[nextPlayer] = SingleTreeNode.createChildNode(this, actionChosen.copy(), openLoopState, factory);
//                nextNode = nodeArray[nextPlayer];
//            }
            // pick up the existing one, and set the state
            if (nextNode != null)
                nextNode.setActionsFromOpenLoopState(openLoopState);
            return nextNode;
        }
    }


    // Returns the values according to the selection policy (UCB, EXP3, etc.)
    // This is stage 1 of processing, before we use these to pick an action to take
    protected double[] actionValues(List<AbstractAction> actionsToConsider) {
        double[] retValue = new double[actionsToConsider.size()];
        for (int i = 0; i < actionsToConsider.size(); i++) {
            AbstractAction action = actionsToConsider.get(i);
            retValue[i] = switch (params.treePolicy) {
                case Uniform -> 1.0;
                case Greedy -> getFullValue(action);
                case UCB, AlphaGo, UCB_Tuned -> ucbValue(action);
                case RegretMatching, NoAveragingRM -> rmValue(action);
                case EXP3 -> exp3Value(action);
            };
        }
        return retValue;
    }

    private double getFullValue(AbstractAction action) {
        double value = getActionValue(action);
        int actionVisits = actionVisits(action);
        if (params.normaliseRewards && actionVisits > 0) {
            value = normalise(value, root.lowReward, root.highReward);
        }
        if (params.progressiveBias > 0)
            value += getBiasValue(action);
        // apply OMA
        value = getOMAValue(action, value);
        return value;
    }

    private double getOMAValue(AbstractAction action, double childValue) {
        int actionVisits = actionVisits(action);
        double retValue = childValue;
        // consider OMA term
        if (params.omaVisits > 0 && (params.opponentTreePolicy == OMA_All || params.opponentTreePolicy == OMA)) {
            OMATreeNode oma = ((OMATreeNode) this).OMAParent.orElse(null);
            if (oma != null) {
                double beta = Math.sqrt(params.omaVisits / (double) (params.omaVisits + 3 * actionVisits));
                // we need to find the action taken from the OMAParent
                SingleTreeNode iteratingNode = this;
                List<AbstractAction> actionsTaken = new ArrayList<>();
                do {
                    actionsTaken.add(iteratingNode.actionToReach);
                    iteratingNode = iteratingNode.parent;
                    if (iteratingNode == null)
                        throw new AssertionError("Should always find OMA node before root");
                } while (iteratingNode != oma);
                Map<AbstractAction, OMATreeNode.OMAStats> tmp = oma.OMAChildren.get(actionsTaken.get(actionsTaken.size() - 1));
                if (tmp == null) {
                    if (actionVisits == 0) {
                        // do nothing - this is possible as we do not create OMA node until we back-propagate
                        // so on the first visit there may not be one yet
                    } else {
                        throw new AssertionError("We have somehow failed to find the OMA node for this action");
                    }
                } else {
                    OMATreeNode.OMAStats stats = tmp.get(action);
                    if (stats != null && stats.OMAVisits > 0) {
                        double omaValue = stats.OMATotValue / stats.OMAVisits;
                        retValue = (1.0 - beta) * childValue + beta * omaValue;
                    }
                }
            }
        }
        return retValue;
    }

    private double ucbValue(AbstractAction action) {

        // Find 'UCB' value - this is the base to which we then add exploration
        double childValue = getFullValue(action);
        int actionVisits = actionVisits(action);

        // Now for the exploration term
        // default to standard UCB
        int effectiveTotalVisits = validVisitsFor(action);
        // use first play urgency as replacement for exploration term if action not previously taken
        // we add in the second term based on the AlphaGo selection rule, so that the exploration term is monotonically increasing with N
        // this will come into play for small values of FPU and acts as soft-pruning rather than the harder form if FPU is a fixed constant
        double explorationTerm = Math.max(params.firstPlayUrgency, params.K * Math.sqrt(effectiveTotalVisits));
        if (actionVisits > 0) {
            explorationTerm = switch (params.treePolicy) {
                case UCB_Tuned -> {
                    double range = root.highReward - root.lowReward;
                    if (range < 1e-6) range = 1e-6;
                    double meanSq = actionSquaredValue(action, decisionPlayer) / actionVisits;
                    double standardVar = 0.25;
                    if (params.normaliseRewards) {
                        // we also need to standardise the sum of squares to calculate the variance
                        meanSq = (meanSq
                                + root.lowReward * root.lowReward
                                - 2 * root.lowReward * actionTotValue(action, decisionPlayer) / actionVisits
                        ) / (range * range);
                    } else {
                        // we need to modify the standard variance as it is not on a 0..1 basis (which is where 0.25 comes from)
                        standardVar = Math.sqrt(range / 2.0);
                    }
                    double variance = Math.max(0.0, meanSq - childValue * childValue);
                    double minTerm = Math.min(standardVar, variance + Math.sqrt(2 * Math.log(effectiveTotalVisits) / actionVisits));
                    yield params.K * Math.sqrt(Math.log(effectiveTotalVisits) / actionVisits * minTerm);
                }
                case AlphaGo -> params.K * Math.sqrt(effectiveTotalVisits) / (actionVisits + 1.0);
                default -> Math.sqrt(Math.log(effectiveTotalVisits) / actionVisits);
            };
        }
        if (params.pUCT) {
            // in this case we multiply the exploration term by the pUCT factor (the probability that the action would be taken by
            // our actionHeuristic). These were calculated in setActionsFromOpenLoopState
            explorationTerm *= actionPDFEstimates.get(action);
        }

        // Paranoid/SelfOnly control determines childValue here
        // If we are Paranoid, then the back-propagation will ensure that childValue is minus our score for opponent nodes
        double uctValue = childValue + explorationTerm;
        if (Double.isNaN(uctValue))
            throw new AssertionError("Numeric error calculating uctValue");

        // Assign value
        // Apply small noise to break ties randomly
        //   uctValue = noise(uctValue, params.noiseEpsilon, rnd.nextDouble());
        return uctValue;
    }

    public double exp3Value(AbstractAction action) {
        double actionValue = getActionValue(action);
        int actionVisits = actionVisits(action);
        // we then normalise to [0, 1], or we subtract the mean action value to get an advantage (and reduce risk of
        // NaN or Infinities when we exponentiate)
        if (actionVisits > 0) {
            if (params.normaliseRewards)
                actionValue = normalise(actionValue, root.lowReward, root.highReward);
            else
                actionValue = actionValue - nodeValue(decisionPlayer);
        }
        if (params.progressiveBias > 0)
            actionValue += getBiasValue(action);
        double retValue = Math.exp(actionValue / params.exp3Boltzmann);

        if (Double.isNaN(retValue) || Double.isInfinite(retValue)) {
            System.out.printf("We have a non-number %s in EXP3 (from %.0f) somewhere from %s %n", retValue, actionValue, action);
            retValue = 1e6;  // to avoid numeric issues later
        }
        // We add FPU after exponentiation for safety (as it likely a large number)
        if (actionVisits == 0) {
            retValue += params.firstPlayUrgency;
        }
        return retValue;
    }

    public double rmValue(AbstractAction action) {
        double actionValue = getActionValue(action);
        if (params.progressiveBias > 0)
            actionValue += getBiasValue(action);
        double nodeValue = nodeValue(decisionPlayer);
        // potential value is our estimate of our accumulated reward if we had always taken this action
        double potentialValue = actionValue * nVisits;
        double regret = potentialValue - nodeValue * nVisits;
        // We add FPU after all the exponentiation for safety
        int actionVisits = actionVisits(action);
        if (actionVisits == 0) {
            regret += params.firstPlayUrgency;
        }
        return Math.max(0.0, regret);
    }

    private double getActionValue(AbstractAction action) {
        int actionVisits = actionVisits(action);
        // if we are at 'expansion' phase, then we break ties by expansion policy (which is the same actionHeuristic as progressive bias)
        return actionVisits > 0 ? actionTotValue(action, decisionPlayer) / actionVisits : 0.0;
    }

    private double getBiasValue(AbstractAction action) {
        int actionVisits = actionVisits(action);
        return params.progressiveBias * actionValueEstimates.getOrDefault(action, 0.0) / (actionVisits + 1);
    }

    /**
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    protected double[] rollout(int lastActor) {
        lastActorInRollout = lastActor;
        roundAtStartOfRollout = openLoopState.getRoundCounter();
        turnAtStartOfRollout = openLoopState.getTurnCounter();
        lastTurnInRollout = openLoopState.getTurnCounter();
        lastRoundInRollout = openLoopState.getRoundCounter();

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = openLoopState;
        if (params.rolloutLength > 0 || params.rolloutTermination != EXACT) {
            // even if rollout length is zero, we may rollout a few actions to reach the end of our turn, or the start of our next turn
            if (params.information == Closed_Loop) {
                // the thinking here is that in openLoop we copy the state right at the root, and then use the forward
                // model at each action. Hence the current state on the node is the one we have been using up to now.
                /// Hence we do not need to copy it.
                rolloutState = state.copy();
                root.copyCount++;
            }

            AbstractAction next = null;
            while (!finishRollout(rolloutState)) {
                List<AbstractAction> availableActions = forwardModel.computeAvailableActions(rolloutState, params.actionSpace);
                if (availableActions.isEmpty()) {
                    throw new AssertionError("No actions available in rollout!" + (next != null ? " Last action: " + next : ""));
                }
                AbstractPlayer agent = rolloutState.getCurrentPlayer() == root.decisionPlayer ? params.getRolloutStrategy() : params.getOpponentModel();
                next = agent.getAction(rolloutState, availableActions);
                advanceState(rolloutState, next, true);
            }
        }
        // Evaluate final state and return normalised score
        double[] retValue = new double[rolloutState.getNPlayers()];

        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = params.heuristic.evaluateState(rolloutState, i);
            if (Double.isNaN(retValue[i]) || Double.isInfinite(retValue[i]))
                throw new AssertionError("Illegal heuristic value - should be a number - " + params.heuristic.toString());
        }
        return retValue;
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
     *
     * @param rollerState - current state
     * @return - true if rollout finished, false otherwise
     */
    protected boolean finishRollout(AbstractGameState rollerState) {
        if (!rollerState.isNotTerminal())
            return true;
        int currentActor = rollerState.getTurnOwner();
        int maxRollout = params.rolloutLengthPerPlayer ? params.rolloutLength * rollerState.getNPlayers() : params.rolloutLength;
        int rolloutDepth = switch (params.rolloutIncrementType) {
            case TICK -> root.actionsInRollout.size();
            case TURN -> rollerState.getTurnCounter() - turnAtStartOfRollout;
            case ROUND -> rollerState.getRoundCounter() - roundAtStartOfRollout;
        };
        if (rolloutDepth >= maxRollout) {
            return switch (params.rolloutTermination) {
                case EXACT -> true;
                case END_ACTION -> lastActorInRollout == root.decisionPlayer && currentActor != root.decisionPlayer;
                case START_ACTION -> lastActorInRollout != root.decisionPlayer && currentActor == root.decisionPlayer;
                case END_TURN -> rollerState.getTurnCounter() != lastTurnInRollout;
                case END_ROUND -> rollerState.getRoundCounter() != lastRoundInRollout;
            };
        }
        return false;
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param delta - value of rollout to backup
     */
    protected void backUp(double[] delta) {
        normaliseRewardsAfterIteration(delta);
        double[] result = processResultsForParanoidOrSelfOnly(delta);
        // we need to go backwards up the tree, as the result may change
        for (int i = root.currentNodeTrajectory.size() - 1; i >= 0; i--) {
            int actingPlayer = root.actionsInTree.get(i).a;
            AbstractAction action = root.actionsInTree.get(i).b;
            SingleTreeNode n = root.currentNodeTrajectory.get(i);
            if (n.decisionPlayer != actingPlayer)
                throw new AssertionError("We have a mismatch between the player who took the action and the player who should be acting");
            result = n.backUpSingleNode(action, result);
        }
    }

    protected void normaliseRewardsAfterIteration(double[] result) {
        // after each iteration we update the min and max rewards seen, to be used in future iterations.
        // These are only stored on the root
        if (params.normaliseRewards || params.treePolicy == UCB_Tuned) {
            DoubleSummaryStatistics stats = Arrays.stream(result).summaryStatistics();
            if (root.lowReward > stats.getMin())
                root.lowReward = stats.getMin();
            if (root.highReward < stats.getMax())
                root.highReward = stats.getMax();
        }
        if (root.lowReward == Double.NEGATIVE_INFINITY || root.highReward == Double.POSITIVE_INFINITY)
            throw new AssertionError("We have somehow failed to update the min or max rewards");
    }

    protected double[] processResultsForParanoidOrSelfOnly(double[] result) {
        // then we take of SelfOnly or Paranoid assumptions to update the results
        double[] retValue = result.clone();
        switch (params.opponentTreePolicy) {
            case SelfOnly:
            case MCGSSelfOnly:
                Arrays.fill(retValue, result[root.decisionPlayer]);
                break;
            case OneTree:
            case MultiTree:
            case OMA_All:
            case OMA:
            case MCGS:
                if (params.paranoid) {
                    int paranoid = root.paranoidPlayer == -1 ? root.decisionPlayer : root.paranoidPlayer;
                    for (int j = 0; j < result.length; j++) {
                        if (j != paranoid) {
                            retValue[j] = -result[paranoid];
                        }
                    }
                }
                break;
        }
        return retValue;
    }

    /**
     * This take in the result coming from the child node, and updates the statistics for the action taken
     * It returns the reward that should be back-propagated to the parent node.
     * In the case of vanilla MCTS, this is unchanged from the input result.
     * But, if we are interpolating some max/Q update, then this will change the result.
     */
    protected double[] backUpSingleNode(AbstractAction actionTaken, double[] result) {
        if (params.discardStateAfterEachIteration) {
            if (depth > 0)
                openLoopState = null; // releases for Garbage Collection
            if (depth > 0 && !params.maintainMasterState)
                state = null;
        }
        nVisits++;
        // Here we look at actionsFromOpenLoopState to see which ones were valid
        // when we passed through, and keep track of valid visits
        List<AbstractAction> actionsToConsider = actionsToConsider(actionsFromOpenLoopState);

        // then we update the statistics for the action taken
        if (!actionsToConsider.contains(actionTaken)) {
            if (params.opponentTreePolicy != MCGS && params.opponentTreePolicy != MCGSSelfOnly)
                throw new AssertionError("We have somehow failed to find the action taken in the list of valid actions");

            // If MCGS, then this is possible if we have looped in the graph, so that OpenLoopState refers
            // to a different state than the one for which the action was taken. This is awkward.
            // In the absence of any good information, we just increment the valid visits of all actions
            for (ActionStats stats : actionValues.values()) {
                stats.validVisits++;
            }
        } else {
            for (AbstractAction action : actionsToConsider) {
                if (!actionValues.containsKey(action))
                    actionValues.put(action, new ActionStats(result.length));
                actionValues.get(action).validVisits++;
            }
        }
        ActionStats stats = actionValues.get(actionTaken);
        if (stats == null)
            throw new AssertionError("We have somehow failed to find the action taken in the list of actions");
        if (stats.validVisits == 0)
            throw new AssertionError("We have somehow failed to find the action taken in the list of valid actions");

        stats.update(result);

        if (params.treePolicy == RegretMatching) {
            int updateEvery = Math.max(actionsToConsider.size(), 10);
            if (nVisits >= updateEvery && nVisits % updateEvery == 0) {
                // we update the average policy each time we have had the opportunity to take each action once (or every 10 visits, if that is greater)
                updateRegretMatchingAverage(actionsToConsider);
            }
        }

        if (params.backupPolicy == MCTSEnums.BackupPolicy.MonteCarlo)
            return result;

        // otherwise we do some more complex backup
        double resultToPropagateUpwards[] = result.clone();
        AbstractAction bestAction = bestAction(actionsToConsider);
        double[] maxValue = actionValues.get(bestAction).totValue.clone();
        for (int i = 0; i < maxValue.length; i++) {
            maxValue[i] /= actionValues.get(bestAction).nVisits;
        }
        return switch (params.backupPolicy) {
            case MonteCarlo:
                yield result;
            case Lambda:
                // SARSA-style on-policy update. We weight the action average by 1 - lambda
                for (int i = 0; i < result.length; i++) {
                    resultToPropagateUpwards[i] = params.backupLambda * result[i] + (1.0 - params.backupLambda) * stats.totValue[i] / stats.nVisits;
                }
                yield resultToPropagateUpwards;
            case MaxLambda:
                // SARSA-style off-policy update.
                for (int i = 0; i < result.length; i++) {
                    resultToPropagateUpwards[i] = params.backupLambda * result[i] + (1.0 - params.backupLambda) * maxValue[i];
                }
                yield resultToPropagateUpwards;
            case MaxMC:
                if (nVisits > params.maxBackupThreshold) {
                    // in this case we mix in a max backup
                    // *if* we took an action other than the one with the current best estimate
                    if (bestAction == null) {
                        // this can happen for low maxBackupCounts with no actions available
                        // we default to ignoring Max functionality
                        bestAction = actionTaken;
                    }
                    if (!bestAction.equals(actionTaken)) {
                        double maxWeight = (nVisits - params.maxBackupThreshold) / (double) nVisits;
                        // we mix for all players, based on the counterfactual decision of the acting player
                        for (int i = 0; i < result.length; i++) {
                            resultToPropagateUpwards[i] = (1 - maxWeight) * result[i] + maxWeight * maxValue[i];
                        }
                    }
                    yield resultToPropagateUpwards;
                } else {
                    yield result;
                }
        };

    }

    public AbstractAction bestAction(List<AbstractAction> actionsToConsider) {
        AbstractAction bestAction = null;
        double maxValue = -Double.MAX_VALUE;
        for (AbstractAction action : actionsToConsider) {
            ActionStats temp = actionValues.get(action);
            double value = temp.nVisits == 0 ? -Double.MAX_VALUE :
                    temp.totValue[decisionPlayer] / temp.nVisits;
            if (value > maxValue) {
                maxValue = value;
                bestAction = action;
            }
        }
        if (bestAction == null)
            return actionsToConsider.get(rnd.nextInt(actionsToConsider.size()));
        return bestAction;
    }


    protected void MASTBackup(List<Pair<Integer, AbstractAction>> rolloutActions, double[] delta) {
        for (Pair<Integer, AbstractAction> pair : rolloutActions) {
            AbstractAction action = pair.b;
            int player = pair.a;
            Object actionKey = params.MASTActionKey == null ? action.copy() : params.MASTActionKey.key(action);
            Pair<Integer, Double> stats = MASTStatistics.get(player).getOrDefault(actionKey, new Pair<>(0, 0.0));
            stats.a++;  // visits
            stats.b += delta[player];   // value
            MASTStatistics.get(player).put(actionKey, stats);
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
        if (params.selectionPolicy == ROBUST &&
                Arrays.stream(actionVisits()).boxed().collect(toSet()).size() == 1) {
            policy = SIMPLE;
        }
        if (params.treePolicy == EXP3) {
            // EXP3 uses the tree policy (without exploration)
            bestAction = treePolicyAction(false);
        } else if (params.treePolicy == RegretMatching) {
            // RM uses a special policy as the average of all previous root policies
            if (regretMatchingAverage.isEmpty()) // in case we have not yet updated the regret matching average
                updateRegretMatchingAverage(actionsToConsider(actionsFromOpenLoopState));
            bestAction = regretMatchingAverage();
        } else {
            // We iterate through all actions valid in the original root state
            // as openLoopState is fine with SingleTreeNode or MultiTreeNode
            List<AbstractAction> availableActions = actionsToConsider(actionsFromOpenLoopState);
            if (state != null && (
                    (redeterminisationPlayer != -1 && redeterminisationPlayer != decisionPlayer)
                            || params.opponentTreePolicy == MCGS
                            || params.opponentTreePolicy == MCGSSelfOnly)) {
                // In these cases we need to recompute the available actions from the root state to ensure that
                // we only consider the ones that are valid in the caller (in MCGS case it is possible that we have a loop round to the root)
                availableActions = actionsToConsider(forwardModel.computeAvailableActions(state, params.actionSpace));
            }
            for (AbstractAction action : availableActions) {
                if (!actionValues.containsKey(action)) {
                    throw new AssertionError("Hashcode / equals contract issue for " + action);
                }
                if (actionValues.get(action) != null) {
                    ActionStats stats = actionValues.get(action);
                    double childValue = stats.nVisits; // if ROBUST
                    if (policy == SIMPLE)
                        childValue = stats.totValue[decisionPlayer] / (stats.nVisits + params.noiseEpsilon);

                    // Apply small noise to break ties randomly
                    childValue = noise(childValue, params.noiseEpsilon, rnd.nextDouble());

                    // Save best value
                    if (childValue > bestValue) {
                        bestValue = childValue;
                        bestAction = action;
                    }
                }
            }
        }

        if (bestAction == null) {
            if (nVisits < 2) {
//                System.out.println("Only one visit to root node - insufficient information - hopefully due to JVM warming up");
                bestAction = actionValues.keySet().stream().findFirst().orElseThrow(() -> new AssertionError("No children"));
            } else
                throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }

    protected void updateRegretMatchingAverage(List<AbstractAction> actionsToConsider) {
        double[] av = actionValues(actionsToConsider);
        double[] pdf = pdf(av);
        for (int i = 0; i < actionsToConsider.size(); i++) {
            regretMatchingAverage.merge(actionsToConsider.get(i), pdf[i], Double::sum);
        }
    }

    protected AbstractAction regretMatchingAverage() {
        List<AbstractAction> actionsToConsider = actionsToConsider(actionsFromOpenLoopState);
        double[] potentials = new double[actionsToConsider.size()];
        int count = 0;
        for (AbstractAction action : actionsToConsider) {
            potentials[count] = regretMatchingAverage.getOrDefault(action, 0.0);
            count++;
        }
        double[] pdf = pdf(potentials);
        int index = sampleFrom(pdf, rnd.nextDouble());
        return actionsToConsider.get(index);
    }

    public int getVisits() {
        return nVisits;
    }

    public int getDepth() {
        return depth;
    }

    public void setRedeterminisationPlayer(int player) {
        redeterminisationPlayer = player;
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
        return filterTree(n -> !allMatch.test(n));
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

    /**
     * This looks for the first parent node that matches the specified Predicate
     * This will look at parent first, then grandparent, etc.
     * This returns null if no match is found
     *
     * @param match
     * @return
     */
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
        String valueString = String.format("%.2f", nodeValue(decisionPlayer));
        if (!params.opponentTreePolicy.selfOnlyTree && openLoopState != null) {
            valueString = IntStream.range(0, openLoopState.getNPlayers())
                    .mapToDouble(this::nodeValue)
                    .mapToObj(v -> String.format("%.2f", v))
                    .collect(joining(", "));
        }
        retValue.append(String.format("%d total visits, value %s, with %d children, %d actions, depth %d, FMCalls %d: \n",
                nVisits, valueString, children.size(), actionValues.size(), depth, fmCallsCount));
        // sort all actions by visit count
        List<AbstractAction> sortedActions = actionValues.keySet().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(a -> -actionVisits(a)))
                .toList();

        for (AbstractAction action : sortedActions) {
            String actionName = action.toString();
            int actionVisits = actionVisits(action);
            int effectiveVisits = validVisitsFor(action);
            if (actionName.length() > 50)
                actionName = actionName.substring(0, 50);
            valueString = String.format("%.2f", actionTotValue(action, decisionPlayer) / actionVisits);
            if (params.opponentTreePolicy == OneTree) {
                int players = state == null ? children.get(action).length : state.getNPlayers();
                valueString = IntStream.range(0, players)
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
