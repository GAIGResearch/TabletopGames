package core;

import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;

import java.util.ArrayList;
import java.util.List;

public class DecoratedForwardModel extends AbstractForwardModel {


    // This wraps a Forward Model in one or more Decorators that modify (restrict) the actions available to the player.
    // This enables the Forward Model to be passed to the decision algorithm (e.g. MCTS), and ensure that any
    // restrictions are applied to the actions available to the player during search, and not just
    // in the main game loop.

    // most function calls are forwarded to the wrapped forward model, except for
    // _computeAvailableActions, to which we first apply the decorators

    final List<IPlayerDecorator> decorators;
    final AbstractForwardModel wrappedFM;
    final int decisionPlayerID;

    public DecoratedForwardModel(AbstractForwardModel forwardModel, List<IPlayerDecorator> decorators, int playerID) {
        this.wrappedFM = forwardModel;
        this.decorators = new ArrayList<>(decorators);
        this.decisionPlayerID = playerID;
    }

    @Override
    protected void _setup(AbstractGameState firstState) {
        wrappedFM._setup(firstState);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        wrappedFM._next(currentState, action);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = wrappedFM.computeAvailableActions(gameState);
        for (IPlayerDecorator decorator : decorators) {
            if (decorator.decisionPlayerOnly() && gameState.getCurrentPlayer() != decisionPlayerID)
                continue;
            actions = decorator.actionFilter(gameState, actions);
        }
        return actions;
    }

    @Override
    protected void endPlayerTurn(AbstractGameState state) {
        wrappedFM.endPlayerTurn(state);
    }

}
