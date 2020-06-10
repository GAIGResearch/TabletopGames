package games.catan;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;

import java.util.List;

public class CatanForwardModel extends AbstractForwardModel {
    CatanParameters params;
    int nPlayers;

    public CatanForwardModel(){}

    public CatanForwardModel(CatanParameters pp, int nPlayers) {
        this.params = pp;
        this.nPlayers = nPlayers;
    }

    @Override
    protected void _setup(AbstractGameState firstState) {

    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return null;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return null;
    }
}
