package players.basicHueristicMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Random;


/**
 * This is a simple version of MCTS that may be useful for newcomers to TAG and MCTS-like algorithms
 * It strips out some of the additional configuration of MCTSPlayer. It uses BasicTreeNode in place of
 * SingleTreeNode.
 */
public class BasicHeuristicMCTSPlayer extends AbstractPlayer {

    public BasicHeurTreeNode root;

    public BasicHeuristicMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public BasicHeuristicMCTSPlayer(long seed) {
        super(new BasicHeurMCTSParams(), "Basic MCTS");
        // for clarity we create a new set of parameters here, but we could just use the default parameters
        parameters.setRandomSeed(seed);
        rnd = new Random(seed);

        // These parameters can be changed, and will impact the Basic MCTS algorithm
        BasicHeurMCTSParams params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;

    }

    public BasicHeuristicMCTSPlayer(BasicHeurMCTSParams params) {
        this(params, "Basic MCTS");
    }

    public BasicHeuristicMCTSPlayer(BasicHeurMCTSParams params, String name) {
        super(params, name);
        rnd = new Random(params.getRandomSeed());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {

        // Search for best action from the root
        root = new BasicHeurTreeNode(this, null, gameState, rnd);

        // mctsSearch does all of the hard work
        root.mctsSearch();

        // Return best action
        return root.bestAction();
    }

    @Override
    public BasicHeurMCTSParams getParameters() {
        return (BasicHeurMCTSParams) parameters;
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().heuristic = heuristic;
    }


    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public BasicHeuristicMCTSPlayer copy() {
        return new BasicHeuristicMCTSPlayer((BasicHeurMCTSParams) parameters.copy());
    }
}