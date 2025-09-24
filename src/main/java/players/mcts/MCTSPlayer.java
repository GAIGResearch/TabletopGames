package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import evaluation.listeners.IGameListener;
import core.interfaces.IStateHeuristic;
import evaluation.metrics.Event;
import llm.IHasStateHeuristic;
import players.IAnyTimePlayer;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static players.mcts.MCTSEnums.OpponentTreePolicy.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MultiTree;

public class MCTSPlayer extends AbstractPlayer implements IAnyTimePlayer, IHasStateHeuristic {

    // Heuristics used for the agent
    protected boolean debug = false;
    protected SingleTreeNode root;
    protected Pair<Integer, AbstractAction> lastAction;
    List<Map<Object, Pair<Integer, Double>>> MASTStats;
    protected Map<Object, Integer> oldGraphKeys = new HashMap<>();
    protected List<Object> recentlyRemovedKeys = new ArrayList<>();

    public MCTSPlayer() {
        this(new MCTSParams());
    }

    public MCTSPlayer(MCTSParams params) {
        this(params, "MCTSPlayer");
    }

    public MCTSPlayer(MCTSParams params, String name) {
        super(params, name);
        rnd = new Random(parameters.getRandomSeed());
    }

    @Override
    public MCTSParams getParameters() {
        return (MCTSParams) parameters;
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        if (getParameters().resetSeedEachGame) {
            rnd = new Random(parameters.getRandomSeed());
            getParameters().rolloutPolicy = null;
            getParameters().getRolloutStrategy();
            getParameters().opponentModel = null;  // thi swill force reconstruction from random seed
            getParameters().getOpponentModel();
            //       System.out.println("Resetting seed for MCTS player to " + params.getRandomSeed());
        }
        if (getParameters().actionHeuristic instanceof AbstractPlayer)
            ((AbstractPlayer) getParameters().actionHeuristic).initializePlayer(state);
        MASTStats = null;
        root = null;
        oldGraphKeys = new HashMap<>();
        getParameters().getRolloutStrategy().initializePlayer(state);
        getParameters().getOpponentModel().initializePlayer(state);
    }

    /**
     * This is intended mostly for debugging purposes. It allows the user to provide a Node
     * factory that specifies the node class, and can have relevant tests/hooks inserted; for
     * example to run a check after each MCTS iteration
     */
    protected Supplier<? extends SingleTreeNode> getFactory() {
        return () -> {
            if (getParameters().opponentTreePolicy == OMA || getParameters().opponentTreePolicy == OMA_All)
                return new OMATreeNode();
            else if (getParameters().opponentTreePolicy == MCGS || getParameters().opponentTreePolicy == MCGSSelfOnly)
                return new MCGSNode();
            else
                return new SingleTreeNode();
        };
    }

    @Override
    public void registerUpdatedObservation(AbstractGameState gameState) {
        super.registerUpdatedObservation(gameState);
        if (!getParameters().reuseTree) {
            root = null;
        }
    }

    protected MultiTreeNode newMultiTreeRootNode(AbstractGameState state) {
        // We need to update each of the individual player root nodes independently
        MultiTreeNode mtRoot = (MultiTreeNode) this.root;
        // We retain this, but update the root nodes
        // We could have run through the history once...but more robust to do this once per player
        // and reuse the code for SelfOnly
        SingleTreeNode[] newRoots = new SingleTreeNode[state.getNPlayers()];

        for (int p = 0; p < state.getNPlayers(); p++) {
            SingleTreeNode oldRoot = mtRoot.roots[p];
            if (oldRoot == null)
                continue;
            if (debug)
                System.out.println("\tBacktracking for player " + mtRoot.roots[p].decisionPlayer);
            newRoots[p] = backtrack(mtRoot.roots[p], state);
            if (newRoots[p] != null) {
                // here we do not do a full rootification as that would set the turnOwner and currentPlayer
                // to the decision player, which we want to avoid
                newRoots[p].rootify(oldRoot, null);
                newRoots[p].resetDepth(newRoots[p]);
                newRoots[p].state = state.copy();
            }
        }
        mtRoot.roots = newRoots;
        mtRoot.state = state.copy();
        return mtRoot;
    }

    protected SingleTreeNode newRootNode(AbstractGameState gameState) {
        MCTSParams params = getParameters();
        recentlyRemovedKeys.clear();
        if (params.reuseTree && (params.opponentTreePolicy == MCGS || params.opponentTreePolicy == MCGSSelfOnly)) {
            // In this case we remove any nodes from the graph that were not present before the last action was taken
            MCGSNode mcgsRoot = (MCGSNode) root;
            for (Object key : oldGraphKeys.keySet()) {
                int oldVisits = oldGraphKeys.get(key);
                MCGSNode node = mcgsRoot.getTranspositionMap().get(key);
                if (node != null) { // it might have been removed for having a negative depth
                    int newVisits = node.nVisits;
                    if (newVisits == oldVisits) {
                        // no change, so remove
                        mcgsRoot.getTranspositionMap().remove(key);
                        recentlyRemovedKeys.add(key);
                    } else if (newVisits < oldVisits) {
                        throw new AssertionError("Unexpectedly fewer visits to a state than before");
                    }
                }
            }
            // then reset the old keys
            if (mcgsRoot == null) {
                oldGraphKeys = new HashMap<>();
                return null;
            }

            oldGraphKeys = mcgsRoot.getTranspositionMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().nVisits));
            // we create the root node as we would have done normally; and then override the transposition map
            MCGSNode retValue = ((MCGSNode) root).getTranspositionMap().get(params.MCGSStateKey.getKey(gameState));
            if (retValue == null) {
                // have left graph; start from scratch
                oldGraphKeys = new HashMap<>();
                return null;
            }
        //    int oldDepth = retValue.depth;
            retValue.setTranspositionMap(mcgsRoot.getTranspositionMap());
            retValue.rootify(root, gameState);
      //      retValue.depth = oldDepth;
            return retValue;
        }

        // Now for standard open loop processing
        SingleTreeNode newRoot = null;
        if (params.reuseTree && root != null) {
            // we see if we can reuse the tree
            // We need to look at all actions taken since our last action
            if (debug)
                System.out.println("Backtracking for " + lastAction + " by " + gameState.getCurrentPlayer());

            if (params.opponentTreePolicy == MultiTree)
                return newMultiTreeRootNode(gameState);

            newRoot = backtrack(root, gameState);

            if (root == newRoot)
                throw new AssertionError("Root node should not be the same as the new root node");
            if (debug && newRoot == null)
                System.out.println("No matching node found");
        }
        // at this stage we should have moved down the tree to get to the correct node
        // based on the actions taken in the game since our last decision
        // The node we have reached should be the new root node
        if (newRoot != null) {
            if (debug)
                System.out.println("Matching node found");
            if (newRoot.turnOwner != gameState.getCurrentPlayer() || newRoot.decisionPlayer != gameState.getCurrentPlayer()) {
                throw new AssertionError("Current player does not match decision player in tree");
                // if this is a problem, we can just set newRoot = null;
            }
            // We need to make the new root the root of the tree
            // We need to remove the parent link from the new root
            //   newRoot.instantiate(null, null, gameState);
            newRoot.rootify(root, gameState);
        }
        return newRoot;
    }

    protected SingleTreeNode backtrack(SingleTreeNode startingRoot, AbstractGameState gameState) {
        List<Pair<Integer, AbstractAction>> history = gameState.getHistory();
        Pair<Integer, AbstractAction> lastExpected = lastAction;
        MCTSParams params = getParameters();
        boolean selfOnly = params.opponentTreePolicy == SelfOnly || params.opponentTreePolicy == MultiTree;
        SingleTreeNode newRoot = startingRoot;
        int rootPlayer = startingRoot.decisionPlayer;
        boolean foundPointInHistory = false;
        for (int backwardLoop = history.size() - 1; backwardLoop >= 0; backwardLoop--) {
            if (history.get(backwardLoop).equals(lastExpected)) {
                // We can reuse the tree from this point
                // We now work forward through the actions
                foundPointInHistory = true;
                if (debug)
                    System.out.println("Matching action found at " + backwardLoop + " of " + history.size() + " - tracking forward");
                for (int forwardLoop = backwardLoop; forwardLoop < history.size(); forwardLoop++) {
                    if (selfOnly && history.get(forwardLoop).a != rootPlayer)
                        continue; // we only care about our actions
                    AbstractAction action = history.get(forwardLoop).b;
                    int nextActionPlayer = forwardLoop < history.size() - 1 ? history.get(forwardLoop + 1).a : gameState.getCurrentPlayer();
                    // then make sure that we have a transition for this player and this action
                    // If we are SelfOnly, then we only store our actions in the tree
                    nextActionPlayer = selfOnly ? rootPlayer : nextActionPlayer;
                    if (debug)
                        System.out.println("\tAction: " + action.toString() + "\t Next Player: " + nextActionPlayer);
                    if (newRoot.children != null && newRoot.children.get(action) != null)
                        newRoot = newRoot.children.get(action)[nextActionPlayer];
                    else
                        newRoot = null;
                    if (newRoot == null)
                        break;
                }
                break;
            }
        }
        if (!foundPointInHistory) {
            throw new AssertionError("Unable to find matching action in history : " + lastExpected);
        }
        if (debug)
            System.out.println("\tBacktracking complete : " + (newRoot == null ? "no matching node found" : "node found"));
        return newRoot;
    }

    protected void createRootNode(AbstractGameState gameState) {
        SingleTreeNode newRoot = newRootNode(gameState);
        if (newRoot == null) {
            if (getParameters().opponentTreePolicy == MultiTree)
                root = new MultiTreeNode(this, gameState, rnd);
            else
                root = SingleTreeNode.createRootNode(this, gameState, rnd, getFactory());
        } else {
            root = newRoot;
        }
        if (MASTStats != null && getParameters().MASTGamma > 0.0)
            root.MASTStatistics = MASTStats.stream()
                    .map(m -> Utils.decay(m, getParameters().MASTGamma))
                    .collect(Collectors.toList());

        if (getParameters().getRolloutStrategy() instanceof IMASTUser) {
            ((IMASTUser) getParameters().getRolloutStrategy()).setMASTStats(root.MASTStatistics);
        }
        if (getParameters().getOpponentModel() instanceof IMASTUser) {
            ((IMASTUser) getParameters().getOpponentModel()).setMASTStats(root.MASTStatistics);
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        long currentTimeNano = System.nanoTime();
        createRootNode(gameState);
        long timeTaken = System.nanoTime() - currentTimeNano;

        root.mctsSearch(timeTaken / 1000000);

        if (getParameters().actionHeuristic instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().actionHeuristic).process(root);
        if (getParameters().getRolloutStrategy() instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().getRolloutStrategy()).process(root);
        if (getParameters().heuristic instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().heuristic).process(root);
        if (getParameters().getOpponentModel() instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().getOpponentModel()).process(root);

        if (debug) {
            if (getParameters().opponentTreePolicy == MultiTree)
                System.out.println(((MultiTreeNode) root).getRoot(gameState.getCurrentPlayer()));
            else
                System.out.println(root);
        }
        MASTStats = root.MASTStatistics;

        if (root.children.size() > 3 * actions.size() && !(root instanceof MCGSNode) && !getParameters().reuseTree && !getParameters().actionSpace.equals(gameState.getCoreGameParameters().actionSpace))
            throw new AssertionError(String.format("Unexpectedly large number of children: %d with action size of %d", root.children.size(), actions.size()));
        lastAction = new Pair<>(gameState.getCurrentPlayer(), root.bestAction());
        return lastAction.b.copy();
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {
        getParameters().getRolloutStrategy().onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        getParameters().getOpponentModel().onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        if (getParameters().heuristic instanceof IGameListener)
            ((IGameListener) getParameters().heuristic).onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        if (getParameters().actionHeuristic instanceof IGameListener)
            ((IGameListener) getParameters().actionHeuristic).onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));

    }

    @Override
    public MCTSPlayer copy() {
        MCTSPlayer retValue = new MCTSPlayer((MCTSParams) getParameters().copy(), toString());
        if (getForwardModel() != null)
            retValue.setForwardModel(getForwardModel());
        return retValue;
    }

    @Override
    public void setForwardModel(AbstractForwardModel model) {
        super.setForwardModel(model);
        if (getParameters().getRolloutStrategy() != null)
            getParameters().getRolloutStrategy().setForwardModel(model);
        if (getParameters().getOpponentModel() != null)
            getParameters().getOpponentModel().setForwardModel(model);
    }

    @Override
    public Map<AbstractAction, Map<String, Object>> getDecisionStats() {
        Map<AbstractAction, Map<String, Object>> retValue = new LinkedHashMap<>();

        if (root != null && root.getVisits() > 1) {
            for (AbstractAction action : root.actionValues.keySet()) {
                ActionStats stats = root.actionValues.get(action);
                int visits = stats == null ? 0 : stats.nVisits;
                double visitProportion = visits / (double) root.getVisits();
                double meanValue = stats == null || visits == 0 ? 0.0 : stats.totValue[root.decisionPlayer] / visits;
                double heuristicValue = getParameters().heuristic.evaluateState(root.state, root.decisionPlayer);
                double actionValue = getParameters().actionHeuristic.evaluateAction(action, root.state, root.actionsFromOpenLoopState);

                Map<String, Object> actionValues = new HashMap<>();
                actionValues.put("visits", visits);
                actionValues.put("visitProportion", visitProportion);
                actionValues.put("meanValue", meanValue);
                actionValues.put("heuristic", heuristicValue);
                actionValues.put("actionValue", actionValue);
                retValue.put(action, actionValues);
            }
        }

        return retValue;
    }

    @Override
    public void setBudget(int budget) {
        parameters.budget = budget;
        parameters.setParameterValue("budget", budget);
    }

    @Override
    public int getBudget() {
        return parameters.budget;
    }

    @Override
    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().setParameterValue("heuristic", heuristic);
    }
    @Override
    public IStateHeuristic getStateHeuristic() {
        return getParameters().heuristic;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}