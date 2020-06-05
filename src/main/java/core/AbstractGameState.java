package core;

import core.actions.AbstractAction;
import core.components.Area;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.turnorders.TurnOrder;
import utilities.Utils;

import java.util.*;

import static utilities.Utils.GameResult.GAME_ONGOING;


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
    protected TurnOrder turnOrder;
    private Area allComponents;

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
     * @param turnOrder - turn order for this game.
     */
    public AbstractGameState(AbstractGameParameters gameParameters, TurnOrder turnOrder){
        this.gameParameters = gameParameters;
        this.turnOrder = turnOrder;
    }

    /**
     * Resets variables initialised for this game state.
     */
    void reset() {
        turnOrder.reset();
        allComponents = new Area(-1, "All Components");
        availableActions = new ArrayList<>();
        gameStatus = GAME_ONGOING;
        playerResults = new Utils.GameResult[getNPlayers()];
        Arrays.fill(playerResults, GAME_ONGOING);
        gamePhase = DefaultGamePhase.Main;
        _reset();
    }

    /**
     * Resets variables initialised for this game state.
     */
    void reset(long seed) {
        gameParameters.gameSeed = seed;
        reset();
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
    public final void setAvailableActions(List<AbstractAction> availableActions) {
        this.availableActions = availableActions;
    }

    // Getters
    public final TurnOrder getTurnOrder(){return turnOrder;}
    public final int getCurrentPlayer() { return turnOrder.getCurrentPlayer(this); }
    public final Utils.GameResult getGameStatus() {  return gameStatus; }
    public final AbstractGameParameters getGameParameters() { return this.gameParameters; }
    public final int getNPlayers() { return turnOrder.nPlayers(); }
    public final Utils.GameResult[] getPlayerResults() { return playerResults; }
    public final boolean isNotTerminal(){ return gameStatus == GAME_ONGOING; }
    public final List<AbstractAction> getActions() {
        return Collections.unmodifiableList(availableActions);
    }
    public final IGamePhase getGamePhase() {
        return gamePhase;
    }
    public final Component getComponentById(int id) {
        return allComponents.getComponent(id);
    }
    public final Area getAllComponents() {
        return allComponents;
    }
    /* Limited access final methods */

    /**
     * Adds all components given by the game to the allComponents map in the correct way, first clearing the map.
     */
    protected final void addAllComponents() {
        allComponents.clear();
        allComponents.putComponents(_getAllComponents());
    }

    /**
     * Copies the current game state, including super class methods, given player ID.
     * Reduces state variables to only those that the player observes.
     * @param playerId - player observing the state
     * @return - reduced copy of the game state.
     */
    final AbstractGameState copy(int playerId) {
        AbstractGameState s = _copy(playerId);
        // Copy super class things
        s.turnOrder = turnOrder.copy();
        s.allComponents = new Area(-1, "All components");
        s.gameStatus = gameStatus;
        s.playerResults = playerResults.clone();
        s.gamePhase = gamePhase;
        s.data = data;  // Should never be modified

        s.availableActions = new ArrayList<>();
        for (AbstractAction a: availableActions) {
            s.availableActions.add(a.copy());
        }

        // Update the list of components for ID matching in actions.
        s.addAllComponents();
        return s;
    }

    /* Methods to be implemented by subclass, protected access. */

    /**
     * Returns all components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state.
     * @return - List of components in the game.
     */
    protected abstract List<Component> _getAllComponents();

    /**
     * Create a copy of the game state containing only those components the given player can observe (if partial
     * observable).
     * @param playerId - player observing this game state.
     */
    protected abstract AbstractGameState _copy(int playerId);

    /**
     * Provide a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    protected abstract double _getScore(int playerId);

    /**
     * Resets variables initialised for this game state.
     */
    protected abstract void _reset();


    /* ####### Public AI agent API ####### */

    /**
     * Public access copy method, which always does a full copy of the game state.
     * @return - full copy of this game state.
     */
    public final AbstractGameState copy() {
        return copy(-1);
    }

    /**
     * Provide a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    public final double getScore(int playerId) {
        return _getScore(playerId);
    }

}
