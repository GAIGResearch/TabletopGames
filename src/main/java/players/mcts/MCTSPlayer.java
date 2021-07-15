package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import games.dicemonastery.DiceMonasteryStateAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MCTSPlayer extends AbstractPlayer {

    // Random object for this player
    protected Random rnd;
    // Parameters for this player
    protected MCTSParams params;
    // Heuristics used for the agent
    IStateHeuristic heuristic;
    AbstractPlayer rolloutStrategy;
    AbstractPlayer opponentModel;
    protected boolean debug = false;
    protected SingleTreeNode root;

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
        setName(name);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        root = new SingleTreeNode(this, null, null, gameState, rnd);
        if (rolloutStrategy instanceof MASTPlayer) {
            ((MASTPlayer) rolloutStrategy).setRoot(root);
            ((MASTPlayer) rolloutStrategy).temperature = params.MASTBoltzmann;
        }
        root.mctsSearch(getStatsLogger());
        if (params.gatherExpertIterationData) {
            ExpertIterationDataGatherer eidg = new ExpertIterationDataGatherer(params.expertIterationFileStem, Arrays.asList(DiceMonasteryStateAttributes.values()));
            eidg.recordData(root, getForwardModel());
        }
        if (debug)
            System.out.println(root.toString());

        // Return best action
        return root.bestAction();
    }

    public AbstractPlayer getOpponentModel(int playerID) {
        return opponentModel;
    }

}