package updated_core.gamestates;

import updated_core.ForwardModel;
import updated_core.GameParameters;
import updated_core.actions.IAction;
import updated_core.observations.Observation;
import updated_core.players.AbstractPlayer;

import java.util.List;


/**
 * Interface GameState handling only the most basic game mechanics
 */
public abstract class GameState {

    public int activePlayer;
    public final GameParameters gameParameters;
    private final ForwardModel forwardModel;

    public GameState(GameParameters gameParameters, ForwardModel forwardModel){
        this.gameParameters = gameParameters;
        this.forwardModel = forwardModel;
    }

    public abstract Observation getObservation(AbstractPlayer player);

    public abstract List<IAction> getActions(AbstractPlayer player);

    public abstract void getWinner();

    public int getNPlayers() { return gameParameters.nPlayers; }

    public int getActivePlayer() { return activePlayer; }
}
