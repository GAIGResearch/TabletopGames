package games.toads;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.actions.PlayFlankCard;
import games.toads.actions.UndoOpponentFlank;
import players.mcts.*;

import java.util.List;

import static players.mcts.MCTSEnums.OpponentTreePolicy.MCGSSelfOnly;
import static players.mcts.MCTSEnums.OpponentTreePolicy.SelfOnly;

public class ToadMCTSPlayer extends MCTSPlayer {

    public ToadMCTSPlayer(MCTSParams params){
        super(params);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // We check if we are playing in defence (or are using a self-only tree)
        // if not, then we delegate to the super() method
        ToadGameState state = (ToadGameState) gameState;
        MCTSParams params = getParameters();
        if (params.opponentTreePolicy == SelfOnly || params.opponentTreePolicy == MCGSSelfOnly) {
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
        ToadGameState oldState = (ToadGameState) state.copy();
        getForwardModel().next(state, flankAction);

        // and then return the bestAction from the next state in the tree
        // How?

        AbstractAction actualAction;
        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MultiTree) {
            // in this case we use the root node for the decision player
            SingleTreeNode ourRoot = ((MultiTreeNode) root).getRoot(currentPlayer);
            actualAction = ourRoot.bestAction();
        } else if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MCGS) {
            // in this case we look up the node for the new state after applying the flank action
            Object stateKey = params.MCGSStateKey.getKey(state);
            MCGSNode node = ((MCGSNode) root).getTranspositionMap().get(stateKey);
            if (node == null) {
                throw new AssertionError("No node found for state key " + stateKey);
            }
            actualAction = node.bestAction();
        } else {
            // we have OMA or OneTree or something...we navigate manually down the tree and take the best action from the node we reach
            SingleTreeNode childNode = root.getChildren().get(flankAction)[currentPlayer];
            actualAction = childNode.bestAction();
        }
        return actualAction;
    }
}
