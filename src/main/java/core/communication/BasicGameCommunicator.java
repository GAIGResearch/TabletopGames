package core.communication;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.comms.IPlayerCommunicator;

import java.util.List;

public class BasicGameCommunicator extends GameCommunicator {

    @Override
    public void _OnBeforeAction(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, List<AbstractAction> avActions) {
        Message msg = comms.sendMessage(game, observation, avActions, currentPlayer);
        if(msg != null) {
            msg.setTick(observation.getGameTick());
            blackboard.post(msg, observation);
            blackboard.broadcastLast(players, observation.getGameTick());
        }
    }

    @Override
    public void _OnAfterAction(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, AbstractAction action) {
        Message msg = comms.sendMessage(game, observation, action, currentPlayer);
        if (msg != null) {
            msg.setTick(observation.getGameTick());
            blackboard.post(msg, observation);
            blackboard.broadcastLast(players, observation.getGameTick());
        }
    }

}
