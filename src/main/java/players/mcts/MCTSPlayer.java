package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

public class MCTSPlayer extends AbstractPlayer {

    // Random object for this player
    Random rnd;
    // Parameters for this player
    MCTSParams params;
    AbstractPlayer rolloutStrategy;

    public MCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public MCTSPlayer(long seed) {
        this(new MCTSParams(seed));
    }

    public MCTSPlayer(MCTSParams params) {
        this.params = params;
        rnd = new Random(this.params.getRandomSeed());
        rolloutStrategy = params.getRolloutStrategy();
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState) {
        // Gather all available actions:
        List<AbstractAction> allActions = gameState.getActions();

        // Search for best action from the root
        SingleTreeNode root = new SingleTreeNode(this, allActions.size());
        root.setRootGameState(root, gameState);
        root.mctsSearch();

        // Return best action
        return allActions.get(root.mostVisitedAction());
    }

}