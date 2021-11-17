package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.Information.Closed_Loop;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MaxN;
import static players.mcts.MCTSEnums.OpponentTreePolicy.SelfOnly;
import static players.mcts.MCTSEnums.Strategies.MAST;
import static utilities.Utils.entropyOf;
import static utilities.Utils.noise;

public class SingleTreeNode {
    // Root node of tree
    SingleTreeNode root;
    // Parent of this node
    SingleTreeNode parent;
    // Children of this node. The value is an Array because we have to cater for the possibility that the next decision
    // could be by any player - each of which would transition to a different Node OpenLoop search. (Closed Loop will
    // only ever have one position in the array populated: and similarly if we are using a SelfOnly tree).
    Map<AbstractAction, SingleTreeNode[]> children = new HashMap<>();
    List<Map<AbstractAction, Pair<Integer, Double>>> MASTStatistics; // a list of one Map per player. Action -> (visits, totValue)
    // Depth of this node
    final int depth;
    double highReward = Double.NEGATIVE_INFINITY;
    double lowReward = Double.POSITIVE_INFINITY;

    // Total value of this node
    private final double[] totValue;
    private final double[] totSquares;
    // the id of the player who makes the decision at this node
    final int decisionPlayer;
    // Number of visits
    private int nVisits;
    // Number of FM calls and State copies up until this node
    protected int fmCallsCount;
    protected int copyCount;
    // Parameters guiding the search
    protected final MCTSPlayer player;
    protected final Random rnd;

    // State in this node (closed loop)
    protected final AbstractAction actionToReach;
    protected final AbstractGameState state;
    protected AbstractGameState openLoopState;
    List<AbstractAction> actionsFromOpenLoopState;
    Map<AbstractAction, Double> advantagesOfActionsFromOLS = new HashMap<>();

    ToDoubleBiFunction<AbstractAction, AbstractGameState> advantageFunction = (a, s) -> advantagesOfActionsFromOLS.getOrDefault(a, 0.0);
    ToDoubleBiFunction<AbstractAction, AbstractGameState> MASTFunction;

    public AbstractGameState getState() {
        return state;
    }

    // Called in tree expansion
    public SingleTreeNode(MCTSPlayer player, SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state, Random rnd) {
        this.player = player;
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        MASTStatistics = new ArrayList<>();
        if (root == this) {
            // only root node maintains MAST statistics
            for (int i = 0; i < state.getNPlayers(); i++)
                MASTStatistics.add(new HashMap<>());
        }
        this.actionToReach = actionToReach;
        decisionPlayer = state.getCurrentPlayer();
        MASTFunction = (a, s) -> {
            Map<AbstractAction, Pair<Integer, Double>> MAST = MASTStatistics.get(decisionPlayer);
            if (MAST.containsKey(a)) {
                Pair<Integer, Double> stats = MAST.get(a);
                return stats.b / stats.a;
            }
            return 0.0;
        };

        totValue = new double[state.getNPlayers()];
        totSquares = new double[state.getNPlayers()];
        openLoopState = state;
        if (player.params.information != Closed_Loop) {
            // if we're using open loop, then we need to make sure the reference state is never changed
            root.copyCount++;
            this.state = state.copy();
        } else {
            this.state = state;
        }

        setActionsFromOpenLoopState(state);
        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
        this.rnd = rnd;
    }

    protected void setActionsFromOpenLoopState(AbstractGameState actionState) {
        actionsFromOpenLoopState = actionState.isNotTerminal() ? player.getForwardModel().computeAvailableActions(actionState) :
                Collections.emptyList();
        if (player.params.expansionPolicy == MAST) {
            advantagesOfActionsFromOLS = actionsFromOpenLoopState.stream()
                    .collect(toMap(a -> a, a -> MASTFunction.applyAsDouble(a, actionState)));
        } else {
            if (player.advantageFunction != null)
                advantagesOfActionsFromOLS = actionsFromOpenLoopState.stream()
                        .collect(toMap(a -> a, a -> player.advantageFunction.applyAsDouble(a, actionState)));
        }
        for (AbstractAction action : actionsFromOpenLoopState) {
            if (!children.containsKey(action)) {
                children.put(action, null); // mark a new node to be expanded
                // This *does* rely on a good equals method being implemented for Actions
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
        int remainingLimit = player.params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (player.params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(player.params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;
        boolean stop = false;
        while (!stop) {
            switch (player.params.information) {
                case Closed_Loop:
                    // no effect
                    break;
                case Open_Loop:
                    openLoopState = state.copy();
                    copyCount++;
                    break;
                case Information_Set:
                    openLoopState = state.copy(player.getPlayerID());
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
            PlayerConstants budgetType = player.params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if (budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= player.params.budget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCallsCount > player.params.budget || numIters > player.params.budget;
            } else if (budgetType == BUDGET_COPY_CALLS) {
                stop = copyCount > player.params.budget || numIters > player.params.budget;
            } else if (budgetType == BUDGET_FMANDCOPY_CALLS) {
                stop = (copyCount + fmCallsCount) > player.params.budget || numIters > player.params.budget;
            }
        }

        if (statsLogger != null)
            logTreeStatistics(statsLogger, numIters, elapsedTimer.elapsedMillis());
    }

    /**
     * oneSearchIteration() implements the strategy for tree search (plus expansion, rollouts, backup and so on)
     * Its result is purely stored in the tree generated from root
     */
    protected void oneSearchIteration() {
        double[] startingValues = IntStream.range(0, openLoopState.getNPlayers())
                .mapToDouble(i -> player.heuristic.evaluateState(openLoopState, i)).toArray();

        List<Pair<Integer, AbstractAction>> treeActions = new ArrayList<>();
        SingleTreeNode selected = treePolicy(treeActions);
        // Monte carlo rollout: return value of MC rollout from the newly added node
        List<Pair<Integer, AbstractAction>> rolloutActions = new ArrayList<>();
        double[] delta = selected.rollOut(rolloutActions, startingValues);
        // Back up the value of the rollout through the tree
        selected.backUp(delta);
        if (player.params.useMAST) {
            List<Pair<Integer, AbstractAction>> MASTActions = new ArrayList<>();
            switch (player.params.MAST) {
                case Rollout:
                    MASTActions = rolloutActions;
                    break;
                case Tree:
                    MASTActions = treeActions;
                    break;
                case Both:
                    MASTActions = rolloutActions;
                    MASTActions.addAll(treeActions);
                    break;
            }
            root.MASTBackup(MASTActions, delta);
        }
    }

    private void logTreeStatistics(IStatisticLogger statsLogger, int numIters, long timeTaken) {
        Map<String, Object> stats = new HashMap<>();
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
        statsLogger.record(stats);
    }

    /**
     * Uses plain java loop instead of streams for performance
     * (this is called often enough it can make a measurable difference)
     */
    private int actionVisits(AbstractAction action) {
        int retValue = 0;
        for (SingleTreeNode node : children.get(action)) {
            if (node != null)
                retValue += node.nVisits;
        }
        return retValue;
    }

    /**
     * Uses plain java loop instead of streams for performance
     * (this is called often enough it can make a measurable difference)
     */
    private double actionTotValue(AbstractAction action, int playerId) {
        double retValue = 0.0;
        for (SingleTreeNode node : children.get(action)) {
            if (node != null)
                retValue += node.totValue[playerId];
        }
        return retValue;
    }

    private double actionSquaredValue(AbstractAction action, int playerId) {
        double retValue = 0.0;
        for (SingleTreeNode node : children.get(action)) {
            if (node != null)
                retValue += node.totSquares[playerId];
        }
        return retValue;
    }

    /**
     * Uses only by TreeStatistics and bestAction() after mctsSearch()
     * For this reason not converted to old-style java loop as there would be no performance gain
     */
    private int[] actionVisits() {
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
        while (cur.openLoopState.isNotTerminalForPlayer(actingPlayer) && cur.depth < player.params.maxTreeDepth && cur.actionsFromOpenLoopState.size() > 0) {
            List<AbstractAction> unexpanded = cur.unexpandedActions();
            if (!unexpanded.isEmpty()) {
                // We have an unexpanded action
                AbstractAction chosen = cur.expand(unexpanded);
                // copy the current state and advance it using the chosen action
                // we first copy the action so that the one stored in the node will not have any state changes
                AbstractGameState nextState = cur.openLoopState.copy();
                root.copyCount++;
                AbstractAction actionCopy = chosen.copy();
                advance(nextState, actionCopy);
                // then create the new node
                return cur.expandNode(actionCopy, nextState);
            } else {
                // Move to next child given by UCT function
                AbstractAction chosen = cur.treePolicyAction();
                if (player.params.information != Closed_Loop) {
                    // We do not need to copy the state, as we advance this as we descend the tree.
                    // In open loop we never re-use the state...the only purpose of storing it on the Node is
                    // to pick it up in the next uct() call as we descend the tree
                    AbstractAction chosenCopy = chosen.copy();
                    advance(cur.openLoopState, chosenCopy);
                }
                cur = cur.nextNodeInTree(chosen);
                treeActions.add(new Pair<>(actingPlayer, cur.actionToReach));
            }
        }
        return cur;
    }

    protected List<AbstractAction> actionsToConsider(List<AbstractAction> allAvailable, int usedElsewhere) {
        if (!allAvailable.isEmpty() && player.params.progressiveWideningConstant >= 1.0) {
            int actionsToConsider = (int) Math.floor(player.params.progressiveWideningConstant * Math.pow(nVisits + 1, player.params.progressiveWideningExponent));
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
        List<AbstractAction> topActions = player.params.progressiveWideningConstant >= 1.0
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

        ToDoubleBiFunction<AbstractAction, AbstractGameState> valueFunction = player.params.expansionPolicy == MAST ? MASTFunction : advantageFunction;
        if (valueFunction != null) {
            double bestValue = Double.NEGATIVE_INFINITY;
            for (AbstractAction action : notChosen) {
                double estimate = valueFunction.applyAsDouble(action, state);
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
        SingleTreeNode tn = new SingleTreeNode(player, this, actionCopy, nextState, rnd);
        SingleTreeNode[] nodeArray = new SingleTreeNode[state.getNPlayers()];
        nodeArray[nextState.getCurrentPlayer()] = tn;
        if (player.params.opponentTreePolicy == SelfOnly && nextState.getCurrentPlayer() != decisionPlayer)
            throw new AssertionError("Awooga!");
        children.put(actionCopy, nodeArray);
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    protected void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
        if (player.params.opponentTreePolicy == SelfOnly && gs.getCurrentPlayer() != player.getPlayerID())
            advanceToTurnOfPlayer(gs, player.getPlayerID());
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
            AbstractPlayer oppModel = player.getOpponentModel(gs.getCurrentPlayer());
            List<AbstractAction> availableActions = player.getForwardModel().computeAvailableActions(gs);
            if (availableActions.isEmpty())
                throw new AssertionError("Should always have at least one action possible...");
            AbstractAction action = oppModel.getAction(gs, availableActions);
            player.getForwardModel().next(gs, action);
            root.fmCallsCount++;
        }
    }

    /**
     * Apply relevant policy to choose a child.
     *
     * @return - child node according to the tree policy
     */
    protected AbstractAction treePolicyAction() {

        if (player.params.opponentTreePolicy == SelfOnly && state.getCurrentPlayer() != player.getPlayerID())
            throw new AssertionError("An error has occurred. SelfOnly should only call uct when we are moving.");

        List<AbstractAction> availableActions = actionsToConsider(actionsFromOpenLoopState, 0);
        AbstractAction actionChosen;
        if (availableActions.size() == 1) {
            actionChosen = availableActions.get(0);
        } else {
            switch (player.params.treePolicy) {
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
                    throw new AssertionError("Unknown treepolicy: " + player.params.treePolicy);
            }
        }

        return actionChosen;
    }

    protected SingleTreeNode nextNodeInTree(AbstractAction actionChosen) {
        // Only advance the state if this is open loop
        SingleTreeNode[] nodeArray = children.get(actionChosen);
        if (player.params.information == Closed_Loop) {
            // in this case we have determinism...there should just be a single child node in the array...so we get that
            Optional<SingleTreeNode> next = Arrays.stream(nodeArray).filter(Objects::nonNull).findFirst();
            if (next.isPresent()) {
                return next.get();
            } else {
                throw new AssertionError("We have no node to move to...");
            }
        } else {
            int nextPlayer = openLoopState.getCurrentPlayer();
            SingleTreeNode nextNode = nodeArray[nextPlayer];
            if (nextNode == null) {
                // need to create a new node
                nodeArray[nextPlayer] = new SingleTreeNode(player, this, actionChosen.copy(), openLoopState, rnd);
                nextNode = nodeArray[nextPlayer];
            } else {
                // pick up the existing one, and set the state
                nextNode.openLoopState = openLoopState;
                nextNode.setActionsFromOpenLoopState(openLoopState);
            }
            // we also need to check to see if there are any new actions on this transition
            return nextNode;
        }
    }

    private AbstractAction ucb(List<AbstractAction> availableActions) {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;

        double nodeValue = totValue[decisionPlayer] / nVisits;

        for (AbstractAction action : availableActions) {
            SingleTreeNode[] childArray = children.get(action);
            if (childArray == null)
                throw new AssertionError("Should not be here");

            // Find child value
            double hvVal = actionTotValue(action, decisionPlayer);

            int actionVisits = actionVisits(action);
            double childValue = hvVal / (actionVisits + player.params.epsilon);

            // consider any progressive bias term
            if (player.params.biasVisits > 0) {
                double beta = Math.sqrt(player.params.biasVisits / (double) (player.params.biasVisits + 3 * actionVisits));
                childValue = (1.0 - beta) * childValue + beta * (advantagesOfActionsFromOLS.getOrDefault(action, 0.0) + nodeValue);
            }

            if (player.params.normaliseRewards) {
                childValue = Utils.normalise(childValue, root.lowReward, root.highReward);
            }

            // default to standard UCB
            double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (actionVisits + player.params.epsilon));
            // unless we are using a variant
            switch (player.params.treePolicy) {
                case AlphaGo:
                    explorationTerm = player.params.K * Math.sqrt(this.nVisits) / (actionVisits + 1.0);
                    break;
                case UCB_Tuned:
                    double range = root.highReward - root.lowReward;
                    double meanSq = actionSquaredValue(action, decisionPlayer) / (actionVisits + player.params.epsilon);
                    double standardVar = 0.25;
                    if (player.params.normaliseRewards) {
                        // we also need to standardise the sum of squares to calculate the variance
                        meanSq = (meanSq
                                + root.lowReward * root.lowReward
                                - 2 * root.lowReward * actionTotValue(action, decisionPlayer) / (actionVisits + player.params.epsilon)
                        ) / (range * range);
                    } else {
                        // we need to modify the standard variance as it is not on a 0..1 basis (which is where 0.25 comes from)
                        standardVar = Math.sqrt(range / 2.0);
                    }
                    double variance = meanSq - childValue * childValue;
                    double minTerm = Math.min(standardVar, variance + Math.sqrt(2 * Math.log(this.nVisits) / (actionVisits + player.params.epsilon)));
                    explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits) / (actionVisits + player.params.epsilon) * minTerm);
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
            uctValue = noise(uctValue, player.params.epsilon, player.rnd.nextDouble());

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
        double meanAdvantageFromAction = (actionValue / actionVisits) - (totValue[decisionPlayer] / nVisits);
        if (player.params.biasVisits > 0) {
            double beta = Math.sqrt(player.params.biasVisits / (double) (player.params.biasVisits + 3 * actionVisits));
            meanAdvantageFromAction = (1.0 - beta) * meanAdvantageFromAction + beta * advantagesOfActionsFromOLS.getOrDefault(action, 0.0);
        }
        return Math.exp(meanAdvantageFromAction);
    }

    public double rmValue(AbstractAction action) {
        // TODO: This is not quite correct for game in which not all actions are available for each visit
        // TODO: (see comment in checkActions() - to be enhanced to keep track of this at some future point)
        double actionValue = actionTotValue(action, decisionPlayer);
        int actionVisits = actionVisits(action);
        if (player.params.biasVisits > 0) {
            double beta = Math.sqrt(player.params.biasVisits / (double) (player.params.biasVisits + 3 * actionVisits));
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
        switch (player.params.treePolicy) {
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
        double exploreBonus = player.params.exploreEpsilon / actionToValueMap.size();
        Map<AbstractAction, Double> probabilityOfSelection = actionToValueMap.entrySet().stream().collect(
                toMap(Map.Entry::getKey, e -> e.getValue() * (1.0 - player.params.exploreEpsilon) + exploreBonus));

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
    protected double[] rollOut(List<Pair<Integer, AbstractAction>> rolloutActions, double[] startingValues) {
        int rolloutDepth = 0; // counting from end of tree

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = openLoopState;
        if (player.params.rolloutLength > 0) {
            if (player.params.information == Closed_Loop) {
                // the thinking here is that in openLoop we copy the state right at the root, and then use the forward
                // model at each action. Hence the current state on the node is the one we have been using up to now.
                /// Hence we do not need to copy it.
                rolloutState = state.copy();
                root.copyCount++;
            }

            while (!finishRollout(rolloutState, rolloutDepth)) {
                // we advance to the deciding player's turn. This ensures that the other agents use the respective
                // opponent models (if applicable) and not the rollout strategy
                // this also means that rollout depth only increments for actions by the decisionPlayer
                advanceToTurnOfPlayer(rolloutState, decisionPlayer);
                List<AbstractAction> availableActions = player.getForwardModel().computeAvailableActions(rolloutState);
                if (availableActions.isEmpty())
                    break;
                AbstractAction next = player.rolloutStrategy.getAction(rolloutState, availableActions);
                rolloutActions.add(new Pair<>(rolloutState.getCurrentPlayer(), next));
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        double[] retValue = new double[state.getNPlayers()];

        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = player.heuristic.evaluateState(rolloutState, i) - startingValues[i];
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
    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        if (depth >= player.params.rolloutLength)
            return true;

        // End of game (well, at least for the deciding player)
        return rollerState.isNotTerminalForPlayer(decisionPlayer);
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

        if (player.params.normaliseRewards || player.params.treePolicy == MCTSEnums.TreePolicy.UCB_Tuned) {
            DoubleSummaryStatistics stats = Arrays.stream(result).summaryStatistics();
            if (n.root.lowReward > stats.getMin())
                n.root.lowReward = stats.getMin();
            if (n.root.highReward < stats.getMax())
                n.root.highReward = stats.getMax();
        }
        while (n != null) {
            n.nVisits++;
            switch (player.params.opponentTreePolicy) {
                case SelfOnly:
                    for (int j = 0; j < result.length; j++) {
                        n.totValue[j] += result[root.decisionPlayer];
                        n.totSquares[j] += squaredResults[root.decisionPlayer];
                    }
                    break;
                case Paranoid:
                case MultiTreeParanoid:
                    for (int j = 0; j < result.length; j++) {
                        if (j == root.decisionPlayer) {
                            n.totValue[j] += result[root.decisionPlayer];
                            n.totSquares[j] += squaredResults[root.decisionPlayer];

                        } else {
                            n.totValue[j] -= result[root.decisionPlayer];
                            n.totSquares[j] += squaredResults[root.decisionPlayer];
                        }
                    }
                    break;
                case MaxN:
                case MultiTree:
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
            MASTStatistics.get(player).put(action, stats);
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

        MCTSEnums.SelectionPolicy policy = player.params.selectionPolicy;
        // check to see if all nodes have the same number of visits
        // if they do, then we use average score instead
        if (player.params.selectionPolicy == MCTSEnums.SelectionPolicy.ROBUST &&
                Arrays.stream(actionVisits()).boxed().collect(toSet()).size() == 1) {
            policy = MCTSEnums.SelectionPolicy.SIMPLE;
        }


        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                double childValue = actionVisits(action); // if ROBUST
                if (policy == MCTSEnums.SelectionPolicy.SIMPLE)
                    childValue = actionTotValue(action, decisionPlayer) / (actionVisits(action) + player.params.epsilon);

                // Apply small noise to break ties randomly
                childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());

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

    public int getActor() {
        return decisionPlayer;
    }

    public AbstractForwardModel getForwardModel() {
        return player.getForwardModel();
    }

    @Override
    public String toString() {
        // we return some interesting data on this node
        // child actions
        // visits and values for each
        StringBuilder retValue = new StringBuilder();
        String valueString = String.format("%.2f", totValue[decisionPlayer] / nVisits);
        if (player.params.opponentTreePolicy == MaxN) {
            valueString = Arrays.stream(totValue)
                    .mapToObj(v -> String.format("%.2f", v / nVisits))
                    .collect(joining(", "));
        }
        retValue.append(String.format("%s, %d total visits, value %s, with %d children, depth %d, FMCalls %d: \n",
                player, nVisits, valueString, children.size(), depth, fmCallsCount));
        // sort all actions by visit count
        List<AbstractAction> sortedActions = children.keySet().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(a -> -actionVisits(a)))
                .collect(toList());
        for (AbstractAction action : sortedActions) {
            String actionName = action.toString();
            int actionVisits = actionVisits(action);
            if (actionName.length() > 50)
                actionName = actionName.substring(0, 50);
            valueString = String.format("%.2f", actionTotValue(action, decisionPlayer) / actionVisits);
            if (player.params.opponentTreePolicy == MaxN) {
                valueString = IntStream.range(0, state.getNPlayers())
                        .mapToObj(p -> String.format("%.2f", actionTotValue(action, p) / actionVisits))
                        .collect(joining(", "));
            }
            retValue.append(String.format("\t%-50s  visits: %d\tvalue %s\n", actionName, actionVisits, valueString));
        }
        retValue.append(new TreeStatistics(root).toString());
        return retValue.toString();
    }
}
