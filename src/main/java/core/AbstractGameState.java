package core;

import core.actions.AbstractAction;
import core.actions.LogEvent;
import core.components.Area;
import core.components.Component;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import core.interfaces.IExtendedSequence;
import core.interfaces.IGameEvent;
import core.interfaces.IGamePhase;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.GameType;
import utilities.ElapsedCpuChessTimer;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static core.CoreConstants.GameResult.GAME_ONGOING;
import static java.util.stream.Collectors.toList;

/**
 * Contains all game state information.
 * <p>
 * This is distinct from the Game, of which it is a component. The Game also controls the players in the game, and
 * this information is not present in (and must not be present in) the AbstractGameState.
 * <p>
 * A copy of the AbstractGameState is provided to each AbstractPlayer when it is their turn to act.
 * Separately the AbstractPlayer has a ForwardModel to be used if needed - this caters for the possibility that
 * agents may want to use a different/learned forward model in some use cases.
 */
public abstract class AbstractGameState {

    // Parameters, forward model and turn order for the game
    protected final AbstractParameters gameParameters;
    // Game being played
    protected final GameType gameType = _getGameType();
    private Area allComponents;

    // Game tick, number of iterations of game loop
    private int tick = 0;

    // Migrated from TurnOrder...may move later
    protected int roundCounter, turnCounter, turnOwner, firstPlayer;
    protected int nPlayers;
    protected int nTeams;
    protected List<IGameListener> listeners = new ArrayList<>();

    // Timers for all players
    protected ElapsedCpuChessTimer[] playerTimer;

    // A record of all actions taken to reach this game state
    private List<AbstractAction> history = new ArrayList<>();
    private List<String> historyText = new ArrayList<>();

    // Status of the game, and status for each player (in cooperative games, the game status is also each player's status)
    protected CoreConstants.GameResult gameStatus;
    protected CoreConstants.GameResult[] playerResults;
    // Current game phase
    protected IGamePhase gamePhase;
    // Stack for extended actions
    protected Stack<IExtendedSequence> actionsInProgress = new Stack<>();
    CoreParameters coreGameParameters;
    private int gameID;

    /**
     * @param gameParameters - game parameters.
     */
    public AbstractGameState(AbstractParameters gameParameters, int nPlayers) {
        this.nPlayers = nPlayers;
        this.nTeams = nPlayers;  // we always default the number of teams to the number of players
        // this is then overridden in the game-specific constructor if needed
        this.gameParameters = gameParameters;
        this.coreGameParameters = new CoreParameters();
    }

    protected abstract GameType _getGameType();

    /**
     * Resets variables initialised for this game state.
     */
    public void reset() {
        allComponents = new Area(-1, "All Components");
        gameStatus = GAME_ONGOING;
        playerResults = new CoreConstants.GameResult[getNPlayers()];
        Arrays.fill(playerResults, GAME_ONGOING);
        history = new ArrayList<>();
        historyText = new ArrayList<>();
        playerTimer = new ElapsedCpuChessTimer[getNPlayers()];
        tick = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
        firstPlayer = 0;
        actionsInProgress.clear();
    }

    /**
     * Resets variables initialised for this game state.
     */
    public void reset(long seed) {
        gameParameters.randomSeed = seed;
        reset();
    }

    // Getters
    public CoreParameters getCoreGameParameters() {
        return coreGameParameters;
    }
    public final CoreConstants.GameResult getGameStatus() {
        return gameStatus;
    }
    public final AbstractParameters getGameParameters() {
        return this.gameParameters;
    }
    public int getNPlayers() { return nPlayers; }
    public int getNTeams() { return nTeams; }
    /**
     * Returns the team number the specified player is on.
     * This defaults to one team per player and should be overridden
     * in child classes if relevant to the game
     */
    public int getTeam(int player) { return player;}
    public int getCurrentPlayer() {
        return isActionInProgress() ? actionsInProgress.peek().getCurrentPlayer(this) : turnOwner;
    }
    public final CoreConstants.GameResult[] getPlayerResults() {return playerResults;}
    public final Set<Integer> getWinners() {
        Set<Integer> winners = new HashSet<>();
        for (int i = 0; i < playerResults.length; i++) {
            if (playerResults[i] == CoreConstants.GameResult.WIN_GAME) winners.add(i);
        }
        return winners;
    }
    public final Set<Integer> getTied() {
        Set<Integer> tied = new HashSet<>();
        for (int i = 0; i < playerResults.length; i++) {
            if (playerResults[i] == CoreConstants.GameResult.DRAW_GAME) tied.add(i);
        }
        return tied;
    }
    public final IGamePhase getGamePhase() {
        return gamePhase;
    }
    public final ElapsedCpuChessTimer[] getPlayerTimer() {
        return playerTimer;
    }
    public final GameType getGameType() {
        return gameType;
    }

    /**
     * @return All actions that have been executed on this state since reset()/initialisation
     */
    public List<AbstractAction> getHistory() { return new ArrayList<>(history);}
    public List<String> getHistoryAsText() {
        return new ArrayList<>(historyText);
    }
    public int getGameID() {
        return gameID;
    }
    public int getRoundCounter() {return roundCounter;}
    public int getTurnCounter() {return turnCounter;}

    /**
     * In general getCurrentPlayer() should be used to find the current player.
     * getTurnOwner() will give a different answer if an Extended Action Sequence is in progress.
     * In this case getTurnOwner() returns the underlying player on whose turn the Action Sequence was initiated.
     * @return
     */
    public int getTurnOwner() {return turnOwner;}
    public int getFirstPlayer() {return firstPlayer;}

    // Setters
    void setCoreGameParameters(CoreParameters coreGameParameters) {
        this.coreGameParameters = coreGameParameters;
    }
    public final void setGameStatus(CoreConstants.GameResult status) {
        this.gameStatus = status;
    }
    public final void setPlayerResult(CoreConstants.GameResult result, int playerIdx) {
        this.playerResults[playerIdx] = result;
    }
    public final void setGamePhase(IGamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }
    void setGameID(int id) {
        gameID = id;
    } // package level deliberately
    void advanceGameTick() {tick++;}

    public void setTurnOwner(int newTurnOwner) {turnOwner = newTurnOwner;}
    public void setFirstPlayer(int newFirstPlayer) {
        firstPlayer = newFirstPlayer;
        turnOwner = newFirstPlayer;
    }

    public void addListener(IGameListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    /* Limited access final methods */
    public final boolean isNotTerminal() {
        return gameStatus == GAME_ONGOING;
    }

    public final boolean isNotTerminalForPlayer(int player) {
        return playerResults[player] == GAME_ONGOING && gameStatus == GAME_ONGOING;
    }
    public final int getGameTick() {return tick;}
    public final Component getComponentById(int id) {
        Component c = allComponents.getComponent(id);
        if (c == null) {
            try {
                addAllComponents();
                c = allComponents.getComponent(id);
            } catch (Exception ignored) {
            }  // Can crash from concurrent modifications if running with GUI TODO: this is an ugly fix
        }
        return c;
    }

    public final Area getAllComponents() {
        addAllComponents(); // otherwise the list of allComponents is only ever updated when we copy the state!
        return allComponents;
    }
    public double[] getFeatureVector() {return null;} //Gets a feature vector for games that have it, otherwise returns null

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

    /**
     * Adds all components given by the game to the allComponents map in the correct way, first clearing the map.
     */
    protected final void addAllComponents() {
        allComponents.clear();
        allComponents.putComponents(_getAllComponents());
    }
    // 更多的逻辑可以添加，比如检查是否所有的牌都已经被发出

    /**
     * Public access copy method, which always does a full copy of the game state.
     * (I.e. with no shuffling of hidden data)
     * Implement _copy() for game-specific functionality
     *
     * @return - full copy of this game state.
     */
    public final AbstractGameState copy() {
        return copy(-1);
    }

    /**
     * Copies the current game state, including super class methods, given player ID.
     * Reduces state variables to only those that the player observes.
     *
     * @param playerId - player observing the state
     * @return - reduced copy of the game state.
     */
    public final AbstractGameState copy(int playerId) {
        AbstractGameState s = _copy(playerId);
        // Copy super class things
        s.allComponents = allComponents.emptyCopy();
        s.gameStatus = gameStatus;
        s.playerResults = playerResults.clone();
        s.gamePhase = gamePhase;
        s.coreGameParameters = coreGameParameters;
        s.tick = tick;
        s.nPlayers = nPlayers;
        s.roundCounter = roundCounter;
        s.turnCounter = turnCounter;
        s.turnOwner = turnOwner;
        s.firstPlayer = firstPlayer;

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

    /**
     * Used by ForwardModel.next() to log history (very useful for debugging)
     *
     * @param action The action that has just been applied (or is about to be applied) to the game state
     */
    protected final void recordAction(AbstractAction action, int player) {
        history.add(action);
        historyText.add("Player " + player + " : " + action.getString(this));
    }


    // helper function to avoid time-consuming string manipulations if the message is not actually
    // going to be logged anywhere
    public void logEvent(IGameEvent event, Supplier<String> eventText) {
        if (listeners.isEmpty() && !getCoreGameParameters().recordEventHistory)
            return; // to avoid expensive string manipulations
        logEvent(event, eventText.get());
    }
    public void logEvent(IGameEvent event, String eventText) {
        LogEvent logAction = new LogEvent(eventText);
        listeners.forEach(l -> l.onEvent(Event.createEvent(event, this, logAction)));
        if (getCoreGameParameters().recordEventHistory) {
            recordHistory(eventText);
        }
    }
    public void logEvent(IGameEvent event) {
        LogEvent logAction = new LogEvent(event.name());
        listeners.forEach(l -> l.onEvent(Event.createEvent(event, this, logAction)));
        if (getCoreGameParameters().recordEventHistory) {
            recordHistory(event.name());
        }
    }

    public void recordHistory(String history) {
        historyText.add(history);
    }

    /* Methods dealing with ExtendedActions and the actionStack */

    public final IExtendedSequence currentActionInProgress() {
        return actionsInProgress.isEmpty() ? null : actionsInProgress.peek();
    }

    public final boolean isActionInProgress() {
        // This checkActionsInProgress is essential
        // When an action is completely executed this is marked on the Action (accessible via IExtendedSequence.executionComplete())
        // However this does not [currently] actively remove the action from the queue on the game state. Whenever we check the actionsInProgress queue, we
        // therefore first have to remove any completed actions (which is what checkActionsInProgress() does).
        checkActionsInProgress();
        return !actionsInProgress.empty();
    }

    public final boolean setActionInProgress(IExtendedSequence action) {
        if (action == null && !actionsInProgress.isEmpty())
            actionsInProgress.pop();
        else
            actionsInProgress.push(action);
        return true;
    }

    final void checkActionsInProgress() {
        while (!actionsInProgress.isEmpty() &&
                currentActionInProgress().executionComplete(this)) {
            actionsInProgress.pop();
        }
    }
    public final Stack<IExtendedSequence> getActionsInProgress() {
        return actionsInProgress;
    }


    /* Methods to be implemented by subclass, protected access. */

    /**
     * Returns all components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state.
     *
     * @return - List of components in the game.
     */
    protected abstract List<Component> _getAllComponents();

    /**
     * Create a copy of the game state containing only those components the given player can observe (if partial
     * observable).
     *
     * @param playerId - player observing this game state.
     */
    protected abstract AbstractGameState _copy(int playerId);

    /**
     * Provide a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     * This should generally be in the range [-1, +1], with +1 being a certain win, and -1 being a certain loss
     *
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    protected abstract double _getHeuristicScore(int playerId);

    /**
     * This provides the current score in game terms. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     * (Unlike _getHeuristicScore(), there is no constraint on the range..whatever the game rules say.
     *
     * @param playerId - player observing the state.
     * @return - double, score of current state
     */
    public abstract double getGameScore(int playerId);

    /**
     * @param playerId - the player observed
     * @param tier - if multiple tiebreaks available in the game, this parameter can be used to specify what each one does, applied in the order 1,2,3 ...
     * @return Double.MAX_VALUE - meaning no tiebreak set for the game; if overwriting, should return the player's tiebreak score, given tier
     */
    public double getTiebreak(int playerId, int tier) {
        return Double.MAX_VALUE;
    }

    /**
     * This sets the number of tieBreak levels in a game.
     * If we reach this level then we stop recursing.
     * @return
     */
    public int getTiebreakLevels() {return 5;}

    /**
     * Returns the ordinal position of a player using getGameScore().
     * <p>
     * If a Game does not have a score, but does have the concept of player position (e.g. in a race)
     * then this method should be overridden.
     * This may also apply for games with important tie-breaking rules not visible in the raw score.
     *
     * @param playerId player ID
     * @return The ordinal position of the player; 1 is 1st, 2 is 2nd and so on.
     */
    public int getOrdinalPosition(int playerId, Function<Integer, Double> scoreFunction, BiFunction<Integer, Integer, Double> tiebreakFunction) {
        int ordinal = 1;
        double playerScore = scoreFunction.apply(playerId);
        for (int i = 0, n = getNPlayers(); i < n; i++) {
            double otherScore = scoreFunction.apply(i);
            if (otherScore > playerScore)
                ordinal++;
            else if (otherScore == playerScore && tiebreakFunction != null && tiebreakFunction.apply(i, 1) != Double.MAX_VALUE) {
                if (getOrdinalPositionTiebreak(i, tiebreakFunction, 1) > getOrdinalPositionTiebreak(playerId, tiebreakFunction, 1))
                    ordinal++;
            }
        }
        return ordinal;
    }

    public int getOrdinalPositionTiebreak(int playerId, BiFunction<Integer, Integer, Double> tiebreakFunction, int tier) {
        int ordinal = 1;
        Double playerScore = tiebreakFunction.apply(playerId, tier);
        if (playerScore == null) return ordinal;

        for (int i = 0, n = getNPlayers(); i < n; i++) {
            double otherScore = tiebreakFunction.apply(i, tier);
            if (otherScore > playerScore)
                ordinal++;
            else if (otherScore == playerScore && tier < getTiebreakLevels() && tiebreakFunction.apply(i, tier+1) != null) {
                if (getOrdinalPositionTiebreak(i, tiebreakFunction, tier+1) > getOrdinalPositionTiebreak(playerId, tiebreakFunction, tier+1))
                    ordinal++;
            }
        }
        return ordinal;
    }
    public int getOrdinalPosition(int playerId) {
        return getOrdinalPosition(playerId, this::getGameScore, this::getTiebreak);
    }

    /**
     * Provide a list of component IDs which are hidden in partially observable copies of games.
     * Depending on the game, in the copies these might be completely missing, or just randomized.
     * <p>
     * Generally speaking there is no need to implement this method if you consistently use PartialObservableDeck,
     * Deck, and IComponentContainer (for anything else that contains Components)
     * <p>
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
        container.getComponents().stream().filter(c -> c instanceof IComponentContainer<?>).forEach(c ->
                retValue.addAll(unknownComponents((IComponentContainer<?>) c, player))
        );
        return retValue;
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    protected abstract boolean _equals(Object o);

    /**
     * Retrieves a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     * This should generally be in the range [-1, +1], with +1 being a certain win, and -1 being a certain loss
     * The default implementation calls the game-specific heuristic
     *
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    public final double getHeuristicScore(int playerId) {
        return _getHeuristicScore(playerId);
    }

    /**
     * Retrieves a list of component IDs which are hidden in partially observable copies of games.
     * Depending on the game, in the copies these might be completely missing, or just randomized.
     *
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
        return retValue;
    }

    /**
     * The equals method is final, but is left here so it is next to hashcode, which is not final
     * @param o
     * @return
     */

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractGameState)) return false;
        AbstractGameState gameState = (AbstractGameState) o;
        return Objects.equals(gameParameters, gameState.gameParameters) &&
                gameStatus == gameState.gameStatus &&
                nPlayers == gameState.nPlayers && roundCounter == gameState.roundCounter &&
                turnCounter == gameState.turnCounter && turnOwner == gameState.turnOwner &&
                firstPlayer == gameState.firstPlayer && tick == gameState.tick &&
                Arrays.equals(playerResults, gameState.playerResults) &&
                Objects.equals(gamePhase, gameState.gamePhase) &&
                Objects.equals(actionsInProgress, gameState.actionsInProgress) &&
                _equals(o);
        // we deliberately exclude history and allComponents from this equality check
        // this is because history is deliberately erased at times to hide hidden information (and is read-only)
        // and allComponents is not always populated (it is a convenience to get hold of all components in a game
        // at the superclass level - the actually important components are instantiated in sub-classes, and should be
        // included in the _equals() method implemented there
    }

    /**
     * Override the hashCode as needed for individual game states
     * (It is OK for two java objects to be not equal and have the same hashcode)
     *
     *         we deliberately exclude history and allComponents from the hashcode
     *         this is because history is deliberately erased at times to hide hidden information (and is read-only)
     *         and allComponents is not always populated (it is a convenience to get hold of all components in a game
     *         at the superclass level - the actually important components are instantiated in sub-classes, and should be
     *         included in the hashCode() method implemented there
     * @return
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(gameParameters, gameStatus, gamePhase, actionsInProgress);
        result = 31 * result + Objects.hash(tick, nPlayers, roundCounter, turnCounter, turnOwner, firstPlayer);
        result = 31 * result + Arrays.hashCode(playerResults);
        return result;
    }
}
