package core;

import core.actions.IAction;
import core.observations.IObservation;
import core.turnorder.TurnOrder;
import utilities.Utils;

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
    protected Utils.GameResult gameStatus = Utils.GameResult.GAME_ONGOING;
    protected Utils.GameResult[] playerResults;

    // Set of parameters for this game.
    protected final GameParameters gameParameters;

    public AbstractGameState(GameParameters gameParameters, int nPlayers){
        this.nPlayers = nPlayers;
        this.gameParameters = gameParameters;
        this.playerResults = new Utils.GameResult[nPlayers];
        Arrays.fill(this.playerResults, Utils.GameResult.GAME_ONGOING);
        availableActions = new ArrayList<>();
    }

    //Getters & setters
    public Utils.GameResult getGameStatus() {  return gameStatus; }
    public void setForwardModel(ForwardModel fm) { this.forwardModel = fm; }
    //public GameParameters getGameParameters() { return this.gameParameters; }
    public final void setGameStatus(Utils.GameResult status) { this.gameStatus = status; }
    public final void setPlayerResult(Utils.GameResult result, int playerIdx) {  this.playerResults[playerIdx] = result; }
    public final int getNPlayers() { return nPlayers; }
    public final Utils.GameResult[] getPlayerResults() { return playerResults; }
    public final boolean isTerminal(){ return terminalState; }
    public final int nPossibleActions() { return numAvailableActions; }
    public final List<IAction> getActions(int player) {
        if (availableActions == null || availableActions.size() == 0) {
            availableActions = computeAvailableActions(player);
        }
        return availableActions;
    }
    public final List<IAction> setAvailableActions(List<IAction> actions, int player) {
        if (actions != null && actions.size() > 0) {
            numAvailableActions = actions.size();
            availableActions = actions;
        } else computeAvailableActions(player);
        return availableActions;
    }

    /* Methods to be implemented by subclass */
    public abstract IObservation getObservation(int player);
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
