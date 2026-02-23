package core.communication;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.comms.IPlayerCommunicator;

import java.util.List;

public class NullGameCommunicator extends GameCommunicator {

    @Override
    public void OnBeforeActionPlayer(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, List<AbstractAction> avActions) {
        //Nothing
    }

    @Override
    public void OnAfterActionPlayer(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, AbstractAction action) {
        //Nothing
    }
}
