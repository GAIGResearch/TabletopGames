package core.communication;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import players.comms.IPlayerCommunicator;

import java.util.List;

public abstract class GameCommunicator {


    public enum CommMode {
        NO_COMMS,
        BASIC;
//        BEFORE_ACTION,
//        AFTER_ACTION,
//        BEFORE_AND_AFTER_ACTION

        public GameCommunicator createComms(){
            return switch (this) {
                case NO_COMMS -> new NullGameCommunicator();
                case BASIC -> new BasicGameCommunicator();
            };
        }
    }

    protected Game game; //Reference to the game being played.
    protected List<AbstractPlayer> players; // Players of this game.
    protected Blackboard blackboard;

    public void setup(Game game, List<AbstractPlayer> players)
    {
        this.game = game;
        this.players = players;
        this.blackboard = new Blackboard();
    }


    public void reset() {
        blackboard = new Blackboard();
    }

    /**
     * Function used to communicate something to the other players of the game. This is called just before asking the
     * player for an action.
     * @param observation Current state of the game as observed by the emitter
     * @param avActions Available actions for the player.
     * @param currentPlayer Player who communicates
     */
    public void OnBeforeAction(AbstractPlayer currentPlayer, AbstractGameState observation, List<AbstractAction> avActions)
    {
        if(currentPlayer.parameters.comms != null)
            _OnBeforeAction(currentPlayer.parameters.comms, currentPlayer, observation, avActions);
    }

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward.
     * @param observation Current state of the game, after "action" has been applied.
     * @param action Last action executed by this player.
     * @param currentPlayer Player who communicates
     */
    public void OnAfterAction(AbstractPlayer currentPlayer, AbstractGameState observation, AbstractAction action)
    {
        if(currentPlayer.parameters.comms != null)
            _OnAfterAction(currentPlayer.parameters.comms, currentPlayer, observation, action);
    }


    /**
     * Function used to communicate something to the other players of the game. This is called just before asking the
     * player for an action.
     * @param observation Current state of the game as observed by the emitter
     * @param avActions Available actions for the player.
     * @param currentPlayer Player who communicates
     */
    public abstract void _OnBeforeAction(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, List<AbstractAction> avActions);

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward.
     * @param observation Current state of the game, after "action" has been applied.
     * @param action Last action executed by this player.
     * @param currentPlayer Player who communicates
     */
    public abstract void _OnAfterAction(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, AbstractAction action);

}
