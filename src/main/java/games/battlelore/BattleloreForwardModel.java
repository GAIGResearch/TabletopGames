package games.battlelore;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;
import core.components.Token;

import java.util.List;

public class BattleloreForwardModel extends AbstractForwardModel
{
    @Override
    protected void _setup(AbstractGameState initialState)
    {
        BattleloreParameters gameParams = (BattleloreParameters) initialState.getGameParameters();
        int hexHeight = gameParams.hexHeight;
        int hexWidth = gameParams.hexWidth;
        BattleloreGameState gameState = (BattleloreGameState)initialState;
        gameState.gridBoard = new GridBoard<>(hexHeight, hexWidth, new Token(BattleloreConstants.emptyHex));
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
