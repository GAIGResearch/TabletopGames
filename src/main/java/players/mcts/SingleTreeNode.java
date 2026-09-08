package players.mcts;

import core.*;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import core.interfaces.IActionHeuristic;
import players.PlayerConstants;
import utilities.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
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
    protected int nodeClash;
    protected SingleTreeNode root;
    SingleTreeNode parent;
    // Children of this node. The value is an Array because we have to cater for the possibility that the next decision
    // could be by any player - and this may differ on each iteration with OpenLoop search. (Closed Loop will
    // only ever have one position in the array populated: and similarly if we are using a SelfOnly tree).
    Map<AbstractAction, SingleTreeNode[]> children = new LinkedHashMap<>();
    // The players who decide at this node, as reported by the game state on the most recent visit (in Open Loop)
    // A single player in a sequential search; several entries at a simultaneous-move node in decoupled search.
    protected List<Integer> actingPlayers = Collections.emptyList();
    // The candidate actions and statistics for each player who decides at this node.
    protected final Map<Integer, PlayerDecisionStats> statsByPlayer = new HashMap<>();
    List<Map<Object, Pair<Integer, Double>>> MASTStatistics; // a Map per player. Action -> (visits, totValue)
    // The total value of all trajectories through this node (one element per player), over every visit
    // whoever was acting. Kept on the node rather than derived from a player's action table, because at a
    // multi-actor node the acting set can differ between visits and no single table then covers them all.
    protected double[] totValue;
    private Supplier<? extends SingleTreeNode> factory;
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

        if (params.information != Closed_Loop && (params.maintainMasterState || parent == null)) {
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
            // decisionPlayer is the actingPlayer; or the root player in the case of simultaneous actions
            decisionPlayer = terminalStateInSelfOnlyTree(state) ? parent.decisionPlayer
                    : actingPlayersAt(state).size() > 1 ? root.decisionPlayer : state.getCurrentPlayer();
        } else { // this is the root node (possibly reused from previous tree)
            resetDepth(this);
            // The root always has a decision owner - the player the search is on behalf of
            // True even for decoupled search
            decisionPlayer = state.getCurrentPlayer();
        }

        // During Tree reuse instantiate() runs a second time on a node only via rootify(), i.e.
        // under reuseTree, and the statistics must not be deleted. decisionPlayer has just been
        // recomputed from the new root state, so move the single existing entry to that key (this can change under MCGS).
        // A multi-actor node (decoupled UCT) holds one entry per acting player,
        // which do not depend on the current player, so there is nothing to change.
        // (Also decoupled UCT currently does not support tree reuse)
        if (statsByPlayer.size() == 1 && !statsByPlayer.containsKey(decisionPlayer)) {
            PlayerDecisionStats existing = statsByPlayer.values().iterator().next();
            statsByPlayer.clear();
            statsByPlayer.put(decisionPlayer, existing);
            // JG: I need to understand the precise scenario under which the new root node has a different actingPlayer...I am not sure I see how this can happen?
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
     * The players who decide in the given state. In a standard sequential search that is always just the
     * current player; in a decoupled search it may be several players on a simultaneous turn.
     */
    protected List<Integer> actingPlayersAt(AbstractGameState s) {
        if (!params.decoupled) return Collections.singletonList(s.getCurrentPlayer());
        return s.getCurrentSimultaneousPlayers();
    }

    /** True when several players decide at this node, i.e. its children are keyed by joint actions. */
    public boolean isMultiActor() {
        return actingPlayers.size() > 1;
    }

    /** The players who decide at this node, as of the most recent visit. */
    public List<Integer> getActingPlayers() {
        return actingPlayers;
    }

    /**
     * Called when the tree search 'moves' to this node.
     * When we are using Open Loop search, we need to make sure that the state is updated to reflect the
     * state in the current trajectory; each visit to the node may have a different underlying state, and it's
     * perfectly possible for different actions to be available on different visits.
     * This method looks at the actions available this time round and initialises relevant parts of the
     * node information that will then be used during the rest of the decision-making process at this node.
     *
     */
    protected void setActionsFromOpenLoopState(AbstractGameState actionState) {
        openLoopState = actionState;
        actingPlayers = actingPlayersAt(actionState);
        if (!isMultiActor()) {
            // The standard sequential path, for which getCurrentPlayer() is used
            if (actionState.getCurrentPlayer() == this.decisionPlayer && actionState.isNotTerminalForPlayer(decisionPlayer)) {
                setActionsForPlayer(actionState, decisionPlayer);
            } else if (!params.opponentTreePolicy.selfOnlyTree) {
                throw new AssertionError("Unexpected non-self current player");
            }
        } else {
            // Several players decide here. Each gets their own candidate list and statistics; the child
            // will be keyed by the joint action.
            for (int p : actingPlayers) {
                if (actionState.isNotTerminalForPlayer(p))
                    setActionsForPlayer(actionState, p);
                else
                    throw new AssertionError("A player who is allegedly active *must* have some actions available");
            }
        }
    }

    /**
     * Work out one player's options at this node on this iteration and initialise their statistics.
     */
    protected void setActionsForPlayer(AbstractGameState actionState, int actingPlayer) {
        PlayerDecisionStats pds = statsFor(actingPlayer);
        pds.actionsFromOpenLoopState = forwardModel.computeAvailableActions(actionState, params.actionSpace, actingPlayer);
        //      System.out.printf("Setting OLS actions for P%d (%d)%n%s%n", actingPlayer, actionState.getCurrentPlayer(),
//                pds.actionsFromOpenLoopState.stream().map(a -> "\t" + a.toString() + "\n").collect(joining()));
        if (pds.actionsFromOpenLoopState.size() != pds.actionsFromOpenLoopState.stream().distinct().count())
            throw new AssertionError("Duplicate actions found in action list: " +
                    pds.actionsFromOpenLoopState.stream().map(a -> "\t" + a.toString() + "\n").collect(joining()));
        if ((params.useActionHeuristicForMoveOrdering && nVisits < pds.actionsFromOpenLoopState.size())
                || params.pUCTTemperature <= 10000.0 || params.progressiveWideningConstant > 1.0
                || params.progressiveBias > 0 || params.initialiseVisits > 0) {
            // We only need to calculate actionValueEstimates if we are going to be using the data in one of these variants
            // If not, then we can save processing time by not calculating them
            // actionHeuristicRecalculationThreshold defines how often we recalculate the action values
            // if the actionHeuristic is fixed, then this should be set to a very high value
            // if, like MAST, the actionHeuristic is dynamic, then this should be set to a lower value as estimates may
            // change over the course of the search. Setting it to 1 will update it on every visit; but possibly
            // at a high additional computational cost.
            if (params.actionHeuristic != IActionHeuristic.nullReturn) {
                if (pds.actionValueEstimates.isEmpty() || nVisits % params.actionHeuristicRecalculationThreshold == 0) {
                    // in this case we initialise all action values
                    double[] heuristicScores = params.actionHeuristic.evaluateAllActions(pds.actionsFromOpenLoopState, actionState);
                    for (int i = 0; i < pds.actionsFromOpenLoopState.size(); i++) {
                        pds.actionValueEstimates.put(pds.actionsFromOpenLoopState.get(i), heuristicScores[i]);
                    }
                } else {
                    // we just initialise the new actions
                    for (AbstractAction action : pds.actionsFromOpenLoopState) {
                        if (!pds.actionValueEstimates.containsKey(action)) {
                            pds.actionValueEstimates.put(action, params.actionHeuristic.evaluateAction(action, actionState, pds.actionsFromOpenLoopState));
                        }
                    }
                }
            } else {
                params.pUCTTemperature = 10001.0;
                params.progressiveBias = 0.0;
                params.initialiseVisits = 0;
                params.progressiveWideningConstant = 0.0;
            }
        }
        if (params.pUCTTemperature < 10000.0) {
            // construct the pdf for the pUCT selection
            // This ignores Progressive widening. This should not be a major issue, but means the pdf is calculated
            // over all possible actions, rather than just the ones we are considering
            // Generally if using pUCT we would expect FPU to also be used to give effective pruning, rather than the
            // explicit pruning of Progressive Widening.
            double[] pdf;
            pds.actionPDFEstimates = new HashMap<>();
            if (params.pUCTTemperature > 0.0) {
                // in this case we construct a Boltzmann
                double[] heuristicScores = pds.actionsFromOpenLoopState.stream().
                        mapToDouble(a -> pds.actionValueEstimates.getOrDefault(a, 0.0)).toArray();
                pdf = pdf(exponentiatePotentials(heuristicScores, params.pUCTTemperature));

            } else {
                // in this case, we first set any negative values to zero, and then construct the pdf directly
                double[] heuristicScores = pds.actionsFromOpenLoopState.stream().
                        mapToDouble(a -> Math.max(0.0, pds.actionValueEstimates.getOrDefault(a, 0.0))).toArray();
                pdf = pdf(heuristicScores);
            }
            for (int i = 0; i < pds.actionsFromOpenLoopState.size(); i++) {
                pds.actionPDFEstimates.put(pds.actionsFromOpenLoopState.get(i), pdf[i]);
            }
        }
        for (AbstractAction action : pds.actionsFromOpenLoopState) {
            if (!pds.actionValues.containsKey(action)) {
                pds.actionValues.put(action, new ActionStats(actionState.getNPlayers()));
                // This *does* rely on a good equals method being implemented for Actions
                if (!pds.actionValues.containsKey(action.copy()))
                    throw new AssertionError("We have an action that does not obey the equals/hashcode contract" + action);
                // mark a new node to be expanded (except at a multi-actor node where the children are keyed by the joint action
                if (!isMultiActor())
                    children.put(action.copy(), null);
                // Then we seed the statistics with heuristic biases (if so parameterised)
                // This assumes that we have had params.initialiseVisits trials of each action before we start
                if (params.initialiseVisits > 0) {
                    // This also ignores Progressive widening and initialises all possible actions
                    // As with pUCT, this won't cause any major issues, but will mean that the effective node visits
                    // will be higher than the visits of the considered actions.
                    ActionStats stats = pds.actionValues.get(action);
                    double actionEstimate = pds.actionValueEstimates.getOrDefault(action, 0.0);
                    if (params.normaliseRewards) {
                        if (actionEstimate > root.highReward) root.highReward = actionEstimate;
                        if (actionEstimate < root.lowReward) root.lowReward = actionEstimate;
                    }
                    int nActions = Math.max(pds.actionValues.size(), pds.actionsFromOpenLoopState.size());
                    stats.nVisits = params.initialiseVisits;
                    stats.validVisits = params.initialiseVisits * nActions;
                    stats.totValue[actingPlayer] = actionEstimate * params.initialiseVisits;
                    stats.squaredTotValue[actingPlayer] = actionEstimate * actionEstimate * params.initialiseVisits;
                    if (params.paranoid) // default to zero for other players, unless we're paranoid
                        for (int i = 0; i < actionState.getNPlayers(); i++)
                            if (i != actingPlayer)
                                stats.totValue[i] = -stats.totValue[actingPlayer];
                    if (nVisits < params.initialiseVisits * nActions) {
                        nVisits = params.initialiseVisits * nActions;
                    }
                }
            }
        }
    }

    protected void initialiseRootMetrics() {
        timeTaken = 0.0;
        initialisationTimeTaken = 0.0;
        nodeClash = 0;
        rolloutActionsTaken = 0;
        for (PlayerDecisionStats pds : statsByPlayer.values())
            pds.regretMatchingAverage.clear();
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
            // before each search iteration, we reset the forward model (and its decorators)
            forwardModel.reset();

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
        return actionVisits(decisionPlayer, action);
    }

    public int actionVisits(int actingPlayer, AbstractAction action) {
        return actionVisits(statsFor(actingPlayer), action);
    }

    private int actionVisits(PlayerDecisionStats pds, AbstractAction action) {
        ActionStats stats = pds.actionValues.get(action);
        return stats == null ? 0 : stats.nVisits;
    }

    public int actionValidVisits(AbstractAction action) {
        return actionValidVisits(decisionPlayer, action);
    }

    public int actionValidVisits(int actingPlayer, AbstractAction action) {
        ActionStats stats = statsFor(actingPlayer).actionValues.get(action);
        return stats == null ? 0 : stats.validVisits;
    }

    /**
     * The action-heuristic value estimate for an action (as computed for move ordering / progressive bias),
     * or 0.0 if none has been recorded.
     */
    public double actionHeuristicValue(AbstractAction action) {
        return actionHeuristicValue(decisionPlayer, action);
    }

    public double actionHeuristicValue(int actingPlayer, AbstractAction action) {
        return statsFor(actingPlayer).actionValueEstimates.getOrDefault(action, 0.0);
    }

    /**
     * The actions that are currently valid at this node (those available from the open-loop state of the
     * most recent iteration). Note this can differ from {@link #getChildren()} keys, which may include
     * actions retained from a reused tree that are no longer valid.
     */
    public List<AbstractAction> getActionsFromOpenLoopState() {
        return getActionsFromOpenLoopState(decisionPlayer);
    }

    public List<AbstractAction> getActionsFromOpenLoopState(int actingPlayer) {
        return statsFor(actingPlayer).actionsFromOpenLoopState;
    }

    private int validVisitsFor(PlayerDecisionStats pds, AbstractAction action) {
        if (params.information == Closed_Loop)
            return nVisits;
        ActionStats stats = pds.actionValues.get(action);
        return stats == null ? 1 : stats.validVisits;
    }

    /**
     * Note that {@code playerId} here is the index into the <i>reward</i> vector, not a selector for
     * whose statistics table to read - the two are different ideas that happen to coincide today.
     * Where the table has to be chosen, a {@link PlayerDecisionStats} is passed rather than a second
     * int, so the two can never be swapped by accident.
     */
    public double actionTotValue(AbstractAction action, int playerId) {
        return actionTotValue(statsFor(decisionPlayer), action, playerId);
    }

    private double actionTotValue(PlayerDecisionStats pds, AbstractAction action, int playerId) {
        ActionStats stats = pds.actionValues.get(action);
        return stats == null ? 0.0 : stats.totValue[playerId];
    }

    public double nodeValue(int playerId) {
        return nVisits == 0 ? 0.0 : totValue[playerId] / nVisits;
    }

    private double nodeValue(PlayerDecisionStats pds, int playerId) {
        if (nVisits == 0) return 0.0;
        return pds.actionValues.values().stream().mapToDouble(s -> s.totValue[playerId]).sum() / nVisits;
    }

    private double actionSquaredValue(PlayerDecisionStats pds, AbstractAction action, int playerId) {
        ActionStats stats = pds.actionValues.get(action);
        return stats == null ? 0.0 : stats.squaredTotValue[playerId];
    }

    /**
     * Uses only by TreeStatistics and bestAction() after mctsSearch()
     * For this reason not converted to old-style java loop as there would be no performance gain
     */
    int[] actionVisits() {
        return actionVisitCounts(decisionPlayer);
    }

    /**
     * Named apart from actionVisits(): overloading that on int would make the method reference
     * root::actionVisits ambiguous against actionVisits(AbstractAction) at its existing call sites.
     */
    int[] actionVisitCounts(int actingPlayer) {
        return statsFor(actingPlayer).actionValues.values().stream()
                .mapToInt(a -> a.nVisits)
                .toArray();
    }

    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *  This is only called on the root node and then navigates the tree within this loop (it is not recursive)
     *
     * @return - new node added to the tree.
     */
    protected SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.hasDecisionToMakeInTree() && cur.depth < params.maxTreeDepth) {
            // Move to next child given by relevant selection function. At a multi-actor node this
            // is one selection per acting player, combined into a SimultaneousAction.
            AbstractAction chosen = cur.jointTreePolicyAction(true);

            // In Open_Loop (and all variants other than Closed_Loop), we make a single copy of the state at the start of each iteration
            // this is then updated with all actions (and stored in openLoopState on each node it visits).
            // In Closed_Loop we make a copy of a state only when we expand and add a new node to the tree.
            if (params.information == Closed_Loop) {
                // we do not advance, but do track the actions taken (otherwise done in advanceState)
                // -1 for a joint action at a multi-actor node, as advanceState records it
                actionsInTree.add(new Pair<>(cur.isMultiActor() ? -1 : cur.decisionPlayer, chosen));
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
    /**
     * Whether the tree policy can select an action here: every acting player is still in the game
     * and has at least one candidate action this iteration. In a sequential search this is just the
     * current player.
     */
    protected boolean hasDecisionToMakeInTree() {
        if (!isMultiActor())
            return openLoopState.isNotTerminalForPlayer(decisionPlayer) && !statsFor(decisionPlayer).actionsFromOpenLoopState.isEmpty();
        for (int p : actingPlayers) {
            if (!openLoopState.isNotTerminalForPlayer(p) || statsFor(p).actionsFromOpenLoopState.isEmpty())
                return false;
        }
        return true;
    }

    protected List<AbstractAction> actionsToConsider(List<AbstractAction> allAvailable) {
        return actionsToConsider(decisionPlayer, allAvailable);
    }

    protected List<AbstractAction> actionsToConsider(int actingPlayer, List<AbstractAction> allAvailable) {
        PlayerDecisionStats pds = statsFor(actingPlayer);
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
            sortedActions.sort(Comparator.comparingDouble(a -> -pds.actionValueEstimates.getOrDefault(a, 0.0) -
                    pds.actionValues.getOrDefault(a, new ActionStats(1)).nVisits * 1e-6));
            return new ArrayList<>(sortedActions.subList(0, actionsToConsider));
        }
        return new ArrayList<>(allAvailable);
    }

    protected SingleTreeNode expandNode(AbstractAction actionCopy, AbstractGameState nextState) {
        // then instantiate a new node
        SingleTreeNode tn = createChildNode(actionCopy, nextState);
        // It is possible that we are expanding a node because a different player is the next to act
        SingleTreeNode[] newNodeArray = children.get(actionCopy);
        if (newNodeArray == null)
            newNodeArray = new SingleTreeNode[nextState.getNPlayers() + 1];  // see childSlot() for the extra slot
        newNodeArray[childSlot(nextState)] = tn;
        children.put(actionCopy, newNodeArray);
        return tn;
    }

    /**
     * The index in a child array at which the node for the given next state is stored. A single-actor
     * state is filed under its current player. A state in which several players decide
     * (decoupled search) is filed in the extra slot at index nPlayers
     */
    protected int childSlot(AbstractGameState nextState) {
        return actingPlayersAt(nextState).size() > 1 ? nextState.getNPlayers() : nextState.getCurrentPlayer();
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
            // -1 for a joint action at a multi-actor node; backUp() and MASTBackup() expand it
            // this is the one time (actionsInTree) where -1 is a valid number for an acting player
            int actingPlayer = actingPlayersAt(gs).size() > 1 ? -1 : gs.getCurrentPlayer();
            root.actionsInTree.add(new Pair<>(actingPlayer, act));
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
    public AbstractAction treePolicyAction(boolean explore) {
        return treePolicyAction(decisionPlayer, explore);
    }

    public AbstractAction treePolicyAction(int actingPlayer, boolean explore) {
        if (params.opponentTreePolicy == SelfOnly && parent != null && openLoopState != null && openLoopState.getCurrentPlayer() != decisionPlayer)
            throw new AssertionError("An error has occurred. SelfOnly should only call uct when we are moving.");

        // actionsToConsider takes care of any Progressive Widening in play, so we only consider the
        // widened subset
        PlayerDecisionStats pds = statsFor(actingPlayer);
        List<AbstractAction> availableActions = actionsToConsider(actingPlayer, getActionsFromOpenLoopState(actingPlayer));
        if (availableActions.isEmpty())
            throw new AssertionError("We need to have at least one option");

        AbstractAction actionChosen;
        if (availableActions.size() == 1) {
            actionChosen = availableActions.get(0);
        } else {
            // first we shuffle to break ties
            Collections.shuffle(availableActions, rnd);
            // then get the statsFor(decisionPlayer).actionValues
            double[] policyValues = actionValues(actingPlayer, availableActions);
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
                        if (policyValues[i] > bestValue) {
                            bestValue = policyValues[i];
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
                        double uctValue = ucbValue(pds, availableAction);
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
                    double[] pdf = pdf(policyValues);
                    long nonZeroActions = Arrays.stream(policyValues).filter(v -> v > 0.0).count();
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
     * The action to take from this node on the way down the tree. At a single-actor node that is
     * the decision player's action. At a multi-actor node every
     * acting player chooses independently and the choices are combined
     * into a SimultaneousAction.
     */
    protected AbstractAction jointTreePolicyAction(boolean explore) {
        if (!isMultiActor())
            return treePolicyAction(decisionPlayer, explore);
        Map<Integer, AbstractAction> choices = new LinkedHashMap<>();
        for (int p : actingPlayers)
            choices.put(p, treePolicyAction(p, explore));
        return new SimultaneousAction(choices);
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
            SingleTreeNode nextNode = nodeArray[childSlot(openLoopState)];
            if (nextNode != null)
                nextNode.setActionsFromOpenLoopState(openLoopState);
            return nextNode;
        }
    }

    public ActionStats getActionStats(AbstractAction action) {
        return getActionStats(decisionPlayer, action);
    }

    public ActionStats getActionStats(int actingPlayer, AbstractAction action) {
        return statsFor(actingPlayer).actionValues.get(action);
    }

    /**
     * The statistics for the specified acting player at this node
     */
    private PlayerDecisionStats statsFor(int actingPlayer) {
        return statsByPlayer.computeIfAbsent(actingPlayer, PlayerDecisionStats::new);
    }

    /**
     * The action statistics for this node, keyed by action. This is a live map, not a copy -
     * callers within the package rely on being able to see later updates through it.
     */
    public Map<AbstractAction, ActionStats> getActionValues() {
        return getActionValues(decisionPlayer);
    }

    public Map<AbstractAction, ActionStats> getActionValues(int actingPlayer) {
        return statsFor(actingPlayer).actionValues;
    }

    public List<Pair<Integer, AbstractAction>> getActionsInRollout() {
        return actionsInRollout;
    }

    public List<Pair<Integer, AbstractAction>> getActionsInTree() {
        return actionsInTree;
    }

    // Returns the values according to the selection policy (UCB, EXP3, etc.)
    // This is stage 1 of processing, before we use these to pick an action to take
    public double[] actionValues(List<AbstractAction> actionsToConsider) {
        return actionValues(decisionPlayer, actionsToConsider);
    }

    public double[] actionValues(int actingPlayer, List<AbstractAction> actionsToConsider) {
        PlayerDecisionStats pds = statsFor(actingPlayer);
        double[] retValue = new double[actionsToConsider.size()];
        for (int i = 0; i < actionsToConsider.size(); i++) {
            AbstractAction action = actionsToConsider.get(i);
            retValue[i] = switch (params.treePolicy) {
                case Uniform -> 1.0;
                case Greedy -> getFullValue(pds, action);
                case UCB, AlphaGo, UCB_Tuned -> ucbValue(pds, action);
                case RegretMatching, NoAveragingRM -> rmValue(pds, action);
                case EXP3 -> exp3Value(pds, action);
            };
        }
        return retValue;
    }

    private double getFullValue(PlayerDecisionStats pds, AbstractAction action) {
        double value = getActionValue(pds, action);
        int actionVisits = actionVisits(pds, action);
        if (params.normaliseRewards && actionVisits > 0) {
            value = normalise(value, root.lowReward, root.highReward);
        }
        if (params.progressiveBias > 0)
            value += getBiasValue(pds, action);
        // apply OMA
        value = getOMAValue(pds, action, value);
        return value;
    }

    private double getOMAValue(PlayerDecisionStats pds, AbstractAction action, double childValue) {
        int actionVisits = actionVisits(pds, action);
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

    private double ucbValue(PlayerDecisionStats pds, AbstractAction action) {

        // Find 'UCB' value - this is the base to which we then add exploration
        double childValue = getFullValue(pds, action);
        int actionVisits = actionVisits(pds, action);

        // Now for the exploration term
        // default to standard UCB
        int effectiveTotalVisits = validVisitsFor(pds, action);
        // use first play urgency as replacement for exploration term if action not previously taken
        double explorationTerm = params.firstPlayUrgency;
        if (actionVisits > 0) {
            explorationTerm = switch (params.treePolicy) {
                case UCB_Tuned -> {
                    double range = root.highReward - root.lowReward;
                    if (range < 1e-6) range = 1e-6;
                    double meanSq = actionSquaredValue(pds, action, pds.player) / actionVisits;
                    double standardVar = 0.25;
                    if (params.normaliseRewards) {
                        // we also need to standardise the sum of squares to calculate the variance
                        meanSq = (meanSq
                                + root.lowReward * root.lowReward
                                - 2 * root.lowReward * actionTotValue(pds, action, pds.player) / actionVisits
                        ) / (range * range);
                    } else {
                        // we need to modify the standard variance as it is not on a 0..1 basis (which is where 0.25 comes from)
                        standardVar = Math.sqrt(range / 2.0);
                    }
                    double variance = Math.max(0.0, meanSq - childValue * childValue);
                    double minTerm = Math.min(standardVar, variance + Math.sqrt(2 * Math.log(effectiveTotalVisits) / actionVisits));
                    yield params.K * Math.sqrt(Math.log(effectiveTotalVisits) / actionVisits * minTerm);
                }
                case AlphaGo -> params.K * Math.sqrt(effectiveTotalVisits) / actionVisits;
                default -> params.K * Math.sqrt(Math.log(effectiveTotalVisits) / actionVisits);
            };
        }
        if (params.pUCTTemperature < 10000.0) {
            // in this case we multiply the exploration term by the pUCT factor (the probability that the action would be taken by
            // our actionHeuristic). These were calculated in setActionsFromOpenLoopState
            explorationTerm *= pds.actionPDFEstimates.get(action);
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
        return exp3Value(statsFor(decisionPlayer), action);
    }

    private double exp3Value(PlayerDecisionStats pds, AbstractAction action) {
        double actionValue = getActionValue(pds, action);
        int actionVisits = actionVisits(pds, action);
        // we then normalise to [0, 1], or we subtract the mean action value to get an advantage (and reduce risk of
        // NaN or Infinities when we exponentiate)
        if (actionVisits > 0) {
            if (params.normaliseRewards)
                actionValue = normalise(actionValue, root.lowReward, root.highReward);
            else
                actionValue = actionValue - nodeValue(pds, pds.player);
        }
        if (params.progressiveBias > 0)
            actionValue += getBiasValue(pds, action);
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
        return rmValue(statsFor(decisionPlayer), action);
    }

    private double rmValue(PlayerDecisionStats pds, AbstractAction action) {
        double actionValue = getActionValue(pds, action);
        if (params.progressiveBias > 0)
            actionValue += getBiasValue(pds, action);
        double nodeValue = nodeValue(pds, pds.player);
        // potential value is our estimate of our accumulated reward if we had always taken this action
        double potentialValue = actionValue * nVisits;
        double regret = potentialValue - nodeValue * nVisits;
        // We add FPU after all the exponentiation for safety
        int actionVisits = actionVisits(pds, action);
        if (actionVisits == 0) {
            regret += params.firstPlayUrgency;
        }
        return Math.max(0.0, regret);
    }

    private double getActionValue(PlayerDecisionStats pds, AbstractAction action) {
        int actionVisits = actionVisits(pds, action);
        // if we are at 'expansion' phase, then we break ties by expansion policy (which is the same actionHeuristic as progressive bias)
        return actionVisits > 0 ? actionTotValue(pds, action, pds.player) / actionVisits : 0.0;
    }

    private double getBiasValue(PlayerDecisionStats pds, AbstractAction action) {
        int actionVisits = actionVisits(pds, action);
        return params.progressiveBias * pds.actionValueEstimates.getOrDefault(action, 0.0) / (actionVisits + 1);
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
            if (action instanceof SimultaneousAction) {
                if (!n.isMultiActor())
                    throw new AssertionError("A joint action was taken at a node with a single acting player");
            } else if (n.decisionPlayer != actingPlayer)
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
    /**
     * Record one player's decision at this node: which of their actions were valid on the way
     * through (so that valid-visit counts stay meaningful under progressive widening), and the
     * result for the one they actually took.
     * <p>
     * Split out of backUpSingleNode so that a node with several acting players can run this once
     * per player while running the policy tail below it exactly once. Note that node nVisits is
     * deliberately NOT incremented here - there is one node, so there is one visit count.
     *
     * @return the actions considered, for the caller to reuse - see the note at the call site.
     */
    protected List<AbstractAction> updateActionStats(int actingPlayer, AbstractAction actionTaken, double[] result) {
        PlayerDecisionStats pds = statsFor(actingPlayer);
        // Here we look at the actions from the open loop state to see which ones were valid
        // when we passed through, and keep track of valid visits
        List<AbstractAction> actionsToConsider = actionsToConsider(actingPlayer, pds.actionsFromOpenLoopState);

        // then we update the statistics for the action taken
        if (!actionsToConsider.contains(actionTaken)) {
            if (params.opponentTreePolicy != MCGS && params.opponentTreePolicy != MCGSSelfOnly)
                throw new AssertionError("We have somehow failed to find the action taken in the list of valid actions");

            // If MCGS, then this is possible if we have looped in the graph, so that OpenLoopState refers
            // to a different state than the one for which the action was taken. This is awkward.
            // In the absence of any good information, we just increment the valid visits of all actions
            for (ActionStats stats : pds.actionValues.values()) {
                stats.validVisits++;
            }
        } else {
            for (AbstractAction action : actionsToConsider) {
                if (!pds.actionValues.containsKey(action))
                    pds.actionValues.put(action, new ActionStats(result.length));
                pds.actionValues.get(action).validVisits++;
            }
        }
        ActionStats stats = pds.actionValues.get(actionTaken);
        if (stats == null)
            throw new AssertionError("We have somehow failed to find the action taken in the list of actions");
        if (stats.validVisits == 0)
            throw new AssertionError("We have somehow failed to find the action taken in the list of valid actions");

        stats.update(result);
        return actionsToConsider;
    }

    protected double[] backUpSingleNode(AbstractAction actionTaken, double[] result) {
        if (params.discardStateAfterEachIteration) {
            if (depth > 0)
                openLoopState = null; // releases for Garbage Collection
            if (depth > 0 && !params.maintainMasterState)
                state = null;
        }
        nVisits++;
        if (totValue == null) totValue = new double[result.length];
        for (int i = 0; i < result.length; i++) totValue[i] += result[i];
        if (isMultiActor()) {
            // Decoupled backup: each acting player credits their own component of the joint action,
            // against their own table. The regret-matching refresh is per player too.
            SimultaneousAction joint = (SimultaneousAction) actionTaken;
            for (int p : actingPlayers) {
                AbstractAction component = joint.getPlayerActions().get(p);
                if (component == null)
                    throw new AssertionError("Joint action " + joint + " has no component for acting player " + p);
                List<AbstractAction> considered = updateActionStats(p, component, result);
                if (params.treePolicy == RegretMatching) {
                    int updateEvery = Math.max(considered.size(), 10);
                    if (nVisits >= updateEvery && nVisits % updateEvery == 0)
                        updateRegretMatchingAverage(p, considered);
                }
            }
            // The Lambda / MaxLambda / MaxMC tails below are only valid with sequential tree search, so the
            // backup here is plain Monte Carlo whatever the policy.
            // TODO: We could implement a MAX backup with simultaneous action nodes - we can calculate for each player the best action (marginalising out the results of the other players)
            // TODO: Then we interpolate with this MAX value for each player separately
            return result;
        }
        // The per-acting-player part of the backup
        List<AbstractAction> actionsToConsider = updateActionStats(decisionPlayer, actionTaken, result);
        ActionStats stats = getActionValues(decisionPlayer).get(actionTaken);

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
        double[] maxValue = statsFor(decisionPlayer).actionValues.get(bestAction).totValue.clone();
        for (int i = 0; i < maxValue.length; i++) {
            maxValue[i] /= statsFor(decisionPlayer).actionValues.get(bestAction).nVisits;
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
        return bestAction(statsFor(decisionPlayer), actionsToConsider);
    }

    private AbstractAction bestAction(PlayerDecisionStats pds, List<AbstractAction> actionsToConsider) {
        AbstractAction bestAction = null;
        double maxValue = -Double.MAX_VALUE;
        for (AbstractAction action : actionsToConsider) {
            ActionStats temp = pds.actionValues.get(action);
            double value = temp.nVisits == 0 ? -Double.MAX_VALUE :
                    temp.totValue[pds.player] / temp.nVisits;
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
            if (pair.b instanceof SimultaneousAction joint) {
                // A joint action from a multi-actor tree node: MAST is per player per component
                // action, so expand it here. The list itself must stay index-aligned with currentNodeTrajectory.
                for (Map.Entry<Integer, AbstractAction> e : joint.getPlayerActions().entrySet())
                    MASTBackupOne(e.getKey(), e.getValue(), delta);
            } else {
                MASTBackupOne(pair.a, pair.b, delta);
            }
        }
    }

    private void MASTBackupOne(int player, AbstractAction action, double[] delta) {
        Object actionKey = params.MASTActionKey == null ? action.copy() : params.MASTActionKey.key(action);
        Pair<Integer, Double> stats = MASTStatistics.get(player).getOrDefault(actionKey, new Pair<>(0, 0.0));
        stats.a++;  // visits
        stats.b += delta[player];   // value
        MASTStatistics.get(player).put(actionKey, stats);
    }

    /**
     * Calculates the best action from the root according to the selection policy
     *
     * @return - the best AbstractAction
     */
    public AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        if (params.treePolicy == EXP3) {
            // EXP3 uses the tree policy (without exploration)
            bestAction = treePolicyAction(false);
        } else if (params.treePolicy == RegretMatching) {
            // RM uses a special policy as the average of all previous root policies
            if (statsFor(decisionPlayer).regretMatchingAverage.isEmpty()) // in case we have not yet updated the regret matching average
                updateRegretMatchingAverage(actionsToConsider(statsFor(decisionPlayer).actionsFromOpenLoopState));
            bestAction = regretMatchingAverage();
        } else {
            // We iterate through all actions valid in the original root state
            // as openLoopState is fine with SingleTreeNode or MultiTreeNode
            List<AbstractAction> availableActions = actionsToConsider(statsFor(decisionPlayer).actionsFromOpenLoopState);
            if (state != null && (
                    (redeterminisationPlayer != -1 && redeterminisationPlayer != decisionPlayer)
                            || params.opponentTreePolicy == MCGS
                            || params.opponentTreePolicy == MCGSSelfOnly)) {
                // In these cases we need to recompute the available actions from the root state to ensure that
                // we only consider the ones that are valid in the caller (in MCGS case it is possible that we have a loop round to the root)
                // At a decoupled root the state's current player is still the decision owner
                availableActions = actionsToConsider(forwardModel.computeAvailableActions(state, params.actionSpace, decisionPlayer));
            }
            List<Pair<AbstractAction, Double>> tempValues = new ArrayList<>();
            for (AbstractAction action : availableActions) {
                if (!statsFor(decisionPlayer).actionValues.containsKey(action)) {
                    throw new AssertionError("Hashcode / equals contract issue for " + action);
                }
                double childValue = getValue(action);
                // Apply small noise to break ties randomly
                childValue = noise(childValue, params.noiseEpsilon, rnd.nextDouble());

                tempValues.add(Pair.of(action, childValue));
                // Save best value
                if (childValue > bestValue) {
                    bestValue = childValue;
                    bestAction = action;
                }
            }
            // DDA
            if (params.DDAGameThreshold < bestValue) {
                // we are in DDA territory. We deliberately take a sub-optimal action if we can
                tempValues.sort(Comparator.comparing(p -> -p.b));
                for (Pair<AbstractAction, Double> pair : tempValues) {
                    if (pair.b > bestValue - params.DDAMoveThreshold ) {
                        bestAction = pair.a;
                        bestAction.setSaveGame(true);
                    } else {
                        break;  // now in the really poor moves
                    }
                }
            }
        }

        if (bestAction == null) {
            if (nVisits < 2) {
                bestAction = statsFor(decisionPlayer).actionValues.keySet().stream().findFirst().orElseThrow(() -> new AssertionError("No children"));
            } else
                throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }

    public double getValue(AbstractAction action) {
        if (statsFor(decisionPlayer).actionValues.get(action) != null) {
            ActionStats stats = statsFor(decisionPlayer).actionValues.get(action);
            if (params.selectionPolicy == SIMPLE)
                return stats.totValue[decisionPlayer] / (stats.nVisits + params.noiseEpsilon);
            else
                return stats.nVisits;  // ROBUST
        }
        return 0.0;
    }

    protected void updateRegretMatchingAverage(List<AbstractAction> actionsToConsider) {
        updateRegretMatchingAverage(decisionPlayer, actionsToConsider);
    }

    protected void updateRegretMatchingAverage(int actingPlayer, List<AbstractAction> actionsToConsider) {
        double[] av = actionValues(actingPlayer, actionsToConsider);
        double[] pdf = pdf(av);
        for (int i = 0; i < actionsToConsider.size(); i++) {
            statsFor(actingPlayer).regretMatchingAverage.merge(actionsToConsider.get(i), pdf[i], Double::sum);
        }
    }

    protected AbstractAction regretMatchingAverage() {
        return regretMatchingAverage(decisionPlayer);
    }

    protected AbstractAction regretMatchingAverage(int actingPlayer) {
        List<AbstractAction> actionsToConsider = actionsToConsider(actingPlayer, getActionsFromOpenLoopState(actingPlayer));
        double[] potentials = new double[actionsToConsider.size()];
        int count = 0;
        for (AbstractAction action : actionsToConsider) {
            potentials[count] = statsFor(actingPlayer).regretMatchingAverage.getOrDefault(action, 0.0);
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
        // one block of action statistics per acting player; a sequential node has exactly one
        List<Integer> players = isMultiActor() ? actingPlayers : List.of(decisionPlayer);
        int nActions = players.stream().mapToInt(p -> statsFor(p).actionValues.size()).sum();
        String valueString = decisionPlayer == -1 ? "n/a" : String.format("%.2f", nodeValue(decisionPlayer));
        if (!params.opponentTreePolicy.selfOnlyTree && openLoopState != null) {
            valueString = IntStream.range(0, openLoopState.getNPlayers())
                    .mapToDouble(this::nodeValue)
                    .mapToObj(v -> String.format("%.2f", v))
                    .collect(joining(", "));
        }
        retValue.append(String.format("%d total visits, value %s, with %d children, %d actions, depth %d, FMCalls %d: \n",
                nVisits, valueString, children.size(), nActions, depth, fmCallsCount));
        for (int p : players) {
            PlayerDecisionStats pds = statsFor(p);
            if (isMultiActor())
                retValue.append(String.format("    Player %d:\n", p));
            // sort all actions by visit count
            List<AbstractAction> sortedActions = pds.actionValues.keySet().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(a -> -actionVisits(pds, a)))
                    .toList();

            for (AbstractAction action : sortedActions) {
                String actionName = action.toString();
                int actionVisits = actionVisits(pds, action);
                int effectiveVisits = validVisitsFor(pds, action);
                if (actionName.length() > 50)
                    actionName = actionName.substring(0, 50);
                valueString = String.format("%.2f", actionTotValue(pds, action, p) / actionVisits);
                if (params.opponentTreePolicy == OneTree) {
                    // the reward vector length is the player count, whether or not a state is held
                    int nPlayers = pds.actionValues.get(action).totValue.length;
                    valueString = IntStream.range(0, nPlayers)
                            .mapToObj(q -> String.format("%.2f", actionTotValue(pds, action, q) / actionVisits))
                            .collect(joining(", "));
                }
                retValue.append(String.format("\t%-50s  visits: %d (%d)\tvalue %s\n", actionName, actionVisits, effectiveVisits, valueString));
            }
        }

        if (!(root instanceof MultiTreeNode))
            retValue.append(new TreeStatistics(root));
        return retValue.toString();
    }

}
