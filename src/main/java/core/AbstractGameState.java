package core;

import core.actions.IAction;
import core.gamestates.PlayerResult;
import core.observations.Observation;
import core.turnorder.TurnOrder;
import games.pandemic.Constants;

import java.util.*;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class AbstractGameState {

    protected ForwardModel forwardModel;
    protected TurnOrder turnOrder;
    public TurnOrder getTurnOrder(){return turnOrder;}
    protected void setTurnOrder(TurnOrder turnOrder){ this.turnOrder = turnOrder;}

    protected int nPlayers;
    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game
    //protected ArrayList<Integer> reactivePlayers;
    protected int numAvailableActions = 0;
    protected List<IAction> availableActions;

    public int roundStep;
    protected boolean terminalState;
    protected Constants.GameResult gameStatus = Constants.GameResult.GAME_ONGOING;
    protected PlayerResult[] playerResults;

    // Set of parameters for this game.
    protected final GameParameters gameParameters;

    public AbstractGameState(GameParameters gameParameters, int nPlayers){
        this.gameParameters = gameParameters;
        this.playerResults = new PlayerResult[nPlayers];
        Arrays.fill(this.playerResults, PlayerResult.Undecided);
        availableActions = new ArrayList<>();
    }

    //Getters & setters
    public Constants.GameResult getGameStatus() {  return gameStatus; }
    void setForwardModel(ForwardModel fm) { this.forwardModel = fm; }
    ForwardModel getModel() {return this.forwardModel;}
    //public GameParameters getGameParameters() { return this.gameParameters; }
    public void setGameOver(Constants.GameResult status){  this.gameStatus = status; }
    public int getNPlayers() { return nPlayers; }
    public PlayerResult[] getPlayerResults() { return playerResults; }
    public boolean isTerminal(){ return terminalState; }
    public int nPossibleActions() { return numAvailableActions; }
    public final List<IAction> getActions(int player) {
        if (availableActions == null || availableActions.size() == 0) {
            availableActions = computeAvailableActions(player);
        }
        return availableActions;
    }
    public List<IAction> setAvailableActions(List<IAction> actions, int player) {
        if (actions != null && actions.size() > 0) {
            numAvailableActions = actions.size();
            availableActions = actions;
        } else computeAvailableActions(player);
        return availableActions;
    }

    /* Methods to be implemented by subclass */
    public abstract Observation getObservation(int player);
    public abstract void endGame();
    public abstract List<IAction> computeAvailableActions(int player);

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

    public abstract AbstractGameState createNewGameState();

    public abstract void copyTo(AbstractGameState dest, int playerId);

    public abstract void setComponents(String dataPath);

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
