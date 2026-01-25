package games.toads;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.actions.PlayFlankCard;
import games.toads.actions.UndoOpponentFlank;
import players.mcts.*;
import utilities.Pair;

import java.util.List;

import static players.mcts.MCTSEnums.OpponentTreePolicy.*;

public class ToadMCTSPlayer extends MCTSPlayer {

    boolean functionalityApplies;
    AbstractAction flankAction;

    public ToadMCTSPlayer(MCTSParams params) {
        super(params);
        if (params.opponentTreePolicy == MultiTree && params.reuseTree) {
            System.out.println("Warning: MultiTree with reuseTree is not supported in ToadMCTSPlayer");
            params.setParameterValue("reuseTree", false);
        }
    }
    @Override
    public void initializePlayer(AbstractGameState state) {
        super.initializePlayer(state);
        flankAction = null;
        functionalityApplies = false;
    }

    private boolean functionalityApplies(AbstractGameState gameState) {
        MCTSParams params = getParameters();
        if (params.opponentTreePolicy == SelfOnly || params.opponentTreePolicy == MCGSSelfOnly) {
            return false;
        }
        ToadGameState state = (ToadGameState) gameState;
        return state.getHiddenFlankCard(1 - getPlayerID()) != null;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // We check if we are playing in defence (or are using a self-only tree)
        // if not, then we delegate to the super() method
        ToadGameState state = (ToadGameState) gameState;
        MCTSParams params = getParameters();

        int currentPlayer = state.getCurrentPlayer();

        if (getPlayerID() != currentPlayer)
            throw new AssertionError("Player ID mismatch in ToadMCTSPlayer");

        if (flankAction != null && params.reuseTree) { // from the last action; we may have some clean up to do
            if (root == null) {
                throw new AssertionError("Root node is null");
            }
            if (params.opponentTreePolicy == MultiTree) {
                // null out the root node for the opponent as it has dummy data in
                MultiTreeNode multiTreeNode = (MultiTreeNode) root;
                multiTreeNode.resetRoot(1 - getPlayerID());
            } else if (params.opponentTreePolicy == MCGS) {
                // nothing to do in this case
            } else {
                // find the node that the flankAction leads to, and set this as the root
                SingleTreeNode childNode = root.getChildren().get(flankAction)[currentPlayer];
                childNode.rootify(root, state);
                root = childNode;
            }
        }
        flankAction = null;

        functionalityApplies = functionalityApplies(state);
        if (!functionalityApplies) {
            if (params.opponentTreePolicy == MultiTree) {
                // hack. the MultiTree root node has the wrong decisionPlayer on it; so we have to override
                super._getAction(state, actions);
                AbstractAction actualAction =  ((MultiTreeNode) root).getRoot(getPlayerID()).bestAction();
                this.lastAction = new Pair<>(getPlayerID(), actualAction);
                return actualAction;
            } else {
                return super._getAction(state, actions);
            }
        }
        // otherwise, we add an UndoOpponentFlank
        new UndoOpponentFlank(state);

        List<AbstractAction> validActionsForOpponent = getForwardModel().computeAvailableActions(gameState);
        flankAction = super._getAction(state, validActionsForOpponent);

        if (!(flankAction instanceof PlayFlankCard)) {
            throw new AssertionError("Expected a PlayFlankCard action");
        }
        // We then actually apply the action that gives us the most information (as they all have the same information set from
        // the perspective of the acting player)
        int highestCount = 0;
        for (AbstractAction action : getRoot().getChildren().keySet()) {
            int thisCount = getRoot().actionVisits(action);
            if (thisCount > highestCount) {
                highestCount = thisCount;
                flankAction = action;
            }
        }
        // and then return the bestAction from the *next* state in the tree

        // in this case we look up the node for the new state after applying the flank action
        // and we don't actually need to apply the flank action as this cannot affect our information state
        // (except for the currentPlayer...)
        // So we can apply *any* valid flank action
        getForwardModel().next(state, validActionsForOpponent.get(0));

        AbstractAction actualAction;
        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MultiTree) {
            // in this case we use the root node for the decision player
            SingleTreeNode ourRoot = ((MultiTreeNode) root).getRoot(currentPlayer);
            actualAction = ourRoot != null ? ourRoot.bestAction() : actions.get(rnd.nextInt(actions.size()));
        } else if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MCGS) {
            Object stateKey = params.MCGSStateKey.getKey(state);
            MCGSNode node = ((MCGSNode) root).getTranspositionMap().get(stateKey);
            if (node == null) {
                throw new AssertionError("No node found for state key " + stateKey);
            }
            actualAction = node.bestAction(actions);
        } else {
            // we have OMA or OneTree or something...we navigate manually down the tree and take the best action from the node we reach
            SingleTreeNode childNode = root.getChildren().get(flankAction)[currentPlayer];
            actualAction = childNode.bestAction(actions);
        }
        this.lastAction = new Pair<>(getPlayerID(), actualAction);
        return actualAction;
    }

    @Override
    protected void createRootNode(AbstractGameState gameState) {
        super.createRootNode(gameState);
        // we then just override the root so that redeterminisations occur from perspective of the correct player
        // otherwise we redeterminise from the root player (i.e. our opponent), which is very bad
        if (functionalityApplies) {  // this is from last time
            root.setRedeterminisationPlayer(getPlayerID());
            // then we need to correct the transposition table
            if (root instanceof MCGSNode mcgsRoot) {
                mcgsRoot.getTranspositionMap().clear();
                mcgsRoot.getTranspositionMap().put(getParameters().MCGSStateKey.getKey(gameState, getPlayerID()), mcgsRoot);
                this.oldGraphKeys.clear();
            }
        }
    }


    // for testing
    public SingleTreeNode getRoot() {
        return root;
    }

    @Override
    public ToadMCTSPlayer copy() {
        ToadMCTSPlayer copy = new ToadMCTSPlayer(getParameters());
        copy.setForwardModel(getForwardModel());
        copy.functionalityApplies = functionalityApplies;
        copy.flankAction = flankAction == null ? null : flankAction.copy();
        return copy;
    }
}
