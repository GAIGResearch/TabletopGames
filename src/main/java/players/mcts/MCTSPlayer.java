package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.interfaces.IStatisticLogger;
import utilities.SummaryLogger;

import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

public class MCTSPlayer extends AbstractPlayer {

    // Random object for this player
    Random rnd;
    // Parameters for this player
    MCTSParams params;
    // Heuristics used for the agent
    IStateHeuristic heuristic;
    AbstractPlayer rolloutStrategy;
    AbstractPlayer opponentModel;
    private boolean debug = false;

    public MCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public MCTSPlayer(long seed) {
        this.params = new MCTSParams(seed);
        rnd = new Random(seed);
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
    public AbstractAction getAction(AbstractGameState gameState) {
        // Gather all available actions:
        List<AbstractAction> allActions = gameState.getActions();

        // Search for best action from the root
        SingleTreeNode root = new SingleTreeNode(this, allActions, rnd);
        root.setRootGameState(root, gameState);
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