package players.comms;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.communication.Message;
import core.interfaces.ICommunicator;

import java.util.List;

// Example class. It just communicates the action played or the number of available actions it has before playing.
public class NameCommunicator implements ICommunicator {

    @Override
    public void send(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter) {
        String message = emitter.toString()  + " (" + action.toString() + ")";;
        game.post(emitter.getPlayerID(), Message.Receiver.All, message);
    }

    @Override
    public void send(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter) {
        String message = emitter.toString() + " (" + availableActions.size() + ")";
        game.post(emitter.getPlayerID(), Message.Receiver.All, message);
    }

}
