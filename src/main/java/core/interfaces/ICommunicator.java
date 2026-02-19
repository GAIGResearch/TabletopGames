package core.interfaces;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;

import java.util.List;

public interface ICommunicator {

    /**
     * Function used to communicate something to the other players of the game. This is called just before asking the
     * player for an action.
     * @param game Game that needs to be used to communicate with other players. This method must use Game.post(...) to make those communications.
     * @param state Current state of the game
     * @param availableActions Available actions for the player.
     * @param emitter Player who communicates
     */
    void send(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter);

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward.
     * @param game Game that needs to be used to communicate with other players. This method must use Game.post(...) to make those communications.
     * @param state Current state of the game, after "action" has been applied.
     * @param action Last action executed by this player.
     * @param emitter Player who communicates
     */
    void send(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter);



}
