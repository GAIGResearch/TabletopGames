package players.mcts.test;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
import core.AbstractGameState;
import core.actions.AbstractAction;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.mcts.SingleTreeNode;

import java.util.List;

public class TestMCTSPlayer extends MCTSPlayer {

    public SingleTreeNode root;

    public TestMCTSPlayer(MCTSParams params) {
        super(params, "TestMCTSPlayer");
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        root = new SingleTreeNode(this, null, gameState, rnd);
        root.mctsSearch(getStatsLogger());

        if (debug)
            System.out.println(root.toString());

        // Return best action
        return root.bestAction();
    }
}
