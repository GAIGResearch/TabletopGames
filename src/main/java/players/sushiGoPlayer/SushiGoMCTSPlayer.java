package players.sushiGoPlayer;

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
public class SushiGoMCTSPlayer extends AbstractPlayer {

    public SushiGoMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public SushiGoMCTSPlayer(long seed) {
        super(new SushiGoMCTSParams(), "Sushi Go MCTS");
        // for clarity we create a new set of parameters here, but we could just use the default parameters
        parameters.setRandomSeed(seed);
        rnd = new Random(seed);

        // These parameters can be changed, and will impact the Basic MCTS algorithm
        SushiGoMCTSParams params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;

    }

    public SushiGoMCTSPlayer(SushiGoMCTSParams params) {
        super(params, "Sushi Go MCTS");
        rnd = new Random(params.getRandomSeed());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        SushiGoTreeNode root = new SushiGoTreeNode(this, null, gameState, rnd);

        // mctsSearch does all of the hard work
        root.mctsSearch();

        // Return best action
        return root.bestAction();
    }

    @Override
    public SushiGoMCTSParams getParameters() {
        return (SushiGoMCTSParams) parameters;
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().heuristic = heuristic;
    }


    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public SushiGoMCTSPlayer copy() {
        return new SushiGoMCTSPlayer((SushiGoMCTSParams) parameters.copy());
    }
}