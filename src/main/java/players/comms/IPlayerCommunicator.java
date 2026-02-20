package players.comms;


import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.communication.Message;

import java.util.List;

public interface IPlayerCommunicator {

    Message sendMessage(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter);
    Message sendMessage(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter);

}
