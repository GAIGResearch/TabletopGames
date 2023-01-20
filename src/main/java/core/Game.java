package core;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IPrintable;
import core.turnorders.ReactiveTurnOrder;
import evaluation.metrics.Event;
import evaluation.listeners.GameListener;
import games.GameType;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import utilities.Pair;

//import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static utilities.Utils.componentToImage;

public class Game {

    private static final AtomicInteger idFountain = new AtomicInteger(0);
    // Type of game
    private final GameType gameType;
    public boolean paused;
    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;
    // Real game state and forward model
    protected AbstractGameState gameState;
    protected AbstractForwardModel forwardModel;
    private List<GameListener> listeners = new ArrayList<>();

    /* Game Statistics */
    private int lastPlayer; // used to track actions per 'turn'

    // Timers for various function calls
    private double nextTime, copyTime, agentTime, actionComputeTime;
    // Keeps track of action spaces for each game tick, pairs of (player ID, #actions)
    private ArrayList<Pair<Integer, Integer>> actionSpaceSize;
    // Number of times an agent is asked for decisions
    private int nDecisions;
    // Number of actions taken in a turn by a player
    private int nActionsPerTurn, nActionsPerTurnSum, nActionsPerTurnCount;
    private boolean pause, stop;
    private boolean debug = false;
    // Video recording

    String fileName = "output.mp4";
    String formatName = "mp4";
    String codecName = null;
    int snapsPerSecond = 10;
    private int turnPause;

    /**
     * Game constructor. Receives a list of players, a forward model and a game state. Sets unique and final
     * IDs to all players in the game, and performs initialisation of the game state and forward model objects.
     *
     * @param players   - players taking part in this game.
     * @param realModel - forward model used to apply game rules.
     * @param gameState - object used to track the state of the game in a moment in time.
     */
    public Game(GameType type, List<AbstractPlayer> players, AbstractForwardModel realModel, AbstractGameState gameState) {
        this.gameType = type;
        this.gameState = gameState;
        this.forwardModel = realModel;
        reset(players);
    }

    /**
     * Game constructor. Receives a forward model and a game state.
     * Performs initialisation of the game state and forward model objects.
     *
     * @param model     - forward model used to apply game rules.
     * @param gameState - object used to track the state of the game in a moment in time.
     */
    public Game(GameType type, AbstractForwardModel model, AbstractGameState gameState) {
        this.gameType = type;
        this.forwardModel = model;
        this.gameState = gameState;
        reset(Collections.emptyList(), gameState.gameParameters.randomSeed);
    }

    public void setTurnPause(int turnPause) {
        this.turnPause = turnPause;
    }


    public final void reset(List<AbstractPlayer> players) {
        reset(players, gameState.gameParameters.randomSeed);
    }

    /**
     * Resets the game. Sets up the game state to the initial state as described by game rules, assigns players
     * and their IDs, and initialises all players.
     *
     * @param players       - new players for the game
     * @param newRandomSeed - random seed is updated in the game parameters object and used throughout the game.
     */
    public final void reset(List<AbstractPlayer> players, long newRandomSeed) {
        gameState.reset(newRandomSeed);
        forwardModel.abstractSetup(gameState);
        if (players.size() == gameState.getNPlayers()) {
            this.players = players;
        } else if (players.isEmpty()) {
            // keep existing players
        } else
            throw new IllegalArgumentException("PlayerList provided to Game.reset() must be empty, or have the same number of entries as there are players");
        int id = 0;
        if (this.players != null)
            for (AbstractPlayer player : this.players) {
                // Create an FM copy for this player (different random seed)
                player.setForwardModel(this.forwardModel.copy());
                // Create initial state observation
                AbstractGameState observation = gameState.copy(id);
                // Give player their ID
                player.playerID = id++;
                // Allow player to initialize

                player.initializePlayer(observation);
            }
        int gameID = idFountain.incrementAndGet();
        gameState.setGameID(gameID);
        resetStats();
    }

    /**
     * All timers and game tick set to 0.
     */
    public void resetStats() {
        nextTime = 0;
        copyTime = 0;
        agentTime = 0;
        actionComputeTime = 0;
        nDecisions = 0;
        actionSpaceSize = new ArrayList<>();
        nActionsPerTurnSum = 0;
        nActionsPerTurn = 1;
        nActionsPerTurnCount = 0;
        lastPlayer = -1;
        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ABOUT_TO_START, gameState)));
    }

    /**
     * Runs the game,
     */
    public final void run() {

        boolean firstEnd = true;

        while (gameState.isNotTerminal() && !stop) {

            synchronized (this) {

                // Now synchronized with possible intervention from the GUI
                // This is only relevant if the game has been paused...so should not affect
                // performance in non-GUI situations
                try {
                    while (pause && !isHumanToMove()) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    // Meh.
                }
                int activePlayer = gameState.getCurrentPlayer();
                if (debug) System.out.printf("Entered synchronized block in Game for player %s%n", activePlayer);

                AbstractPlayer currentPlayer = players.get(activePlayer);

                // we check via a volatile boolean, otherwise GUI button presses do not trigger this
                // as the JVM hoists pause and isHumanToMove() ouside the while loop on the basis that
                // they cannot be changed in this thread....


                /*
                 * The Game is responsible for tracking the players and the current game state
                 * It is important that the Game never passes the main AbstractGameState to the individual players,
                 * but instead always uses copy(playerId) to both:
                 * i) shuffle any hidden data they cannot see
                 * ii) ensure that any changes the player makes to the game state do not affect the genuine game state
                 *
                 * Players should never have access to the Game, or the main AbstractGameState, or to each other!
                 */

                // Get player to ask for actions next
                boolean reacting = (gameState.getTurnOrder() instanceof ReactiveTurnOrder
                        && ((ReactiveTurnOrder) gameState.getTurnOrder()).getReactivePlayers().size() > 0);

                // Check if this is the same player as last, count number of actions per turn
                if (!reacting) {
                    if (currentPlayer != null && activePlayer == lastPlayer) {
                        nActionsPerTurn++;
                    } else {
                        nActionsPerTurnSum += nActionsPerTurn;
                        nActionsPerTurn = 1;
                        nActionsPerTurnCount++;
                    }
                }

                if (gameState.isNotTerminal()) {

                    if (debug) System.out.printf("Invoking oneAction from Game for player %d%n", activePlayer);
                    oneAction();

                } else {
                    if (firstEnd) {
                        if (gameState.coreGameParameters.verbose) {
                            System.out.println("Ended");
                        }
                        terminate();
                        firstEnd = false;
                    }
                }

                if (debug) System.out.println("Exiting synchronized block in Game");
            }
        }
        if (firstEnd) {
            if (gameState.coreGameParameters.verbose) {
                System.out.println("Ended");
            }
            terminate();
        }
    }

    public final boolean isHumanToMove() {
        int activePlayer = gameState.getCurrentPlayer();
        return this.getPlayers().get(activePlayer) instanceof HumanGUIPlayer;
    }

    public final AbstractAction oneAction() {

        // we pause before each action is taken if running with a delay (e.g. for video recording with random players)
        if (turnPause > 0)
            synchronized (this) {
                try {
                    wait(turnPause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        // This is the next player to be asked for a decision
        int activePlayer = gameState.getCurrentPlayer();
        AbstractPlayer currentPlayer = players.get(activePlayer);
        if (debug) System.out.printf("Starting oneAction for player %s%n", activePlayer);

        // Get player observation, and time how long it takes
        double s = System.nanoTime();
        // copying the gamestate also copies the game parameters and resets the random seed (so agents cannot use this
        // to reconstruct the starting hands etc.)
        AbstractGameState observation = gameState.copy(activePlayer);
        copyTime += (System.nanoTime() - s);

        // Get actions for the player
        s = System.nanoTime();
        List<AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);
        actionComputeTime += (System.nanoTime() - s);
        actionSpaceSize.add(new Pair<>(activePlayer, observedActions.size()));

        if (gameState.coreGameParameters.verbose) {
            System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());
        }

        if (observation instanceof IPrintable && gameState.coreGameParameters.verbose) {
            ((IPrintable) observation).printToConsole();
        }

        // Start the timer for this decision
        gameState.playerTimer[activePlayer].resume();

        // Either ask player which action to use or, in case no actions are available, report the updated observation
        AbstractAction action = null;
        if (observedActions.size() > 0) {
            if (observedActions.size() == 1 && (!(currentPlayer instanceof HumanGUIPlayer || currentPlayer instanceof HumanConsolePlayer) || observedActions.get(0) instanceof DoNothing)) {
                // Can only do 1 action, so do it.
                action = observedActions.get(0);
                currentPlayer.registerUpdatedObservation(observation);
            } else {
                // Get action from player, and time it
                s = System.nanoTime();
                if (debug) System.out.printf("About to get action for player %d%n", gameState.getCurrentPlayer());
                action = currentPlayer.getAction(observation, observedActions);
                agentTime += (System.nanoTime() - s);
                nDecisions++;
            }
            if (gameState.coreGameParameters.competitionMode && action != null && !observedActions.contains(action)) {
                System.out.printf("Action played that was not in the list of available actions: %s%n", action.getString(gameState));
                action = null;
            }
            // We publish an ACTION_CHOSEN message before we implement the action, so that observers can record the state that led to the decision
            AbstractAction finalAction = action;
            listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ACTION_CHOSEN, gameState, finalAction)));
        } else {
            currentPlayer.registerUpdatedObservation(observation);
        }

        // End the timer for this decision
        gameState.playerTimer[activePlayer].pause();
        gameState.playerTimer[activePlayer].incrementAction();

        if (gameState.coreGameParameters.verbose && !(action == null)) {
            System.out.println(action);
        }
        if (action == null)
            throw new AssertionError("We have a NULL action in the Game loop");

        // Check player timeout
        if (observation.playerTimer[activePlayer].exceededMaxTime()) {
            forwardModel.disqualifyOrRandomAction(gameState.coreGameParameters.disqualifyPlayerOnTimeout, gameState);
        } else {
            // Resolve action and game rules, time it
            s = System.nanoTime();
            forwardModel.next(gameState, action);
            nextTime += (System.nanoTime() - s);
        }

        gameState.advanceGameTick();

        lastPlayer = activePlayer;

        // We publish an ACTION_TAKEN message once the action is taken so that observers can record the result of the action
        // (such as the next player)
        AbstractAction finalAction1 = action;
        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ACTION_TAKEN, gameState, finalAction1.copy())));

        if (debug) System.out.printf("Finishing oneAction for player %s%n", activePlayer);
        return action;
    }

    /**
     * Called at the end of game loop execution, when the game is over.
     */
    private void terminate() {
        // Print last state
        if (gameState instanceof IPrintable && gameState.coreGameParameters.verbose) {
            ((IPrintable) gameState).printToConsole();
        }

        // Timers should average
        terminateTimers();

        // Perform any end of game computations as required by the game
        forwardModel.endGame(gameState);
        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, gameState)));
        if (gameState.coreGameParameters.recordEventHistory) {
            gameState.recordHistory(Event.GameEvent.GAME_OVER.name());
        }
        if (gameState.coreGameParameters.verbose) {
            System.out.println("Game Over");
        }

        // Allow players to terminate
        for (AbstractPlayer player : players) {
            player.finalizePlayer(gameState.copy(player.getPlayerID()));
        }

        // Close video recording writer
        //terminateVideoRecording();

        // Inform listeners of game end
//        for (GameListener gameTracker : listeners) {
//            gameTracker.allGamesFinished();
//        }
    }

    /**
     * Timers average at the end of the game.
     */
    private void terminateTimers() {
        nextTime /= gameState.getGameTick();
        copyTime /= gameState.getGameTick();
        actionComputeTime /= gameState.getGameTick();
        agentTime /= nDecisions;
        if (nActionsPerTurnCount > 0)
            nActionsPerTurnSum /= nActionsPerTurnCount;
    }

    /**
     * Retrieves the current game state.
     *
     * @return - current game state.
     */
    public final AbstractGameState getGameState() {
        return gameState;
    }

    /**
     * Retrieves the forward model.
     *
     * @return - forward model.
     */
    public AbstractForwardModel getForwardModel() {
        return forwardModel;
    }

    /**
     * Retrieves agent timer value, i.e. how long the AI players took to make decisions in this game.
     *
     * @return - agent time
     */
    public double getAgentTime() {
        return agentTime;
    }

    /**
     * Retrieves the copy timer value, i.e. how long the game state took to produce player observations in this game.
     *
     * @return - copy time
     */
    public double getCopyTime() {
        return copyTime;
    }

    /**
     * Retrieves the next timer value, i.e. how long the forward model took to advance the game state with an action.
     *
     * @return - next time
     */
    public double getNextTime() {
        return nextTime;
    }

    /**
     * Retrieves the action compute timer value, i.e. how long the forward model took to compute the available actions
     * in a game state.
     *
     * @return - action compute time
     */
    public double getActionComputeTime() {
        return actionComputeTime;
    }

    /**
     * Retrieves the number of game loop repetitions performed in this game.
     *
     * @return - tick number
     */
    public int getTick() {
        return gameState.getGameTick();
    }

    /**
     * Retrieves number of decisions made by the AI players in the game.
     *
     * @return - number of decisions
     */
    public int getNDecisions() {
        return nDecisions;
    }

    /**
     * Number of actions taken in a turn by a player, before turn moves to another.
     *
     * @return - number of actions per turn
     */
    public int getNActionsPerTurn() {
        return nActionsPerTurnSum;
    }

    /**
     * Retrieves a list with one entry per game tick, each a pair (player ID, # actions)
     *
     * @return - list of action space sizes
     */
    public ArrayList<Pair<Integer, Integer>> getActionSpaceSize() {
        return actionSpaceSize;
    }

    /**
     * Which game is this?
     *
     * @return type of game.
     */
    public GameType getGameType() {
        return gameType;
    }

    public void addListener(GameListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            gameState.turnOrder.addListener(listener);
            listener.setGame(this);
        }
    }
    public List<GameListener> getListeners() {
        return listeners;
    }

    public void clearListeners() {
        listeners.clear();
        getGameState().turnOrder.clearListeners();
    }

    /**
     * Retrieves the list of players in the game.
     *
     * @return - players list
     */
    public List<AbstractPlayer> getPlayers() {
        return players;
    }

    public boolean isPaused() {
        return pause;
    }

    public void setPaused(boolean paused) {
        this.pause = paused;
    }

    public void flipPaused() {
        this.paused = !this.paused;
    }

    public boolean isStopped() {
        return stop;
    }

    public void setStopped(boolean stopped) {
        this.stop = stopped;
    }

    public CoreParameters getCoreParameters() {
        return gameState.coreGameParameters;
    }

    public void setCoreParameters(CoreParameters coreParameters) {
        this.gameState.setCoreGameParameters(coreParameters);
    }

    @Override
    public String toString() {
        return gameType.toString();
    }

}
