package players.mcts;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;
import utilities.Utils;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.*;
import static players.mcts.MCTSEnums.TreePolicy.*;
import static utilities.Utils.*;

class SingleTreeNode {
    // Root node of tree
    SingleTreeNode root;
    // Parent of this node
    SingleTreeNode parent;
    // Children of this node
    Map<AbstractAction, SingleTreeNode> children = new HashMap<>();
    // Depth of this node
    final int depth;

    // Total value of this node
    private double totValue;
    // Total regret of this node (only used for regret matching)
    private double totRegret;
    // Number of visits
    private int nVisits;
    // Number of FM calls and State copies up until this node
    private int fmCallsCount;
    private int copyCount;
    // Parameters guiding the search
    private MCTSPlayer player;
    private Random rnd;

    // State in this node (closed loop)
    private AbstractGameState state;

    // Called from MCTSPlayer
    SingleTreeNode(MCTSPlayer player, List<AbstractAction> actionsAvailable, Random rnd) {
        this(player, actionsAvailable, null, null, null, rnd);
    }

    // Called in tree expansion
    private SingleTreeNode(MCTSPlayer player, List<AbstractAction> actionsAvailable, SingleTreeNode parent,
                           SingleTreeNode root, AbstractGameState state, Random rnd) {
        this.player = player;
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = root;
        actionsAvailable.forEach(a -> children.put(a, null));
        totValue = 0.0;
        this.state = state;
        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
        this.rnd = rnd;
    }

    /**
     * Initializes the root node
     *
     * @param root - root node
     * @param gs   - root game state
     */
    void setRootGameState(SingleTreeNode root, AbstractGameState gs) {
        this.state = gs;
        this.root = root;
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    void mctsSearch(IStatisticLogger statsLogger) {

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = player.params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (player.params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(player.params.timeBudget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;
        // We keep a copy of this, as if we are using an open loop approach, then we need to advance a state
        // through the tree on each iteration, while still keeping an unchanged master copy (rootState)
        AbstractGameState rootState = state.copy();
        copyCount++;
        while (!stop) {
            // IS-MCTS...we need to redeterminise on each step otherwise we are actually doing PIMC with a single
            // determinisation! with a partially observable game, this could be rather dangerous
            if (player.params.openLoop) { // this assumes that copy(id) randomises the invisible components
                state = player.params.redeterminise ? rootState.copy(player.getPlayerID()) : rootState.copy();
                copyCount++;
            } else
                state = rootState;
            // TODO: Can we determinise in Closed Loop? Closed Loop currently means we do not advance the state though
            // the tree - so shuffling the cards at the root makes no difference.
            //

            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            SingleTreeNode selected = treePolicy();
            // Monte carlo rollout: return value of MC rollout from the newly added node
            double delta = selected.rollOut();
            // Back up the value of the rollout through the tree
            selected.backUp(delta);
            // Finished iteration
            numIters++;

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
                stop = numIters >= player.params.iterationsBudget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = (copyCount + fmCallsCount) > player.params.fmCallsBudget;
            }
        }
        Map<String, Object> stats = new HashMap<>();
        TreeStatistics treeStats = new TreeStatistics(root);
        stats.put("round", state.getTurnOrder().getRoundCounter());
        stats.put("turn", state.getTurnOrder().getTurnCounter());
        stats.put("turnOwner", state.getTurnOrder().getTurnOwner());
        double[] visitProportions = children.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(node -> (double) node.nVisits / this.nVisits).toArray();
        stats.put("visitEntropy", entropyOf(visitProportions));
        stats.put("iterations", numIters);
        stats.put("fmCalls", fmCallsCount);
        stats.put("copyCalls", copyCount);
        stats.put("time", elapsedTimer.elapsedMillis());
        stats.put("totalNodes", treeStats.totalNodes);
        stats.put("leafNodes", treeStats.totalLeaves);
        stats.put("maxDepth", treeStats.depthReached);
        stats.put("nActions", children.size());
        OptionalInt maxVisits = children.values().stream().filter(Objects::nonNull).mapToInt(n -> n.nVisits).max();
        stats.put("maxVisitProportion", (maxVisits.isPresent() ? maxVisits.getAsInt() : 0) / (double) numIters);
        statsLogger.record(stats);
    }

    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    private SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < player.params.maxTreeDepth && state.getActions().size() > 0) {
            if (!cur.unexpandedActions().isEmpty()) {
                // We have an unexpanded action
                cur = cur.expand();
            } else {
                // Move to next child given by UCT function
                cur = cur.nextNodeInTree();
            }
        }

        return cur;
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    private List<AbstractAction> unexpandedActions() {
        return state.getActions().stream().filter(a -> children.get(a) == null).collect(toList());
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    private SingleTreeNode expand() {
        // Find random child not already created
        Random r = new Random(player.params.getRandomSeed());
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(r.nextInt(notChosen.size()));

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        AbstractGameState nextState = state.copy();
        root.copyCount++;
        List<AbstractAction> nextActions = advance(nextState, chosen.copy());

        // then instantiate a new node
        SingleTreeNode tn = new SingleTreeNode(player, nextActions, this, depth == 0 ? this : root, nextState, rnd);
        children.put(chosen, tn);
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     * @return - list of actions available in the next state
     */
    private List<AbstractAction> advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        player.getForwardModel().computeAvailableActions(gs);
        if (gs.getActions().isEmpty()) {
            throw new AssertionError("Should always have at least one action possible...");
        }
        root.fmCallsCount++;
        if (player.params.opponentTreePolicy == SelfOnly && gs.getCurrentPlayer() != player.getPlayerID())
            advanceToTurnOfPlayer(gs, player.getPlayerID());
        return gs.getActions();
    }

    /**
     * Advance the game state to the next point at which it is the turn of the specified player.
     * This is used when we are only tracking our ourselves in the tree.
     *
     * @param id
     */
    private void advanceToTurnOfPlayer(AbstractGameState gs, int id) {
        // For the moment we only have one opponent model - that of a random player
        List<AbstractAction> actionsTaken = new ArrayList<>();
        while (gs.getCurrentPlayer() != id && gs.isNotTerminal()) {
            AbstractPlayer oppModel = player.getOpponentModel(gs.getCurrentPlayer());
            AbstractAction action = oppModel.getAction(gs);
            actionsTaken.add(action);
            player.getForwardModel().next(gs, action);
            player.getForwardModel().computeAvailableActions(gs);
            if (gs.getActions().isEmpty())
                throw new AssertionError("Should always have at least one action possible...");
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

        switch (player.params.treePolicy) {
            case UCB:
            case AlphaGo:
                // These just vary on the form of the exploration term in a UCB algorithm
                return children.get(ucb());
            case EXP3:
            case RegretMatching:
                // These construct a distribution over possible actions and then sample from it
                AbstractAction chosen = sampleFromDistribution();
                return children.get(chosen);
        }
        throw new AssertionError("This code should be unreachable - possibly you've missed something it the switch statement");
    }

    private AbstractAction ucb() {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        SingleTreeNode selected = null;
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        // TODO: Need to distinguish between paranoid and Max^N MCTS. But that needs a vector reward to be back-propagated

        for (AbstractAction action : state.getActions()) {
            SingleTreeNode child = children.get(action);
            if (child == null)
                throw new AssertionError("Should not be here");

            // Find child value
            double hvVal = child.totValue;
            double childValue = hvVal / (child.nVisits + player.params.epsilon);

            // default to standard UCB
            double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + player.params.epsilon));
            // unless we are using a variant
            if (player.params.treePolicy == AlphaGo)
                explorationTerm = player.params.K * Math.sqrt(this.nVisits / (child.nVisits + 1.0));

            // Find 'UCB' value
            boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
            double uctValue = 0;
            switch (player.params.opponentTreePolicy) {
                case Paranoid:
                    uctValue = iAmMoving ? childValue + explorationTerm : -childValue + explorationTerm;
                    break;
                case Paranoid2:
                    // This is the old policy - I'm not sure I agree that the exploration term should be treated
                    // in the same way as the exploitation term (childValue). Hence my two versions of Paranoid. James.
                    uctValue = iAmMoving ? childValue + explorationTerm : -childValue - explorationTerm;
                    break;
                case SelfOnly:
                    uctValue = childValue + explorationTerm;
            }


            // Apply small noise to break ties randomly
            uctValue = noise(uctValue, player.params.epsilon, player.rnd.nextDouble());

            // Assign value
            if (uctValue > bestValue) {
                bestAction = action;
                selected = child;
                bestValue = uctValue;
            }
        }

        if (bestAction == null)
            throw new AssertionError("We have a null value in UCT : shouldn't really happen!");

        // Only advance the state if this is open loop
        if (player.params.openLoop) {
            // we do not need to copy the state, as we advance this as we descend the tree
            // in open loop we never re-use the state...the only purpose of storing it on the Node is
            // to pick it up in the next uct() call as we descend the tree
            List<AbstractAction> nextActions = advance(state, bestAction.copy());
            selected.state = state;
            // we also need to check to see if there are any new actions on this transition
            selected.checkActions(nextActions);
            root.fmCallsCount++;
        } else {
            // If closed loop, then I don't think we should increment the FM count here!
            // root.fmCallsCount++;
        }
        return bestAction;
    }


    public double exp3Value(AbstractAction action) {
        SingleTreeNode child = children.get(action);
//        int nActions = state.getActions().size();
//        double gamma = player.params.exploreEpsilon;
        double meanAdvantageFromAction = (child.totValue / child.nVisits) - (totValue / nVisits);
        return Math.exp(meanAdvantageFromAction);
    }

    public double rmValue(AbstractAction action) {
        // TODO: This is not quite correct for game in which not all actions are available for each visit
        // TODO: (see comment in checkActions() - to be enhanced to keep track of this at some future point
        SingleTreeNode child = children.get(action);
        // potential value is our estimate of our accumulated reward if we had always taken this action
        double potentialValue = child.totValue * nVisits / child.nVisits;
        double regret = potentialValue - totValue;
        return Math.max(0.0, regret);
    }

    private AbstractAction sampleFromDistribution() {
        List<AbstractAction> availableActions = state.getActions();

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
        //      probabilityOfSelection = Utils.normaliseMap(actionToValueMap);

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
     * When in open loop, it is entirely possible that on a transition to a new state we have actions that were
     * not previously possible. (Especially in stochastic games, where different iterations may have different
     * random draws; but this can also happen in deterministic games if we model the opponent moves in their own trees
     * for example.)
     *
     * @param actions The new set of actions that are valid on a new transition to this node
     */
    private void checkActions(List<AbstractAction> actions) {
        // we run through the actions, and add any new ones not currently in the list
        for (AbstractAction action : actions) {
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
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    private double rollOut() {
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
                // rolloutStrategy.setPlayerID(rolloutState.getCurrentPlayer());
                // TODO: While the only possible rolloutStrategy is Random, this is fine
                // TODO: But there is an open issue here around the need to set the playerId for more sophisticated strategies
                AbstractAction next = rolloutStrategy.getAction(rolloutState);
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        if (player.heuristic != null) {
            return player.heuristic.evaluateState(rolloutState, player.getPlayerID());
        } else {
            return rolloutState.getScore(player.getPlayerID());
        }
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
    private void backUp(double result) {
        SingleTreeNode n = this;
        while (n != null) {
            n.nVisits++;
            n.totValue += result;
            n = n.parent;
        }
    }

    /**
     * Calculates the best action from the root according to the selection policy
     *
     * @return - the best AbstractAction
     */
    AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        MCTSEnums.SelectionPolicy policy = player.params.selectionPolicy;
        // check to see if all nodes have the same number of visits
        // if they do, then we use average score instead
        if (player.params.selectionPolicy == MCTSEnums.SelectionPolicy.ROBUST &&
                children.values().stream().filter(Objects::nonNull).map(n -> n.nVisits).collect(toSet()).size() == 1) {
            policy = MCTSEnums.SelectionPolicy.SIMPLE;
        }


        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                SingleTreeNode node = children.get(action);
                double childValue = node.nVisits;
                if (policy == MCTSEnums.SelectionPolicy.SIMPLE)
                    childValue = node.totValue / (node.nVisits + player.params.epsilon);

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

    @Override
    public String toString() {
        // we return some interesting data on this node
        // child actions
        // visits and values for each
        StringBuilder retValue = new StringBuilder();
        retValue.append(String.format("%s, %d total visits, value %.2f, with %d children, depth %d, FMCalls %d: \n",
                player, nVisits, totValue / nVisits, children.size(), depth, fmCallsCount));
        // sort all actions by visit count
        List<AbstractAction> sortedActions = children.entrySet().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(entry -> -entry.getValue().nVisits))
                .map(Map.Entry::getKey).collect(toList());
        for (AbstractAction action : sortedActions) {
            String actionName = action.toString();
            SingleTreeNode node = children.get(action);
            if (actionName.length() > 30)
                actionName = actionName.substring(0, 30);
            retValue.append(String.format("\t%-30s  visits: %d\tvalue %.2f\n", actionName, node.nVisits, node.totValue / node.nVisits));
        }
        retValue.append(new TreeStatistics(root).toString());
        return retValue.toString();
    }
}
