package players.comms;


import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.communication.Message;

import java.util.List;

public interface IPlayerCommunicator {


    /**
     * Function used to communicate something to the other players of the game. This is called just before asking the
     * player for an action.
     * @param game Game being played.
     * @param state Current state of the game as observed by the emitter
     * @param availableActions Available actions for the player.
     * @param emitter Player who communicates
     */
    Message sendMessage(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter);

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward.
     * @param game Game being played.
     * @param state Current state of the game, after "action" has been applied.
     * @param action Last action executed by this player.
     * @param emitter Player who communicates
     */
    Message sendMessage(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter);

    /**
     * Function to be implemented by the player to listen to incoming communication.
     * @param state Current Game State.
     * @param receiver The player that receives this message.
     * @param messages Set of messages that are meant to be received by this player.
     */
    void listen(AbstractGameState state, AbstractPlayer receiver, List<Message> messages);
}
