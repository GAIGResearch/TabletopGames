package players.comms;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.communication.Message;
import players.basicMCTS.BasicMCTSPlayer;

import java.util.List;

public class BasicMCTSCommunicator implements IPlayerCommunicator{

    @Override
    public Message sendMessage(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter) {

        BasicMCTSPlayer player = (BasicMCTSPlayer) emitter;
        AbstractAction action = player._getAction(state, availableActions);
        return new Message(emitter.getPlayerID(), -1, Message.Receiver.All, action);
    }

    @Override
    public Message sendMessage(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter) {
        return null;
    }

    public void listen(AbstractPlayer receiver, List<Message> messages){
        for(Message m : messages)
            System.out.println("["+ receiver.getName() + "("+ receiver.getPlayerID() +")] I've received a message from player " +
                    m.from + ": " + m.msg);

    }

}
