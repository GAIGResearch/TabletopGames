package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import evaluation.listeners.IGameListener;
import core.interfaces.IStateHeuristic;
import evaluation.metrics.Event;
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

public class MCTSPlayer extends AbstractPlayer implements IAnyTimePlayer {

    // Heuristics used for the agent
    protected boolean debug = false;
    protected SingleTreeNode root;
    protected AbstractAction lastAction;
    List<Map<Object, Pair<Integer, Double>>> MASTStats;

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

    protected SingleTreeNode newRootNode(AbstractGameState gameState) {
        //TODO: For MultiTree MCTS we need to do this across each opponent tree in sequence
        SingleTreeNode newRoot = null;
        if (getParameters().reuseTree && root != null) {
            // we see if we can reuse the tree
            // We need to look at all actions taken since our last action
            List<Pair<Integer, AbstractAction>> history = gameState.getHistory();
            Pair<Integer, AbstractAction> lastExpected = new Pair<>(gameState.getCurrentPlayer(), lastAction);
            newRoot = root;
            for (int backwardLoop = history.size() - 1; backwardLoop >= 0; backwardLoop--) {
                if (history.get(backwardLoop).equals(lastExpected)) {
                    // We can reuse the tree from this point
                    // We now work forward through the actions
                    for (int forwardLoop = backwardLoop; forwardLoop < history.size(); forwardLoop++) {
                        AbstractAction action = history.get(forwardLoop).b;
                        int nextActionPlayer = forwardLoop < history.size() - 1 ? history.get(forwardLoop + 1).a : gameState.getCurrentPlayer();
                        // then make sure that we have a transition for this player and this action
                        if (newRoot != null && newRoot.children != null && newRoot.children.get(action) != null)
                            newRoot = newRoot.children.get(action)[nextActionPlayer];
                        else
                            newRoot = null;
                        if (newRoot == null) {
                            // we have not seen this action before, so we stop, as the game has moved
                            // beyond the last stored open loop tree
                            break;
                        }
                    }
                }
            }
        }
        // at this stage we should have moved down the tree to get to the correct node
        // based on the actions taken in the game since our last decision
        // The node we have reached should be the new root node
        if (newRoot != null) {
            if (newRoot.turnOwner != gameState.getCurrentPlayer() || newRoot.decisionPlayer != gameState.getCurrentPlayer()) {
                throw new AssertionError("Current player does not match decision player in tree");
                // if this is a problem, we can just set newRoot = null;
            }
            // We need to make the new root the root of the tree
            // We need to remove the parent link from the new root
            newRoot.instantiate(null, null, gameState);
            newRoot.rootify(root);
        }
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
        if (MASTStats != null)
            root.MASTStatistics = MASTStats.stream()
                    .map(m -> Utils.decay(m, getParameters().MASTGamma))
                    .collect(Collectors.toList());

        if (getParameters().getRolloutStrategy() instanceof IMASTUser) {
            ((IMASTUser) getParameters().getRolloutStrategy()).setStats(root.MASTStatistics);
        }
        if (getParameters().getOpponentModel() instanceof IMASTUser) {
            ((IMASTUser) getParameters().getOpponentModel()).setStats(root.MASTStatistics);
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        createRootNode(gameState);
        root.mctsSearch();

        if (getParameters().actionHeuristic instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().actionHeuristic).process(root);
        if (getParameters().getRolloutStrategy() instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().getRolloutStrategy()).process(root);
        if (getParameters().heuristic instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().heuristic).process(root);
        if (getParameters().getOpponentModel() instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().getOpponentModel()).process(root);

        if (debug)
            System.out.println(root.toString());

        MASTStats = root.MASTStatistics;

        if (root.children.size() > 2 * actions.size() && !(root instanceof MCGSNode) && !getParameters().actionSpace.equals(gameState.getCoreGameParameters().actionSpace))
            throw new AssertionError(String.format("Unexpectedly large number of children: %d with action size of %d", root.children.size(), actions.size()));
        lastAction = root.bestAction();
        return lastAction.copy();
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
        MCTSPlayer retValue = new MCTSPlayer((MCTSParams) getParameters().copy());
        if (getForwardModel() != null)
            retValue.setForwardModel(getForwardModel().copy());
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
                double actionValue = getParameters().actionHeuristic.evaluateAction(action, root.state);

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
    public String toString() {
        return super.toString();
    }
}