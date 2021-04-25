package players.mcts;

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
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MaxN;
import static players.mcts.MCTSEnums.OpponentTreePolicy.SelfOnly;
import static players.mcts.MCTSEnums.TreePolicy.AlphaGo;
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
    List<Map<AbstractAction, Pair<Integer, Double>>> MASTStatistics;
    // Depth of this node
    final int depth;

    // Total value of this node
    private final double[] totValue;
    // the id of the player who makes the decision at this node
    private final int decisionPlayer;
    // Number of visits
    private int nVisits;
    // Number of FM calls and State copies up until this node
    private int fmCallsCount;
    private int copyCount;
    // Parameters guiding the search
    private final MCTSPlayer player;
    private final Random rnd;

    // State in this node (closed loop)
    private AbstractAction actionToReach;
    private AbstractGameState state;
    private List<AbstractAction> actionsFromState;

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
            // Root node maintains MAST statistics
            for (int i = 0; i < state.getNPlayers(); i++)
            MASTStatistics.add(new HashMap<>());
        }
        this.actionToReach = actionToReach;
        decisionPlayer = state.getCurrentPlayer();
        totValue = new double[state.getNPlayers()];
        setState(state); // this also initialises actions and children
        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
        this.rnd = rnd;
    }

    private void setState(AbstractGameState newState) {
        if (newState.getCurrentPlayer() != decisionPlayer)
            throw new AssertionError("Problem: We should never have a state assigned to this node for a different deciding player");
        state = newState;
        actionsFromState = player.getForwardModel().computeAvailableActions(state);
        /*
         * we run through the actions, and add any new ones not currently in the list
         * When in open loop, it is entirely possible that on a transition to a new state we have actions that were
         * not previously possible. (Especially in stochastic games, where different iterations may have different
         * random draws; but this can also happen in deterministic games if we model the opponent moves in their own trees
         * for example.)
         */
        for (AbstractAction action : actionsFromState) {
            if (!children.containsKey(action)) {
                children.put(action, null); // mark a new node to be expanded
                // This *does* rely on a good equals method being implemented for Actions
            }
        }
        // TODO: This does not yet take account of cases where we have rarely possible actions. Where the
        // action frequency can be very variable we should take this into account (see Cowling et al. 2012 I think)
        // This boils down to keeping track of how many times the action was available out of the total visits to the
        // node.
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
        // We keep a copy of this, as if we are using an open loop approach, then we need to advance a state
        // through the tree on each iteration, while still keeping an unchanged master copy (rootState)
        AbstractGameState rootState = state.copy();
        copyCount++;
        while (!stop) {
            if (player.params.openLoop) { // this assumes that copy(id) randomises the invisible components
                setState(player.params.redeterminise ? rootState.copy(player.getPlayerID()) : rootState.copy());
                copyCount++;
            } else
                setState(rootState);
            // TODO: Can we determinise in Closed Loop? Closed Loop currently means we do not advance the state though
            // the tree - so shuffling the cards at the root makes no difference.

            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            List<Pair<Integer, AbstractAction>> treeActions = new ArrayList<>();
            SingleTreeNode selected = treePolicy(treeActions);
            // Monte carlo rollout: return value of MC rollout from the newly added node
            List<Pair<Integer, AbstractAction>> rolloutActions = new ArrayList<>();
            double[] delta = selected.rollOut(rolloutActions);
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
                stop =  fmCallsCount > player.params.budget || numIters > player.params.budget;
            } else if (budgetType == BUDGET_COPY_CALLS) {
                stop = copyCount > player.params.budget || numIters > player.params.budget;
            } else if (budgetType == BUDGET_FMANDCOPY_CALLS) {
                stop = (copyCount + fmCallsCount) > player.params.budget || numIters > player.params.budget;
            }
        }

        if (statsLogger != null)
            logTreeStatistics(statsLogger, numIters, elapsedTimer.elapsedMillis());
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
    private SingleTreeNode treePolicy(List<Pair<Integer, AbstractAction>> treeActions) {

        SingleTreeNode cur = this;
        Integer actingPlayer = cur.decisionPlayer;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < player.params.maxTreeDepth && cur.actionsFromState.size() > 0) {
            if (!cur.unexpandedActions().isEmpty()) {
                // We have an unexpanded action
                return cur.expand();
            } else {
                // Move to next child given by UCT function
                cur = cur.nextNodeInTree();
            }
        }
        treeActions.add(new Pair<>(actingPlayer, cur.actionToReach));
        return cur;
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    private List<AbstractAction> unexpandedActions() {
        return actionsFromState.stream().filter(a -> children.get(a) == null).collect(toList());
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    private SingleTreeNode expand() {
        // Find random child not already created
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = null;
        if (player.params.expansionPolicy == MCTSEnums.Strategies.MAST) {
            Map<AbstractAction, Pair<Integer, Double>> MAST = MASTStatistics.get(decisionPlayer);
            double bestValue = Double.NEGATIVE_INFINITY;
            for (AbstractAction action : unexpandedActions()) {
                double estimate = rnd.nextDouble() / 100.0;
                if (MAST.containsKey(action)) {
                    Pair<Integer, Double> stats = MAST.get(action);
                    estimate = stats.b / stats.a;
                }
                if (estimate > bestValue) {
                    bestValue = estimate;
                    chosen = action;
                }
            }
        } else {
            chosen = notChosen.get(rnd.nextInt(notChosen.size()));
        }
        if (chosen == null)
            throw new AssertionError("We have somehow failed to pick an action to expand");
        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        AbstractGameState nextState = state.copy();
        root.copyCount++;
        AbstractAction actionCopy = chosen.copy();
        advance(nextState, actionCopy);

        // then instantiate a new node
        SingleTreeNode tn = new SingleTreeNode(player, this, actionCopy, nextState, rnd);
        SingleTreeNode[] nodeArray = new SingleTreeNode[state.getNPlayers()];
        nodeArray[nextState.getCurrentPlayer()] = tn;
        children.put(chosen, nodeArray);
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private void advance(AbstractGameState gs, AbstractAction act) {
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
    private void advanceToTurnOfPlayer(AbstractGameState gs, int id) {
        // For the moment we only have one opponent model - that of a random player
        while (gs.getCurrentPlayer() != id && gs.isNotTerminal()) {
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
    private SingleTreeNode nextNodeInTree() {

        if (player.params.opponentTreePolicy == SelfOnly && state.getCurrentPlayer() != player.getPlayerID())
            throw new AssertionError("An error has occurred. SelfOnly should only call uct when we are moving.");

        AbstractAction actionChosen;
        switch (player.params.treePolicy) {
            case UCB:
            case AlphaGo:
                // These just vary on the form of the exploration term in a UCB algorithm
                actionChosen = ucb();
                break;
            case EXP3:
            case RegretMatching:
                // These construct a distribution over possible actions and then sample from it
                actionChosen = sampleFromDistribution();
                break;
            default:
                throw new AssertionError("Unknown treepolicy: " + player.params.treePolicy);
        }

        // Only advance the state if this is open loop
        SingleTreeNode[] nodeArray = children.get(actionChosen);
        if (player.params.openLoop) {
            // We do not need to copy the state, as we advance this as we descend the tree.
            // In open loop we never re-use the state...the only purpose of storing it on the Node is
            // to pick it up in the next uct() call as we descend the tree
            AbstractAction chosenCopy = actionChosen.copy();
            advance(state, chosenCopy);
            int nextPlayer = state.getCurrentPlayer();
            SingleTreeNode nextNode = nodeArray[nextPlayer];
            if (nextNode == null) {
                // need to create a new node
                nodeArray[nextPlayer] = new SingleTreeNode(player, this, chosenCopy, state, rnd);
                nextNode = nodeArray[nextPlayer];
            } else {
                // pick up the existing one, and set the state
                nextNode.setState(state);
            }
            // we also need to check to see if there are any new actions on this transition
            root.fmCallsCount++;
            return nextNode;
        } else {
            // in this case we have determinism...there should just be a single child node in the array...so we get that
            Optional<SingleTreeNode> next = Arrays.stream(nodeArray).filter(Objects::nonNull).findFirst();
            if (next.isPresent()) {
                return next.get();
            } else {
                throw new AssertionError("We have no node to move to...");
            }
        }
    }

    private AbstractAction ucb() {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        // TODO: Need to distinguish between paranoid and Max^N MCTS. But that needs a vector reward to be back-propagated

        for (AbstractAction action : actionsFromState) {
            SingleTreeNode[] childArray = children.get(action);
            if (childArray == null)
                throw new AssertionError("Should not be here");

            // Find child value
            double hvVal = actionTotValue(action, decisionPlayer);
            int actionVisits = actionVisits(action);
            double childValue = hvVal / (actionVisits + player.params.epsilon);

            // default to standard UCB
            double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (actionVisits + player.params.epsilon));
            // unless we are using a variant
            if (player.params.treePolicy == AlphaGo)
                explorationTerm = player.params.K * Math.sqrt(this.nVisits / (actionVisits + 1.0));

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
        return Math.exp(meanAdvantageFromAction);
    }

    public double rmValue(AbstractAction action) {
        // TODO: This is not quite correct for game in which not all actions are available for each visit
        // TODO: (see comment in checkActions() - to be enhanced to keep track of this at some future point)
        double actionValue = actionTotValue(action, decisionPlayer);
        int actionVisits = actionVisits(action);
        // potential value is our estimate of our accumulated reward if we had always taken this action
        double potentialValue = actionValue * nVisits / actionVisits;
        double regret = potentialValue - totValue[decisionPlayer];
        return Math.max(0.0, regret);
    }

    private AbstractAction sampleFromDistribution() {
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
        Map<AbstractAction, Double> actionToValueMap = actionsFromState.stream().collect(toMap(Function.identity(), valueFn));

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
    private double[] rollOut(List<Pair<Integer, AbstractAction>> rolloutActions) {
        int rolloutDepth = 0; // counting from end of tree

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state;
        if (player.params.rolloutLength > 0) {
            if (!player.params.openLoop) {
                // the thinking here is that in openLoop we copy the state right at the root, and then use the forward
                // model at each action. Hence the current state on the node is the one we have been using up to now.
                /// Hence we do not need to copy it.
                rolloutState = state.copy();
                root.copyCount++;
            }

            AbstractPlayer rolloutStrategy = player.rolloutStrategy;
            while (!finishRollout(rolloutState, rolloutDepth)) {
                List<AbstractAction> availableActions = player.getForwardModel().computeAvailableActions(rolloutState);
                if (availableActions.isEmpty())
                    break;
                AbstractAction next = rolloutStrategy.getAction(rolloutState, availableActions);
                rolloutActions.add(new Pair<>(rolloutState.getCurrentPlayer(), next));
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        double[] retValue = new double[state.getNPlayers()];
        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = player.heuristic.evaluateState(rolloutState, i);
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

        // End of game
        return !rollerState.isNotTerminal();
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    private void backUp(double[] result) {
        SingleTreeNode n = this;
        while (n != null) {
            n.nVisits++;
            switch (player.params.opponentTreePolicy) {
                case SelfOnly:
                    for (int j = 0; j < result.length; j++)
                        n.totValue[j] += result[root.decisionPlayer];
                    break;
                case Paranoid:
                    for (int j = 0; j < result.length; j++) {
                        if (j == root.decisionPlayer)
                            n.totValue[j] += result[root.decisionPlayer];
                        else
                            n.totValue[j] -= result[root.decisionPlayer];
                    }
                    break;
                case MaxN:
                    for (int j = 0; j < result.length; j++)
                        n.totValue[j] += result[j];
                    break;
            }
            n = n.parent;
        }
    }


    private void MASTBackup(List<Pair<Integer, AbstractAction>> rolloutActions, double[] delta) {
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
