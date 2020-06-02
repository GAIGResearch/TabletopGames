package core;

import core.actions.AbstractAction;
import core.components.Area;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.observations.VectorObservation;
import core.turnorders.TurnOrder;
import utilities.Distance;
import utilities.Pair;
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
    protected TurnOrder turnOrder;
    private Area<Component> allComponents;

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
        this.allComponents = new Area<>(-1, "All Components");
    }

    /**
     * Resets variables initialised for this game state.
     */
    void reset() {
        turnOrder.reset();
        allComponents.clear();
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
    public final boolean isNotTerminal(){ return gameStatus == Utils.GameResult.GAME_ONGOING; }
    public final List<AbstractAction> getActions() {
        return Collections.unmodifiableList(availableActions);
    }
    public final IGamePhase getGamePhase() {
        return gamePhase;
    }
    public Component getComponentById(int id) {
        return allComponents.getComponent(id);
    }

    /* Final methods */

    /**
     * Package access only, copies the current game state, including super class methods, given player ID.
     * Reduces state variables to only those that the player observes.
     * @param playerId - player observing the state
     * @return - reduced copy of the game state.
     */
    final AbstractGameState _copy(int playerId) {
        AbstractGameState s = copy(playerId);
        // Copy super class things
        s.turnOrder = turnOrder.copy();
        s.allComponents.clear();
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

    /**
     * Public access copy method, which always does a full copy of the game state.
     * @return - full copy of this game state.
     */
    public final AbstractGameState copy() {
        return _copy(-1);
    }

    /**
     * Provide a numerical assessment of the current game state's distance to the other game state provided.
     * @param playerId - the player to calculate the score for.
     * @return double, distance to the other state provided.
     */
    public final double getDistance(AbstractGameState otherState, int playerId) {
        double[] features = getDistanceFeatures(playerId);
        double[] otherFeatures = otherState.getDistanceFeatures(playerId);
        return Distance.manhattan_distance(features, otherFeatures);
    }

    /**
     * Provide a numerical assessment of the current game state's distance to the other distance features provided.
     * @param playerId - the player to calculate the score for.
     * @return double, distance to the other features vector.
     */
    public final double getDistance(double[] otherFeatures, int playerId) {
        double[] features = getDistanceFeatures(playerId);
        assert otherFeatures.length == features.length;
        return Distance.manhattan_distance(features, otherFeatures);
    }

    /**
     * Calculates the distances to all terminal states, returning a list of pairs (Distance, GameResult).
     * @param playerId - player observing the state.
     * @return - list of (distance, game result) pairs.
     */
    public final ArrayList<Pair<Double, Utils.GameResult>> getDistanceToTerminalStates(int playerId) {
        ArrayList<Pair<Double, Utils.GameResult>> distances = new ArrayList<>();
        double[] features = getDistanceFeatures(playerId);

        HashMap<HashMap<Integer, Double>, Utils.GameResult> terminalFeatures = getTerminalFeatures(playerId);
        for (Map.Entry<HashMap<Integer, Double>, Utils.GameResult> e: terminalFeatures.entrySet()) {
            double[] otherFeatures = new double[features.length];
            for (Map.Entry<Integer, Double> m : e.getKey().entrySet()) {
                otherFeatures[m.getKey()] = m.getValue();
            }
            distances.add(new Pair<>(Distance.manhattan_distance(features, otherFeatures), e.getValue()));
        }
        return distances;
    }

    /**
     * Adds all components given by the game to the allComponents map in the correct way, first clearing the map.
     */
    public final void addAllComponents() {
        allComponents.clear();
        allComponents.putComponents(_getAllComponents());
    }

    /* Methods to be implemented by subclass */

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
    protected abstract AbstractGameState copy(int playerId);

    /**
     * Encode the game state into a vector (fixed length during game).
     * @return - a vector observation.
     */
    public abstract VectorObservation getVectorObservation();

    /**
     * Create a double vector representation, where each element represents a feature in the game space by which
     * distance to another game state can be measured (in feature space), e.g.:
     *      - game score
     *      - player position
     *      - event counter value
     *      - round number
     * @param playerId - player observing the state
     * @return - int array, vector of features.
     */
    public abstract double[] getDistanceFeatures(int playerId);

    /**
     * Return all distance feature vectors which describe final game states, with associated result for the given player.
     * Includes a mapping from feature index (as given in getDistanceFeatures() method) to feature value, and
     * associated game result.
     * When features in distance feature vectors extracted from a state coincide with any of these values,
     * the game state is terminal.
     * @param playerId - player observing the state.
     * @return - map from terminal feature vector to game result.
     */
    public abstract HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId);

    /**
     * Provide a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    public abstract double getScore(int playerId);
}
