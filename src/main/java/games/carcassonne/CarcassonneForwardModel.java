package carcassonne;

import actions.IAction;
import core.ForwardModel;
import core.AbstractGameState;
import turnorder.TurnOrder;

public class CarcassonneForwardModel implements ForwardModel {

    public CarcassonneForwardModel(){

    }

    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState currentState, TurnOrder turnOrder, IAction IAction) {

    }

    @Override
    public ForwardModel copy() {
        return null;
    }
}
