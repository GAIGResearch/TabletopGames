package games.catan;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;

public class CatanForwardModel extends ForwardModel {
    CatanParameters params;
    int nPlayers;

    public CatanForwardModel(CatanParameters pp, int nPlayers) {
        this.params = pp;
        this.nPlayers = nPlayers;
    }

    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState currentState, IAction action) {

    }
}
