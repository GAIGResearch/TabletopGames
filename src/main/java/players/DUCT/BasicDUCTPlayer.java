package players.DUCT;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.PlayerParameters;

import java.util.*;


public class BasicDUCTPlayer extends AbstractPlayer {


    public BasicDUCTPlayer(PlayerParameters parameters, String name) {
        super(parameters, name);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return null;
    }

    @Override
    public AbstractPlayer copy() {
        return null;
    }
}
