package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import java.util.List;
import java.util.Random;

public class MCTSPlayer extends AbstractPlayer {

    Random m_rnd;
    MCTSParams params;

    public MCTSPlayer()
    {
        this.params = new MCTSParams();
        m_rnd = new Random(this.params.seed);
    }

    public MCTSPlayer(MCTSParams params) {
        this.params = params;
        m_rnd = new Random(this.params.seed);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState) {
        //Gather all available actions:
        List<AbstractAction> allActions = gameState.getActions();

        SingleTreeNode m_root = new SingleTreeNode(this, allActions.size());
        m_root.setRootGameState(m_root, gameState);
        m_root.mctsSearch();

        return allActions.get(m_root.mostVisitedAction());
    }
}