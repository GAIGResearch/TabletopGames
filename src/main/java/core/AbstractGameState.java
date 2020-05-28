package core;

import core.actions.AbstractAction;
import core.components.Area;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.interfaces.IObservation;
import core.turnorders.TurnOrder;
import utilities.Utils;

import java.util.*;

/**
 * Contains all game state information.
 */
public abstract class AbstractGameState {

    // Default game phases: main, player reaction, end.
    public enum DefaultGamePhase implements IGamePhase {
        Main,
        PlayerReaction,
        End
    }

    // Parameters, forward model and turn order for the game
    protected final AbstractGameParameters gameParameters;
    protected AbstractForwardModel forwardModel;
    protected TurnOrder turnOrder;
    protected Area<Component> allComponents;

    // List of actions currently available for the player
    protected List<AbstractAction> availableActions;

    // Status of the game, and status for each player (in cooperative games, the game status is also each player's status)
    protected Utils.GameResult gameStatus;
    protected Utils.GameResult[] playerResults;

    // Current game phase
    protected IGamePhase gamePhase;

    // Data for this game
    protected AbstractGameData data;

    /**
     * Constructor. Initialises some generic game state variables.
     * @param gameParameters - game parameters.
     * @param model - forward model.
     * @param turnOrder - turn order for this game.
     */
    public AbstractGameState(AbstractGameParameters gameParameters, AbstractForwardModel model, TurnOrder turnOrder){
        this.gameParameters = gameParameters;
        this.forwardModel = model;
        this.turnOrder = turnOrder;
        this.allComponents = new Area<>(-1, "All Components");
    }

    // Setters
    public final void setTurnOrder(TurnOrder turnOrder) {
        this.turnOrder = turnOrder;
    }
    public final void setGameStatus(Utils.GameResult status) { this.gameStatus = status; }
    public final void setPlayerResult(Utils.GameResult result, int playerIdx) {  this.playerResults[playerIdx] = result; }
    public final void setGamePhase(IGamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }
    public final void setMainGamePhase() {
        this.gamePhase = DefaultGamePhase.Main;
    }

    // Getters
    public final TurnOrder getTurnOrder(){return turnOrder;}
    public final int getCurrentPlayer() { return turnOrder.getCurrentPlayer(this); }
    public final Utils.GameResult getGameStatus() {  return gameStatus; }
    public final AbstractGameParameters getGameParameters() { return this.gameParameters; }
    public final int getNPlayers() { return turnOrder.nPlayers(); }
    public final Utils.GameResult[] getPlayerResults() { return playerResults; }
    public final boolean isNotTerminal(){ return gameStatus == Utils.GameResult.GAME_ONGOING; }
    public final List<AbstractAction> getActions() {
        return getActions(false);
    }
    public final List<AbstractAction> getActions(boolean forceCompute) {
        if (forceCompute || availableActions == null || availableActions.size() == 0) {
            availableActions = computeAvailableActions();
        }
        return availableActions;
    }
    public final IGamePhase getGamePhase() {
        return gamePhase;
    }
    public AbstractGameData getData() {
        return data;
    }
    public Component getComponentById(int id) {
        return allComponents.getComponent(id);
    }

    /* Methods to be implemented by subclass */

    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     */
    public void endGame() {}

    /**
     * Retrieves an observation specific to the given player from this game state object. Components which are not
     * observed by the player are removed, the rest are copied.
     * @param player - player observing this game state.
     * @return - IObservation, the observation for this player.
     */
    public abstract IObservation getObservation(int player);

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    public abstract List<AbstractAction> computeAvailableActions();

    /**
     * Must add all components used in the game to the allComponents area, mapping to their assigned component ID
     * and NOT another game specific key. Use one of these functions for this functionality only:
     *          - Area.putComponent(Component component)
     *          - Area.putComponents(List<Component> components)
     *          - Area.putComponents(Area area)
     * Method is called after initialising the game state.
     */
    public abstract void addAllComponents();

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
