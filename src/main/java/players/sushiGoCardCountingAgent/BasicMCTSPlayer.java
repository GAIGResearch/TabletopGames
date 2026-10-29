package players.sushiGoCardCountingAgent;

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
public class BasicMCTSPlayer extends AbstractPlayer {

    public BasicMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public BasicMCTSPlayer(long seed) {
        super(new SushiGoPlayerMCTSParams(), "Basic MCTS");
        // for clarity we create a new set of parameters here, but we could just use the default parameters
        parameters.setRandomSeed(seed);
        rnd = new Random(seed);

        // These parameters can be changed, and will impact the Basic MCTS algorithm
        SushiGoPlayerMCTSParams params = getParameters();
        params.K = Math.sqrt(2);
        params.rolloutLength = 10;
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;

    }

    public BasicMCTSPlayer(SushiGoPlayerMCTSParams params) {
        super(params, "Basic MCTS");
        rnd = new Random(params.getRandomSeed());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        players.sushiGoCardCountingAgent.BasicTreeNode root = new players.sushiGoCardCountingAgent.BasicTreeNode(this, null, gameState, rnd);

        // mctsSearch does all of the hard work
        root.mctsSearch();

        // Return best action
        return root.bestAction();
    }

    @Override
    public SushiGoPlayerMCTSParams getParameters() {
        return (SushiGoPlayerMCTSParams) parameters;
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().heuristic = heuristic;
    }


    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public BasicMCTSPlayer copy() {
        BasicMCTSPlayer retValue = new BasicMCTSPlayer((SushiGoPlayerMCTSParams) parameters.copy());
        retValue.setName(toString());
        return retValue;
    }
}