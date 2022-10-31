package core;

import core.actions.AbstractAction;
import core.components.Area;
import core.components.Component;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import core.interfaces.IExtendedSequence;
import core.interfaces.IGamePhase;
import core.turnorders.TurnOrder;
import games.GameType;
import utilities.ElapsedCpuChessTimer;
import utilities.Utils;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static utilities.Utils.GameResult.GAME_ONGOING;


/**
 * Contains all game state information.
 *
 * This is distinct from the Game, of which it is a component. The Game also controls the players in the game, and
 * this information is not present in (and must not be present in) the AbstractGameState.
 *
 * A copy of the AbstractGameState is provided to each AbstractPlayer when it is their turn to act.
 * Separately the AbstractPlayer has a ForwardModel to be used if needed - this caters for the possibility that
 * agents may want to use a different/learned forward model in some use cases.
 *
 */
public abstract class AbstractGameState {

    // Default game phases: main, player reaction, end.
    public enum DefaultGamePhase implements IGamePhase {
        Main,
        PlayerReaction,
        End
    }

    // Parameters, forward model and turn order for the game
    protected final AbstractParameters gameParameters;
    protected TurnOrder turnOrder;
    private Area allComponents;

    // Timers for all players
    protected ElapsedCpuChessTimer[] playerTimer;
    // Game being played
    protected final GameType gameType;

    // A record of all actions taken to reach this game state
    private List<AbstractAction> history = new ArrayList<>();
    private List<String> historyText = new ArrayList<>();

    // Status of the game, and status for each player (in cooperative games, the game status is also each player's status)
    protected Utils.GameResult gameStatus;
    protected Utils.GameResult[] playerResults;

    // Current game phase
    protected IGamePhase gamePhase;

    // Stack for extended actions
    protected Stack<IExtendedSequence> actionsInProgress = new Stack<>();

    private int gameID;
    CoreParameters coreGameParameters;

    /**
     * Constructor. Initialises some generic game state variables.
     * @param gameParameters - game parameters.
     * @param turnOrder - turn order for this game.
     */
    public AbstractGameState(AbstractParameters gameParameters, TurnOrder turnOrder, GameType gameType){
        this.gameParameters = gameParameters;
        this.turnOrder = turnOrder;
        this.gameType = gameType;
        this.coreGameParameters = new CoreParameters();
    }
    protected AbstractGameState(AbstractParameters gameParameters, GameType type) {
        this.gameParameters = gameParameters;
        this.gameType = type;
    }


    /**
     * Resets variables initialised for this game state.
     */
    void reset() {
        turnOrder.reset();
        allComponents = new Area(-1, "All Components");
        gameStatus = GAME_ONGOING;
        playerResults = new Utils.GameResult[getNPlayers()];
        Arrays.fill(playerResults, GAME_ONGOING);
        gamePhase = DefaultGamePhase.Main;
        history = new ArrayList<>();
        historyText = new ArrayList<>();
        playerTimer = new ElapsedCpuChessTimer[getNPlayers()];
        _reset();
    }

    /**
     * Resets variables initialised for this game state.
     */
    void reset(long seed) {
        gameParameters.randomSeed = seed;
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

    // Getters
    public final TurnOrder getTurnOrder(){return turnOrder;}
    public final int getCurrentPlayer() { return turnOrder.getCurrentPlayer(this); }
    public final Utils.GameResult getGameStatus() {  return gameStatus; }
    public final AbstractParameters getGameParameters() { return this.gameParameters; }
    public final int getNPlayers() { return turnOrder.nPlayers(); }
    public final Utils.GameResult[] getPlayerResults() { return playerResults; }
    public final boolean isNotTerminal(){ return gameStatus == GAME_ONGOING; }
    public final boolean isNotTerminalForPlayer(int player){ return playerResults[player] == GAME_ONGOING && gameStatus == GAME_ONGOING; }
    public final IGamePhase getGamePhase() {
        return gamePhase;
    }
    public final Component getComponentById(int id) {
        Component c = allComponents.getComponent(id);
        if (c == null) {
            try {
                addAllComponents();
                c = allComponents.getComponent(id);
            } catch (Exception ignored) {}  // Can crash from concurrent modifications if running with GUI TODO: this is an ugly fix
        }
        return c;
    }
    public final Area getAllComponents() {
        addAllComponents(); // otherwise the list of allComponents is only ever updated when we copy the state!
        return allComponents;
    }

    /**
     * While getAllComponents() returns an Area containing every component, this method
     * returns a list of just the top-level items. So, for example, a Deck of Cards appears once here, while
     * the Area returned by getAllComponents() will contain the Deck, and every single Card it contains too.
     *
     * @return Return
     */
    public final List<Component> getAllTopLevelComponents() {
        return _getAllComponents();
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
    public final AbstractGameState copy(int playerId) {
        AbstractGameState s = _copy(playerId);
        // Copy super class things
        s.turnOrder = turnOrder.copy();
        s.allComponents = allComponents.emptyCopy();
        s.gameStatus = gameStatus;
        s.playerResults = playerResults.clone();
        s.gamePhase = gamePhase;
        s.coreGameParameters = coreGameParameters;

        if (!coreGameParameters.competitionMode) {
            s.history = new ArrayList<>(history);
            s.historyText = new ArrayList<>(historyText);
            // we do not copy individual actions in history, as these are now dead and should not change
            // History is for debugging and spectation of games. There is a risk that History might contain information
            // formally hidden to some participants. For this reason, in COMPETITION_MODE we explicitly do not copy
            // any history over in case a sneaky agent tries to take advantage of it.
            // If there is any information only available in History that could legitimately be used, then this should
            // be incorporated in the game-specific data in GameState where the correct hiding protocols can be enforced.
        }
        // TODO: uncomment
        s.actionsInProgress = new Stack<>();
        actionsInProgress.forEach(
                a -> s.actionsInProgress.push(a.copy())
        );

        s.playerTimer = new ElapsedCpuChessTimer[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++) {
            s.playerTimer[i] = playerTimer[i].copy();
        }

        // Update the list of components for ID matching in actions.
        s.addAllComponents();
        return s;
    }

    /* Methods to be implemented by subclass, protected access. */

    public IExtendedSequence currentActionInProgress() {
        return actionsInProgress.isEmpty() ? null : actionsInProgress.peek();
    }

    public boolean isActionInProgress() {
        return !actionsInProgress.empty();
    }

    public void setActionInProgress(IExtendedSequence action) {
        if (action == null && !actionsInProgress.isEmpty())
            actionsInProgress.pop();
        else
            actionsInProgress.push(action);
    }

    void checkActionsInProgress() {
        while (!actionsInProgress.isEmpty() &&
                currentActionInProgress().executionComplete(this)) {
            actionsInProgress.pop();
        }
    }

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
     * This should generally be in the range [-1, +1], with +1 being a certain win, and -1 being a certain loss
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    protected abstract double _getHeuristicScore(int playerId);

    /**
     * This provides the current score in game terms. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     * (Unlike _getHeuristicScore(), there is no constraint on the range..whatever the game rules say.
     * @param playerId - player observing the state.
     * @return - double, score of current state
     */
    public abstract double getGameScore(int playerId);

    /**
     * This is an optinal implementation and is used in getOrdinalPosition() to break any ties based on pure game score
     * Implementing this may be a simpler approach in many cases than re-implementing getOrdinalPosition()
     * For example in ColtExpress, the tie break is the number of bullet cards in hand - and this only affects the outcome
     * if the score is a tie.
     * @param playerId
     * @return
     */
    public double getTiebreak(int playerId) {
        return 0.0;
    }
    /**
     * Returns the ordinal position of a player using getGameScore().
     *
     * If a Game does not have a score, but does have the concept of player position (e.g. in a race)
     * then this method should be overridden.
     * This may also apply for games with important tie-breaking rules not visible in the raw score.
     *
     * @param playerId player ID
     * @return The ordinal position of the player; 1 is 1st, 2 is 2nd and so on.
     */
    public int getOrdinalPosition(int playerId) {
        if (playerResults[playerId] == Utils.GameResult.WIN)
            return 1;
        double playerScore = getGameScore(playerId);
        int ordinal = 1;
        for (int i = 0, n = getNPlayers(); i < n; i++) {
            double otherScore = getGameScore(i);
            if (otherScore > playerScore)
                ordinal++;
            else if (otherScore == playerScore) {
                if (getTiebreak(i) > getTiebreak(playerId))
                    ordinal++;
            }
        }
        return ordinal;
    }

    /**
     * Provide a list of component IDs which are hidden in partially observable copies of games.
     * Depending on the game, in the copies these might be completely missing, or just randomized.
     *
     * Generally speaking there is no need to implement this method if you consistently use PartialObservableDeck,
     * Deck, and IComponentContainer (for anything else that contains Components)
     *
     * Only if you have some top-level item (say a single face-down Event Card that is not in a Deck), should you need to implement
     * this.
     *
     * @param playerId - ID of player observing the state.
     * @return - list of component IDs unobservable by the given player.
     */
    protected List<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();
    }

    private List<Integer> unknownComponents(IComponentContainer<?> container, int player) {
        ArrayList<Integer> retValue = new ArrayList<>();
        if (container instanceof PartialObservableDeck<?>) {
            PartialObservableDeck<?> pod = (PartialObservableDeck<?>) container;
            for (int i = 0; i < pod.getSize(); i++) {
                if (!pod.getVisibilityForPlayer(i, player))
                    retValue.add(pod.get(i).getComponentID());
            }
        } else {
            switch (container.getVisibilityMode()) {
                case VISIBLE_TO_ALL:
                    break;
                case HIDDEN_TO_ALL:
                    retValue.addAll(container.getComponents().stream().map(Component::getComponentID).collect(toList()));
                    break;
                case VISIBLE_TO_OWNER:
                    if (((Component) container).getOwnerId() != player)
                        retValue.addAll(container.getComponents().stream().map(Component::getComponentID).collect(toList()));
                    break;
                case FIRST_VISIBLE_TO_ALL:
                    // add everything as unseen, and then remove the first element
                    retValue.addAll(container.getComponents().stream().map(Component::getComponentID).collect(toList()));
                    retValue.remove(container.getComponents().get(0).getComponentID());
                    break;
                case LAST_VISIBLE_TO_ALL:
                    // add in the ID of the last item only
                    int length = container.getComponents().size();
                    retValue.add(container.getComponents().get(length - 1).getComponentID());
                    break;
                case MIXED_VISIBILITY:
                    throw new AssertionError("If something uses this visibility mode, then you need to also add code to this method please!");
            }
        }
        // we also need to run through the contents in case that contains any Containers
        container.getComponents().stream().filter(c -> c instanceof IComponentContainer<?>).forEach( c->
                retValue.addAll(unknownComponents((IComponentContainer<?>) c, player))
        );
        return retValue;
    }

    /**
     * Resets variables initialised for this game state.
     */
    protected abstract void _reset();

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    protected abstract boolean _equals(Object o);

    /* ####### Public AI agent API ####### */

    /**
     * Public access copy method, which always does a full copy of the game state.
     * @return - full copy of this game state.
     */
    public final AbstractGameState copy() {
        return copy(-1);
    }

    public final ElapsedCpuChessTimer[] getPlayerTimer() {
        return playerTimer;
    }

    public final GameType getGameType() {
        return gameType;
    }

    public final Stack<IExtendedSequence> getActionsInProgress() {
        return actionsInProgress;
    }

    /**
     * Retrieves a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     * This should generally be in the range [-1, +1], with +1 being a certain win, and -1 being a certain loss
     * The default implementation calls the game-specific heuristic
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    public final double getHeuristicScore(int playerId) {
        return _getHeuristicScore(playerId);
    }

    /**
     * Retrieves a list of component IDs which are hidden in partially observable copies of games.
     * Depending on the game, in the copies these might be completely missing, or just randomized.
     * @param playerId - ID of player observing the state.
     * @return - list of component IDs unobservable by the given player.
     */
    public final List<Integer> getUnknownComponentsIds(int playerId) {
        // the default implementation assumes that IComponentContainer and PartialObservableDeck have all been
        // used correctly. In this situation there should be no need for any extra game-specific coding.
        // If there is, then use _getUnknownComponentsIds
        List<Component> everything = getAllTopLevelComponents();
        List<Integer> retValue = new ArrayList<>();

        for (Component c : everything) {
            if (c instanceof IComponentContainer<?>)
                retValue.addAll(unknownComponents((IComponentContainer<?>) c, playerId));
        }
        retValue.addAll(_getUnknownComponentsIds(playerId));
        return  retValue;
    }

    /**
     * Used by ForwardModel.next() to log history (very useful for debugging)
     *
     * @param action The action that has just been applied (or is about to be applied) to the game state
     */
    protected void recordAction(AbstractAction action) {
        history.add(action);
        historyText.add("Player " + this.getCurrentPlayer() + " : " + action.getString(this));
    }

    /**
     * @return All actions that have been executed on this state since reset()/initialisation
     */
    public List<AbstractAction> getHistory() {
        return new ArrayList<>(history);
    }
    public List<String> getHistoryAsText() {
        return new ArrayList<>(historyText);
    }

    void setGameID(int id) {gameID = id;} // package level deliberately
    public int getGameID() {return gameID;}
    void setCoreGameParameters(CoreParameters coreGameParameters) {
        this.coreGameParameters = coreGameParameters;
    }
    public CoreParameters getCoreGameParameters() {
        return coreGameParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractGameState)) return false;
        AbstractGameState gameState = (AbstractGameState) o;
        return Objects.equals(gameParameters, gameState.gameParameters) &&
                Objects.equals(turnOrder, gameState.turnOrder) &&
                Objects.equals(allComponents, gameState.allComponents) &&
                gameStatus == gameState.gameStatus &&
                Arrays.equals(playerResults, gameState.playerResults) &&
                Objects.equals(gamePhase, gameState.gamePhase) &&
                Objects.equals(actionsInProgress, gameState.actionsInProgress) &&
                _equals(o);
        // we deliberately exclude history from this equality check
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gameParameters, turnOrder, allComponents, gameStatus, gamePhase);
        result = 31 * result + Arrays.hashCode(playerResults);
        return result;
    }
}
