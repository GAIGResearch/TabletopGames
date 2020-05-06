package uno;

import actions.IAction;
import core.AbstractGameState;
import core.ForwardModel;
import turnorder.TurnOrder;

public class UnoForwardModel extends ForwardModel {

    @Override
    public void next(AbstractGameState gameState, TurnOrder turnOrder, IAction action) {
        action.Execute(gameState, turnOrder);
    }
}
