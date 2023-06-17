package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import evaluation.listeners.IGameListener;
import core.interfaces.IStateHeuristic;
import evaluation.metrics.Event;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static players.mcts.MCTSEnums.OpponentTreePolicy.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MultiTree;

public class MCTSPlayer extends AbstractPlayer {

    // Random object for this player
    protected Random rnd;
    // Parameters for this player
    protected MCTSParams params;
    // Heuristics used for the agent
    protected IStateHeuristic heuristic;
    protected AbstractPlayer rolloutStrategy;
    protected boolean debug = false;
    protected SingleTreeNode root;
    List<Map<Object, Pair<Integer, Double>>> MASTStats;
    private AbstractPlayer opponentModel;
    private IActionHeuristic advantageFunction;

    public MCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public MCTSPlayer(long seed) {
        this(new MCTSParams(seed), "MCTSPlayer");
    }

    public MCTSPlayer(MCTSParams params) {
        this(params, "MCTSPlayer");
    }

    public MCTSPlayer(MCTSParams params, String name) {
        this.params = params;
        rnd = new Random(this.params.getRandomSeed());
        rolloutStrategy = params.getRolloutStrategy();
        opponentModel = params.getOpponentModel();
        heuristic = params.getHeuristic();
        advantageFunction = params.advantageFunction;
        setName(name);
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        rolloutStrategy.initializePlayer(state);
        opponentModel.initializePlayer(state);
        if (advantageFunction instanceof AbstractPlayer)
            ((AbstractPlayer) advantageFunction).initializePlayer(state);
        MASTStats = null;
    }

    /**
     * This is intended mostly for debugging purposes. It allows the user to provide a Node
     * factory that specifies the node class, and can have relevant tests/hooks inserted; for
     * example to run a check after each MCTS iteration
     */
    protected Supplier<? extends SingleTreeNode> getFactory() {
        return () -> {
            if (params.opponentTreePolicy == OMA || params.opponentTreePolicy == OMA_All)
                return new OMATreeNode();
            else
                return new SingleTreeNode();
        };
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        if (params.opponentTreePolicy == MultiTree)
            root = new MultiTreeNode(this, gameState, rnd);
        else
            root = SingleTreeNode.createRootNode(this, gameState, rnd, getFactory());

        if (MASTStats != null)
            root.MASTStatistics = MASTStats.stream()
                    .map(m -> Utils.decay(m, params.MASTGamma))
                    .collect(Collectors.toList());

        if (rolloutStrategy instanceof IMASTUser) {
            ((IMASTUser) rolloutStrategy).setStats(root.MASTStatistics);
        }
        if (opponentModel instanceof IMASTUser) {
            ((IMASTUser) opponentModel).setStats(root.MASTStatistics);
        }
        root.mctsSearch();

        if (advantageFunction instanceof ITreeProcessor)
            ((ITreeProcessor) advantageFunction).process(root);
        if (rolloutStrategy instanceof ITreeProcessor)
            ((ITreeProcessor) rolloutStrategy).process(root);
        if (heuristic instanceof ITreeProcessor)
            ((ITreeProcessor) heuristic).process(root);
        if (opponentModel instanceof ITreeProcessor)
            ((ITreeProcessor) opponentModel).process(root);

        if (debug)
            System.out.println(root.toString());

        MASTStats = root.MASTStatistics;
        // Return best action
        if (root.children.size() > 2 * actions.size())
            throw new AssertionError(String.format("Unexpectedly large number of children: %d with action size of %d", root.children.size(), actions.size()) );
        return root.bestAction();
    }


    public AbstractPlayer getOpponentModel(int playerID) {
        return opponentModel;
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {
        rolloutStrategy.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        opponentModel.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        if (heuristic instanceof IGameListener)
            ((IGameListener) heuristic).onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));
        if (advantageFunction instanceof IGameListener)
            ((IGameListener) advantageFunction).onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, state));

    }

    @Override
    public MCTSPlayer copy() {
        return new MCTSPlayer((MCTSParams) params.copy());
    }

    @Override
    public void setForwardModel(AbstractForwardModel model) {
        super.setForwardModel(model);
        if (rolloutStrategy != null)
            rolloutStrategy.setForwardModel(model);
        if (opponentModel != null)
            opponentModel.setForwardModel(model);
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public Map<AbstractAction, Map<String, Object>> getDecisionStats() {
        Map<AbstractAction, Map<String, Object>> retValue = new LinkedHashMap<>();

        if (root != null && root.getVisits() > 1) {
            for (AbstractAction action : root.children.keySet()) {
                int visits = Arrays.stream(root.children.get(action)).filter(Objects::nonNull).mapToInt(SingleTreeNode::getVisits).sum();
                double visitProportion = visits / (double) root.getVisits();
                double meanValue =  Arrays.stream(root.children.get(action)).filter(Objects::nonNull).mapToDouble(n -> n.getTotValue()[root.decisionPlayer]).sum()/ visits;
                double heuristicValue = heuristic != null ? heuristic.evaluateState(root.state, root.decisionPlayer) : 0.0;
                double advantageValue = advantageFunction != null ? advantageFunction.evaluateAction(action, root.state) : 0.0;

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

}