package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.interfaces.IStatisticLogger;
import utilities.SummaryLogger;

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
        setName(name);
    }

    public MCTSPlayer(IStateHeuristic heuristic){
        this(System.currentTimeMillis());
        this.heuristic = heuristic;
    }

    public MCTSPlayer(long seed, IStateHeuristic heuristic){
        this.params = new MCTSParams(seed);
        rnd = new Random(seed);
        this.heuristic = heuristic;
    }

    public MCTSPlayer( MCTSParams params, IStateHeuristic heuristic){
        this.params = params;
        rnd = new Random(this.params.getRandomSeed());
        this.heuristic = heuristic;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        SingleTreeNode root = new SingleTreeNode(this, null, gameState, rnd);
        root.mctsSearch(getStatsLogger());

        if (debug)
            System.out.println(root.toString());

        // Return best action
        return root.bestAction();
    }

    public AbstractPlayer getOpponentModel(int playerID) {
        return opponentModel;
    }
}