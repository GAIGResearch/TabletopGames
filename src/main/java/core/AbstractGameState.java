package core;

import actions.IAction;
import gamestates.PlayerResult;
import observations.Observation;
import pandemic.Constants;
import players.AbstractPlayer;

import java.util.*;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class AbstractGameState {

    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game
    //protected ArrayList<Integer> reactivePlayers;


    protected int nPlayers;
    public int getNPlayers() { return nPlayers; }

    public int roundStep;

    /**
     * Set of parameters for this game.
     */
    protected final GameParameters gameParameters;

    protected boolean terminalState;
    protected Constants.GameResult gameStatus = Constants.GameResult.GAME_ONGOING;
    protected PlayerResult[] playerResults;
    public PlayerResult[] getPlayerResults() { return playerResults; }


    public AbstractGameState(GameParameters gameParameters){
        this.gameParameters = gameParameters;
        this.playerResults = new PlayerResult[gameParameters.nPlayers];
        Arrays.fill(this.playerResults, PlayerResult.Undecided);
    }

    /*
    public AbstractGameState(AbstractGameState gameState) {
        this(gameState.gameParameters);
        this.activePlayer = gameState.activePlayer;
        this.nPlayers = gameState.nPlayers;
        this.roundStep = gameState.roundStep;
        this.gameStatus = gameState.gameStatus;
    }

    protected AbstractGameState _copy()
    {
        AbstractGameState gsCopy = this.createNewGameState();



        return gsCopy;
    }

     */


    /**
     * Creates a new GameState object.
     * @return the new GameState object
     */
    //public abstract AbstractGameState createNewGameState();

    /**
     * Copies the game state objects defined in the subclass of this game state to a
     * new GameState object and returns it.
     * @param dest      GameState where things need to be copied to.
     * @param playerId ID of the player for which this copy is being created (so observations can be
     *                 adapted for them). -1 indicates the game state should be copied at full.
     */
    //public abstract void copyTo(AbstractGameState dest, int playerId);

    //public abstract void setComponents(String dataPath);

    //Getters & setters
    public Constants.GameResult getGameStatus() {  return gameStatus; }
    //public GameParameters getGameParameters() { return this.gameParameters; }

    public void setGameOver(Constants.GameResult status){  this.gameStatus = status; }


    /* Methods to be implemented by subclass */
    public abstract List<IAction> getActions(AbstractPlayer player);

    public boolean isTerminal(){ return terminalState; }


    public abstract Observation getObservation(AbstractPlayer player);

    public abstract void endGame();

    /*
    public final void init()
    {
        reactivePlayers = new ArrayList<>();
    }


    void setForwardModel(ForwardModel fm) { this.forwardModel = fm; }
    ForwardModel getModel() {return this.forwardModel;}


    void setGameParameters(GameParameters gp) { this.gameParameters = gp; }

    public int getActingPlayer() {  // Returns player taking an action (or possibly a reaction) next
        if (reactivePlayers.size() == 0)
            return activePlayer;
        else return reactivePlayers.get(0);
    }
    public ArrayList<Integer> getReactivePlayers() { return reactivePlayers; }  // Returns players queued to react
    public void addReactivePlayer(int player) { reactivePlayers.add(player); }
    public boolean removeReactivePlayer() {
        if (reactivePlayers.size() > 0) {
            reactivePlayers.remove(0);
            return true;
        }
        return false;
    }
    */
}
