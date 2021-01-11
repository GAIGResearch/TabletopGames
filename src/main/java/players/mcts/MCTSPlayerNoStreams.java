package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public class MCTSPlayerNoStreams extends MCTSPlayer {

    public MCTSPlayerNoStreams(MCTSParams mctsParams) {
        super(mctsParams);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        SingleTreeNodeNoStreams root = new SingleTreeNodeNoStreams(this, null, gameState, rnd);
        root.mctsSearch(getStatsLogger());

        // Return best action
        return root.bestAction();
    }
}
