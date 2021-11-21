package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import core.interfaces.IStateHeuristic;
import games.dicemonastery.DiceMonasteryStateAttributes;
import utilities.Pair;
import utilities.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleBiFunction;
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
    List<Map<AbstractAction, Pair<Integer, Double>>> MASTStats;
    private AbstractPlayer opponentModel;
    private ToDoubleBiFunction<AbstractAction, AbstractGameState> advantageFunction;

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
        advantageFunction = params.getAdvantageFunction();
        setName(name);
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        rolloutStrategy.initializePlayer(state);
        opponentModel.initializePlayer(state);
        if (advantageFunction instanceof AbstractPlayer)
            ((AbstractPlayer) advantageFunction).initializePlayer(state);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        if (params.opponentTreePolicy == MultiTree || params.opponentTreePolicy == MultiTreeParanoid)
            root = new MultiTreeNode(this, gameState, rnd);
        else
            root = SingleTreeNode.createRootNode(this, gameState, rnd);

        if (MASTStats != null)
            root.MASTStatistics = MASTStats.stream()
                    .map(m -> Utils.decay(m, params.MASTGamma))
                    .collect(Collectors.toList());

        if (rolloutStrategy instanceof MASTPlayer) {
            ((MASTPlayer) rolloutStrategy).setRoot(root);
            ((MASTPlayer) rolloutStrategy).temperature = params.MASTBoltzmann;
        }
        root.mctsSearch(getStatsLogger());
        if (params.gatherExpertIterationData) {
            ExpertIterationDataGatherer eidg = new ExpertIterationDataGatherer(params.expertIterationFileStem, Arrays.asList(DiceMonasteryStateAttributes.values()));
            eidg.recordData(root, getForwardModel());
            eidg.close();
        }
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
        return root.bestAction();
    }


    public AbstractPlayer getOpponentModel(int playerID) {
        return opponentModel;
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {
        if (rolloutStrategy instanceof IGameListener)
            ((IGameListener) rolloutStrategy).onEvent(CoreConstants.GameEvents.GAME_OVER, state, null);
        if (opponentModel instanceof IGameListener)
            ((IGameListener) opponentModel).onEvent(CoreConstants.GameEvents.GAME_OVER, state, null);
        if (heuristic instanceof IGameListener)
            ((IGameListener) heuristic).onEvent(CoreConstants.GameEvents.GAME_OVER, state, null);
        if (advantageFunction instanceof IGameListener)
            ((IGameListener) advantageFunction).onEvent(CoreConstants.GameEvents.GAME_OVER, state, null);

    }

}