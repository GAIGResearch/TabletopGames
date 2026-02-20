package players.comms;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.communication.Message;

import java.util.List;

// Example class. It just communicates the action played or the number of available actions it has before playing to ALL players.
public class BasicPlayerCommunicator implements IPlayerCommunicator {

    @Override
    public Message sendMessage(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter) {
        String msgStr = emitter  + " (" + action.toString() + ")";
        return new Message(emitter.getPlayerID(), -1, Message.Receiver.All, msgStr);
    }

    @Override
    public Message sendMessage(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter) {
        String msgStr = emitter + " (" + availableActions.size() + ")";
        return new Message(emitter.getPlayerID(), -1, Message.Receiver.All, msgStr);
    }

}
