package games.toads;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.actions.PlayFlankCard;
import games.toads.actions.UndoOpponentFlank;
import players.mcts.*;

import java.util.List;

public class ToadMCTSPlayer extends MCTSPlayer {

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // We check if we are playing in defence (or are using a self-only tree)
        // if not, then we delegate to the super() method
        ToadGameState state = (ToadGameState) gameState;
        MCTSParams params = getParameters();
        if (params.opponentTreePolicy.selfOnlyTree) {
            return super._getAction(gameState, actions);
        }

        int currentPlayer = state.getCurrentPlayer();
        if (state.getHiddenFlankCard(1 - currentPlayer) == null) {
            return super._getAction(state, actions);
        }
        // otherwise, we add an UndoOpponentFlank

        new UndoOpponentFlank(state);
        AbstractAction flankAction = super._getAction(state, getForwardModel().computeAvailableActions(gameState));

        // we then apply the bestAction (which should be w PlayFlankCard)
        if (!(flankAction instanceof PlayFlankCard)) {
            throw new AssertionError("Expected a PlayFlankCard action");
        }
        getForwardModel().next(state, flankAction);

        // and then return the bestAction from the next state in the tree
        // How?

        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MultiTree) {
            // in this case we use the root node for the decision player
        } else if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MCGS) {
            // in this case we look up the node for the new state after applying the flank action
        } else {
            // we have OMA or OneTree or something...we navigate manually down the tree and take the best action from the node we reach
        }
    }
}
