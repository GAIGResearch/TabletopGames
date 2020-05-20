package core;

import core.actions.IAction;
import core.gamephase.DefaultGamePhase;
import core.gamephase.GamePhase;
import core.observations.IObservation;
import core.turnorder.TurnOrder;
import utilities.Utils;

import java.util.*;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class AbstractGameState {

    protected final GameParameters gameParameters;
    protected ForwardModel forwardModel;
    protected TurnOrder turnOrder;

    protected int numAvailableActions;
    protected List<IAction> availableActions;

    protected Utils.GameResult gameStatus;
    protected Utils.GameResult[] playerResults;

    protected GamePhase gamePhase;

    public AbstractGameState(GameParameters gameParameters, ForwardModel model, int nPlayers, TurnOrder turnOrder){
        this.gameParameters = gameParameters;
        this.forwardModel = model;
        this.turnOrder = turnOrder;

        numAvailableActions = 0;
        availableActions = new ArrayList<>();

        this.gameStatus = Utils.GameResult.GAME_ONGOING;
        this.playerResults = new Utils.GameResult[nPlayers];
        Arrays.fill(this.playerResults, Utils.GameResult.GAME_ONGOING);
    }

    // Setters
    public final void setTurnOrder(TurnOrder turnOrder) {
        this.turnOrder = turnOrder;
    }
    public final void setGameStatus(Utils.GameResult status) { this.gameStatus = status; }
    public final void setPlayerResult(Utils.GameResult result, int playerIdx) {  this.playerResults[playerIdx] = result; }
    public final void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }
    public final void setMainGamePhase() {
        this.gamePhase = DefaultGamePhase.Main;
    }
    public final void setEndGamePhase() {
        this.gamePhase = DefaultGamePhase.End;
    }

    // Getters
    public final TurnOrder getTurnOrder(){return turnOrder;}
    public final Utils.GameResult getGameStatus() {  return gameStatus; }
    public final GameParameters getGameParameters() { return this.gameParameters; }
    public final int getNPlayers() { return turnOrder.nPlayers(); }
    public final Utils.GameResult[] getPlayerResults() { return playerResults; }
    public final boolean isTerminal(){ return gameStatus != Utils.GameResult.GAME_ONGOING; }
    public final int nPossibleActions() { return numAvailableActions; }
    public final List<IAction> getActions() {
        return getActions(false);
    }
    public final List<IAction> getActions(boolean forceCompute) {
        if (forceCompute || availableActions == null || availableActions.size() == 0) {
            availableActions = computeAvailableActions();
            numAvailableActions = availableActions.size();
        }
        return availableActions;
    }
    public final GamePhase getGamePhase() {
        return gamePhase;
    }

    /* Methods to be implemented by subclass */
    public void endGame() {}
    public abstract IObservation getObservation(int player);
    public abstract List<IAction> computeAvailableActions();
    public abstract void setComponents();

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
