package core.communication;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.comms.IPlayerCommunicator;

import java.util.ArrayList;
import java.util.List;

public class MultiGameCommunicator extends GameCommunicator {

    // When preparing copies of game states, the one used to send to other players than the current must have available actions.
    // If not, we redeterminize "redetTolerance" times. If still nothing - we send an actual copy of the real game state.
    // Receiving player does not know when this happens.
    public final int redetTolerance = 25;

    /**
     * Function used to communicate something to the other players of the game. This is called just before asking the
     * player for an action. Calls ALL players before a player takes an action in the game, so each player can
     * send information to anyone else. Player who's going to play sends information first.
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

        // Then the rest
        for (AbstractPlayer ap : players)
            if(ap != currentPlayer & ap.parameters.comms != null) {
                avActions = new ArrayList<>();
                redeterminizeState(model, gameState, ap, avActions);
                OnBeforeActionPlayer(ap.parameters.comms, ap, observation, avActions);

//                try {
//                    OnBeforeActionPlayer(ap.parameters.comms, ap, observation, avActions);
//                }catch(AssertionError er)
//                {
//                    observation = gameState.copy(ap.getPlayerID());
//                    avActions = model.computeAvailableActions(observation, ap.getParameters().actionSpace);
//                    OnBeforeActionPlayer(ap.parameters.comms, ap, observation, avActions);
//                }
            }

    }

    private AbstractGameState redeterminizeState(AbstractForwardModel model, AbstractGameState gameState,
                                                 AbstractPlayer ap, List<AbstractAction> avActions)
    {
        int tries = 0;
        AbstractGameState observation = gameState.copy(ap.getPlayerID());
        List<AbstractAction> actions = model.computeAvailableActions(observation, ap.getParameters().actionSpace);
        while(actions.isEmpty() && tries < redetTolerance)
        {
            // The game says there's no actions for the root player, which is impossible. This is because
            // of the determinization of the hidden state. Try again resampling the state.
            observation = gameState.copy(ap.getPlayerID());
            actions = model.computeAvailableActions(observation, ap.getParameters().actionSpace);
            tries++;
        }

        if(actions.isEmpty())
        {
            //We didn't manage to get a determinization that is compatible with having available actions.
            //"Cheat" using the actual game state.
            observation = gameState.copy(-1);
            actions = model.computeAvailableActions(observation, ap.getParameters().actionSpace);
        }

        System.out.println("Redeterminized at attempt " + (tries+1));
        avActions.addAll(actions);
        return observation;

    }

    /**
     * Function used to communicate something to the other players of the game. This is called just after the player
     * has provided an action and the state has been rolled forward. Calls ALL players after a player takes an action
     * in the game, so each player can send information to anyone else. Player who just plays sends information first.
     * @param players All players of the game
     * @param lastPlayerID ID of the player who played the last action
     * @param action Last action executed by a player.
     * @param gameState Current game state (after action applied in game), not obfuscated for visibility
     */
    public void OnAfterAction(List<AbstractPlayer> players, int lastPlayerID, AbstractGameState gameState, AbstractAction action)
    {
        //Last player first
        AbstractPlayer lastPlayer = players.get(lastPlayerID);
        AbstractGameState observation = gameState.copy(lastPlayer.getPlayerID());
        if(lastPlayer.parameters.comms != null)
            OnAfterActionPlayer(lastPlayer.parameters.comms, lastPlayer, observation, action);

        // Then, the rest of the players.
        for (AbstractPlayer ap : players)
            if(lastPlayer != ap && ap.parameters.comms != null) {
                observation = gameState.copy(ap.getPlayerID());
                OnAfterActionPlayer(ap.parameters.comms, ap, observation, action);
            }
    }


}
