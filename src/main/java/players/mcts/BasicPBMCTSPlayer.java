package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Random;

import static players.mcts.MCTSEnums.OpponentTreePolicy.Paranoid;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.TreePolicy.UCB;
import static players.mcts.MCTSEnums.Strategies.RANDOM;

/**
 * This is a simple version of MCTS that may be useful for newcomers to TAG and MCTS-like algorithms
 * It strips out some of the additional configuration of MCTSPlayer. It uses BasicTreeNode in place of
 * SingleTreeNode.
 */
public class BasicPBMCTSPlayer extends AbstractPlayer {

    Random rnd;
    MCTSParams params;

    public BasicPBMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public BasicPBMCTSPlayer(long seed) {
        this.params = new MCTSParams(seed);
        rnd = new Random(seed);
        setName("BasicPBMCTS");

        // These parameters can be changed, and will impact the Basic MCTS algorithm
        this.params.K = Math.sqrt(2);
        this.params.rolloutLength = 10;
        this.params.maxTreeDepth = 5;
        this.params.epsilon = 1e-6;

        // These parameters are ignored by BasicMCTS - if you want to play with these, you'll
        // need to upgrade to MCTSPlayer
        this.params.information = MCTSEnums.Information.Closed_Loop;
        this.params.rolloutType = RANDOM;
        this.params.selectionPolicy = ROBUST;
        this.params.opponentTreePolicy = Paranoid;
        this.params.treePolicy = UCB;
    }

    public BasicPBMCTSPlayer(MCTSParams params) {
        this.params = params;
        rnd = new Random(params.getRandomSeed());
        setName("BasicPBMCTS");
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> allActions) {
        // Search for best action from the root
        BasicPBTreeNode root = new BasicPBTreeNode(this, null, gameState, rnd);

        // mctsSearch does all of the hard work
        root.mctsSearch();

        // Return best action
        return root.bestAction();
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        this.params.heuristic = heuristic;
    }


    @Override
    public String toString() {
        return "BasicPBMCTS";
    }

    @Override
    public BasicPBMCTSPlayer copy() {
        return this;
    }
}