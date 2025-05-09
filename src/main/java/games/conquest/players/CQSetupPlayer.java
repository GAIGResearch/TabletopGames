package games.conquest.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.conquest.CQGameState;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;

import java.util.List;

public class CQSetupPlayer extends MCTSPlayer {
    public CQSetupPlayer(MCTSParams params) {
        super(params);
    }

    public CQSetupPlayer(MCTSParams params, String name) {
        super(params, name);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        if (gameState.getGamePhase().equals(CQGameState.CQGamePhase.SetupPhase)) {
            // HACK: This uses `omaVisits` as a parameter to register which troop setup it uses.
            // This avoids having to create a separate MCTSparam for this playout.
            return actions.get(((MCTSParams) parameters).omaVisits % actions.size());
        } else {
            return super._getAction(gameState, actions);
        }
    }

    @Override
    public CQSetupPlayer copy() {
        CQSetupPlayer retValue = new CQSetupPlayer((MCTSParams) getParameters().copy(), toString());
        if (getForwardModel() != null)
            retValue.setForwardModel(getForwardModel());
        return retValue;
    }
}
