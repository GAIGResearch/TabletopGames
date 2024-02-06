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
        if (getParameters().advantageFunction instanceof AbstractPlayer)
            ((AbstractPlayer) getParameters().advantageFunction).initializePlayer(state);
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
        // We did not take a decision, so blank out the previous set of data
        root = null;
    }


    private void createRootNode(AbstractGameState gameState) {
        if (getParameters().opponentTreePolicy == MultiTree)
            root = new MultiTreeNode(this, gameState, rnd);
        else
            root = SingleTreeNode.createRootNode(this, gameState, rnd, getFactory());

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

        if (getParameters().advantageFunction instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().advantageFunction).process(root);
        if (getParameters().getRolloutStrategy() instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().getRolloutStrategy()).process(root);
        if (getParameters().heuristic instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().heuristic).process(root);
        if (getParameters().getOpponentModel() instanceof ITreeProcessor)
            ((ITreeProcessor) getParameters().getOpponentModel()).process(root);

        if (debug)
            System.out.println(root.toString());

        MASTStats = root.MASTStatistics;

        if (!(root instanceof MCGSNode) && root.children.size() > 2 * actions.size() && !getParameters().actionSpace.equals(gameState.getCoreGameParameters().actionSpace))
            throw new AssertionError(String.format("Unexpectedly large number of children: %d with action size of %d", root.children.size(), actions.size()));
        return root.bestAction();
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {
        getParameters().getRolloutStrategy().onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        getParameters().getOpponentModel().onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        if (getParameters().heuristic instanceof IGameListener)
            ((IGameListener) getParameters().heuristic).onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        if (getParameters().advantageFunction instanceof IGameListener)
            ((IGameListener) getParameters().advantageFunction).onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));

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
                double heuristicValue = getParameters().heuristic != null ? getParameters().heuristic.evaluateState(root.state, root.decisionPlayer) : 0.0;
                double advantageValue = getParameters().advantageFunction != null ? getParameters().advantageFunction.evaluateAction(action, root.state) : 0.0;

                Map<String, Object> actionValues = new HashMap<>();
                actionValues.put("visits", visits);
                actionValues.put("visitProportion", visitProportion);
                actionValues.put("meanValue", meanValue);
                actionValues.put("heuristic", heuristicValue);
                actionValues.put("advantage", advantageValue);
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