package core.communication;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import players.comms.IPlayerCommunicator;

import java.util.List;

public class GameCommunicator {


    public enum CommMode {
        NO_COMMS,
        BASIC,
        MULTI;
//        BEFORE_ACTION,
//        AFTER_ACTION,
//        BEFORE_AND_AFTER_ACTION

        public GameCommunicator createComms(){
            return switch (this) {
                case NO_COMMS -> new NullGameCommunicator();
                case BASIC -> new GameCommunicator();
                case MULTI -> new MultiGameCommunicator();
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
     * Overloading this method allows the communicator to call ANY player before a player takes an action in the game,
     * so each player can send information to anyone else.
     * @param players All players of the game
     * @param gameState Current game state, not obfuscated for visibility.
     * @param model Forward Model of the game.
     */
    public void OnBeforeAction(List<AbstractPlayer> players, AbstractGameState gameState, AbstractForwardModel model)
    {
        //Current player
        AbstractPlayer currentPlayer = players.get(gameState.getCurrentPlayer());
        AbstractGameState observation = gameState.copy(currentPlayer.getPlayerID());
        List<AbstractAction> avActions = model.computeAvailableActions(observation, currentPlayer.getParameters().actionSpace);
        if(currentPlayer.parameters.comms != null)
            OnBeforeActionPlayer(currentPlayer.parameters.comms, currentPlayer, observation, avActions);
    }

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward.
     * Overloading this method allows the communicator to call ANY player after a player takes an action
     * in the game, so players can send information to anyone else.
     * @param players All players of the game
     * @param lastPlayerID ID of the player who played the last action
     * @param action Last action executed by a player.
     * @param gameState Current game state (after action applied in game), not obfuscated for visibility
     */
    public void OnAfterAction(List<AbstractPlayer> players, int lastPlayerID, AbstractGameState gameState, AbstractAction action)
    {
        //Last player
        AbstractPlayer lastPlayer = players.get(lastPlayerID);
        AbstractGameState observation = gameState.copy(lastPlayer.getPlayerID());
        if(lastPlayer.parameters.comms != null)
            OnAfterActionPlayer(lastPlayer.parameters.comms, lastPlayer, observation, action);
    }


    /**
     * Function used to communicate something to the other players of the game. This is called just before asking the
     * player for an action.
     * Overload this function to manage how messages are sent, stored and broadcast to other players.
     * @param comms Player Communicator
     * @param observation Current state of the game as observed by the emitter
     * @param avActions Available actions for the player.
     * @param currentPlayer Player who communicates something
     */
    public void OnBeforeActionPlayer(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, List<AbstractAction> avActions)
    {
        Message msg = comms.sendMessage(game, observation, avActions, currentPlayer);
        if(msg != null) {
            msg.setTick(observation.getGameTick());
            blackboard.post(msg, observation);
            blackboard.broadcastLast(players, observation.getGameTick());
        }
    }

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward.
     * Overload this function to manage how messages are sent, stored and broadcast to other players.
     * @param comms Player Communicator
     * @param observation Current state of the game, after "action" has been applied.
     * @param action Last action executed by this player.
     * @param currentPlayer Player who communicates something
     */
    public void OnAfterActionPlayer(IPlayerCommunicator comms, AbstractPlayer currentPlayer, AbstractGameState observation, AbstractAction action)
    {
        Message msg = comms.sendMessage(game, observation, action, currentPlayer);
        if (msg != null) {
            msg.setTick(observation.getGameTick());
            blackboard.post(msg, observation);
            blackboard.broadcastLast(players, observation.getGameTick());
        }
    }

}
