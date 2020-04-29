package updated_core.gamestates;

import updated_core.GameParameters;
import updated_core.actions.IAction;
import updated_core.enums.PlayerResult;
import updated_core.observations.Observation;
import updated_core.players.AbstractPlayer;

import java.util.Arrays;
import java.util.List;


/**
 * AbstractGameState handling only the most basic game mechanics
 */
public abstract class AbstractGameState {

    private final GameParameters gameParameters;
    private int turnCounter = 0;
    protected PlayerResult[] playerResults; //Todo: change the name of this variable and its enum
    protected boolean terminalState;

    public AbstractGameState(GameParameters gameParameters){
        this.gameParameters = gameParameters;
        this.playerResults = new PlayerResult[gameParameters.nPlayers];
        Arrays.fill(this.playerResults, PlayerResult.Undecided);
    }

    public abstract Observation getObservation(AbstractPlayer player);

    public abstract List<IAction> getActions(AbstractPlayer player);

    public abstract void endGame();

    public PlayerResult[] getPlayerResults() { return playerResults; }

    public int getNPlayers() { return gameParameters.nPlayers; }

    public boolean isTerminal(){ return terminalState; }

    public int getTurnCounter(){ return turnCounter; }

    public void increaseTurnCounter(){ turnCounter++; }
}
