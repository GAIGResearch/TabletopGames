package core;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import core.turnorders.ReactiveTurnOrder;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import evaluation.summarisers.TAGNumericStatSummary;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GUI;
import gui.GamePanel;
import players.basicMCTS.BasicMCTSPlayer;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.FirstActionPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;
import utilities.Utils;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static games.GameType.*;


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
    private List<IGameListener> listeners = new ArrayList<>();

    /* Game Statistics */
    private int lastPlayer; // used to track actions per 'turn'
    private JFrame frame;
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
    private Rectangle areaBounds;
    private boolean recordingVideo = false;
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

    /**
     * Runs one game.
     *
     * @param gameToPlay          - game to play
     * @param players             - list of players for the game
     * @param seed                - random seed for the game
     * @param randomizeParameters - if true, parameters are randomized for each run of each game (if possible).
     * @return - game instance created for the run
     */
    public static Game runOne(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
                              boolean randomizeParameters, List<IGameListener> listeners, ActionController ac, int turnPause) {
        // Creating game instance (null if not implemented)
        Game game;
        if (parameterConfigFile != null) {
            AbstractParameters params = AbstractParameters.createFromFile(gameToPlay, parameterConfigFile);
            game = gameToPlay.createGameInstance(players.size(), seed, params);
        } else game = gameToPlay.createGameInstance(players.size(), seed);
        if (game == null)
            System.out.println("Error game: " + gameToPlay);

        if (listeners != null) {
            Set<String> agentNames = players.stream()
                    //           .peek(a -> System.out.println(a.toString()))
                    .map(AbstractPlayer::toString).collect(Collectors.toSet());

            for (IGameListener gameTracker : listeners) {
                gameTracker.init(game, players.size(), agentNames);
                game.addListener(gameTracker);
            }
        }

        // Randomize parameters
        if (randomizeParameters) {
            AbstractParameters gameParameters = game.getGameState().getGameParameters();
            gameParameters.randomize();
            System.out.println("Parameters: " + gameParameters);
        }

        // Reset game instance, passing the players for this game
        game.reset(players);
        game.setTurnPause(turnPause);

        if (ac != null) {
            // We spawn the GUI off in another thread

            GUI frame = new GUI();
            GamePanel gamePanel = new GamePanel();
            frame.setContentPane(gamePanel);

            AbstractGUIManager gui = gameToPlay.createGUIManager(gamePanel, game, ac);

            frame.setFrameProperties();
            frame.validate();
            frame.pack();

            // Video recording setup
            if (game.recordingVideo) {
                game.areaBounds = new Rectangle(0, 0, frame.getWidth(), frame.getHeight());
            }

            Timer guiUpdater = new Timer((int) game.getCoreParameters().frameSleepMS, event -> game.updateGUI(gui, frame));
            guiUpdater.start();

            game.run();
            guiUpdater.stop();
            // and update GUI to final game state
            game.updateGUI(gui, frame);

        } else {
            // Run!
            game.run();
        }

        return game;
    }

    /**
     * Runs several games with a given random seed.
     *
     * @param gamesToPlay         - list of games to play.
     * @param players             - list of players for the game.
     * @param nRepetitions        - number of repetitions of each game.
     * @param seed                - random seed for all games. If null, a new random seed is used for each game.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     * @param detailedStatistics  - if true, detailed statistics are printed, otherwise just average of wins
     */
    public static void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, Long seed,
                               int nRepetitions, boolean randomizeParameters,
                               boolean detailedStatistics, List<IGameListener> listeners, int turnPause) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            String[] split = players.get(i).getClass().toString().split("\\.");
            String agentName = split[split.length - 1] + "-" + i;
            overall[i] = new TAGNumericStatSummary("Overall " + agentName);
            agentNames[i] = agentName;
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGNumericStatSummary("{Game: " + gt.name() + "; Player: " + agentNames[i] + "}");
            }

            // Play n repetitions of this game and record player results
            Game game = null;
            int offset = 0;
            for (int i = 0; i < nRepetitions; i++) {
                Long s = seed;
                if (s == null) s = System.currentTimeMillis();
                s += offset;
                game = runOne(gt, null, players, s, randomizeParameters, listeners, null, turnPause);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                    offset = game.getGameState().getRoundCounter() * game.getGameState().getNPlayers();
                } else {
                    break;
                }
//                System.out.println("Game " + i + "/" + nRepetitions);
            }

            if (game != null) {
                System.out.println("---------------------");
                for (int i = 0; i < nPlayers; i++) {
                    // Print statistics for this game
                    if (detailedStatistics) {
                        System.out.println(statSummaries[i].toString());
                    } else {
                        System.out.println(statSummaries[i].name + ": " + statSummaries[i].mean() + " (n=" + statSummaries[i].n() + ")");
                    }

                    // Record in overall statistics
                    overall[i].add(statSummaries[i]);
                }
            }
        }

        // Print final statistics
        System.out.println("\n=====================\n");
        for (int i = 0; i < nPlayers; i++) {
            // Print statistics for this game
            if (detailedStatistics) {
                System.out.println(overall[i].toString());
            } else {
                System.out.println(overall[i].name + ": " + overall[i].mean());
            }
        }
    }

    /**
     * Runs several games with a set of random seeds, one for each repetition of a game.
     *
     * @param gamesToPlay         - list of games to play.
     * @param players             - list of players for the game.
     * @param nRepetitions        - number of repetitions of each game.
     * @param seeds               - random seeds array, one for each repetition of a game.
     * @param ac                  - action controller for GUI interactions, null if playing without visuals.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     */
    public static void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, int nRepetitions,
                               long[] seeds, ActionController ac, boolean randomizeParameters, List<IGameListener> listeners, int turnPause) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            overall[i] = new TAGNumericStatSummary("Overall Player " + i);
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGNumericStatSummary("Game: " + gt.name() + "; Player: " + i);
            }

            // Play n repetitions of this game and record player results
            for (int i = 0; i < nRepetitions; i++) {
                Game game = runOne(gt, null, players, seeds[i], randomizeParameters, listeners, null, turnPause);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                }
            }

            for (int i = 0; i < nPlayers; i++) {
                // Print statistics for this game
                System.out.println(statSummaries[i].toString());

                // Record in overall statistics
                overall[i].add(statSummaries[i]);
            }
        }

        // Print final statistics
        System.out.println("\n---------------------\n");
        for (int i = 0; i < nPlayers; i++) {
            // Print statistics for this game
            System.out.println(overall[i].toString());
        }
    }

    /**
     * Records statistics of given game into the given StatSummary objects. Only WIN, LOSE or DRAW are valid results
     * recorded.
     *
     * @param statSummaries - object recording statistics
     * @param game          - finished game
     */
    public static void recordPlayerResults(TAGNumericStatSummary[] statSummaries, Game game) {
        int nPlayers = statSummaries.length;
        CoreConstants.GameResult[] results = game.getGameState().getPlayerResults();
        for (int p = 0; p < nPlayers; p++) {
            if (results[p] == CoreConstants.GameResult.WIN_GAME || results[p] == CoreConstants.GameResult.LOSE_GAME || results[p] == CoreConstants.GameResult.DRAW_GAME) {
                statSummaries[p].add(results[p].value);
            }
        }
    }

    public void setTurnPause(int turnPause) {
        this.turnPause = turnPause;
    }

    /**
     * Performs GUI update.
     *
     * @param gui - gui to update.
     */
    private void updateGUI(AbstractGUIManager gui, JFrame frame) {
        // synchronise on game to avoid updating GUI in middle of action being taken
        AbstractGameState gameState = getGameState();
        int currentPlayer = gameState.getCurrentPlayer();
        AbstractPlayer player = getPlayers().get(currentPlayer);
        if (gui != null) {
            gui.update(player, gameState, isHumanToMove());
            frame.repaint();
        }
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
        } else if (players.size() == gameState.nTeams){
            this.players = new ArrayList<>();
            // In this case we use (copies of) each agent for all players on the team
            // loop over each player; find out what team they are in; and add an agent copy
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                int team = gameState.getTeam(i);
                AbstractPlayer player = players.get(team);
                this.players.add(player.copy());
            }
        } else
            throw new IllegalArgumentException("PlayerList provided to Game.reset() must be empty, or have the same number of entries as there are players");
        int id = 0;
        if (this.players != null)
            for (AbstractPlayer player : this.players) {
                // Give player their ID
                player.playerID = id++;
                // Create a FM copy for this player (different random seed)
                player.setForwardModel(this.forwardModel.copy());
                // Create initial state observation
                AbstractGameState observation = gameState.copy(player.playerID);
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
    }

    /**
     * Runs the game,
     */
    public final void run() {

        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ABOUT_TO_START, gameState)));

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

                // Get player to ask for actions next (This horrendous line is for backwards compatibility).
                boolean reacting = (gameState instanceof AbstractGameStateWithTurnOrder && ((AbstractGameStateWithTurnOrder) gameState).getTurnOrder() instanceof ReactiveTurnOrder
                        && ((ReactiveTurnOrder) ((AbstractGameStateWithTurnOrder) gameState).getTurnOrder()).getReactivePlayers().size() > 0);

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
        if (!gameState.isNotTerminalForPlayer(activePlayer))
            throw new AssertionError("Player " + activePlayer + " is not allowed to move");
        AbstractPlayer currentPlayer = players.get(activePlayer);
        if (debug) System.out.printf("Starting oneAction for player %s%n", activePlayer);

        // Get player observation, and time how long it takes
        double s = System.nanoTime();
        // copying the gamestate also copies the game parameters and resets the random seed (so agents cannot use this
        // to reconstruct the starting hands etc.)
        AbstractGameState observation = gameState.copy(activePlayer);
        copyTime = (System.nanoTime() - s);
        //      System.out.printf("Total copyTime in ms = %.2f at tick %d (Avg %.3f) %n", copyTime / 1e6, tick, copyTime / (tick +1.0) / 1e6);

        // Get actions for the player
        s = System.nanoTime();
        List<AbstractAction> observedActions = forwardModel.computeAvailableActions(observation, currentPlayer.getParameters().actionSpace);
        if (observedActions.size() == 0) {
            Stack<IExtendedSequence> actionsInProgress = gameState.getActionsInProgress();
            IExtendedSequence topOfStack = null;
            AbstractAction lastAction = null;
            if (actionsInProgress.size() > 0) {
                topOfStack = actionsInProgress.peek();
            }
            if (gameState.getHistory().size() > 1) {
                lastAction = gameState.getHistory().get(gameState.getHistory().size() - 1);
            }
            throw new AssertionError("No actions available for player " + activePlayer
                    + (lastAction != null ? ". Last action: " + lastAction.getClass().getSimpleName() + " (" + lastAction + ")" : ". No actions in history")
                    + ". Actions in progress: " + actionsInProgress.size()
                    + (topOfStack != null ? ". Top of stack: " + topOfStack.getClass().getSimpleName() + " (" + topOfStack + ")" : ""));

        }
        actionComputeTime = (System.nanoTime() - s);
        actionSpaceSize.add(new Pair<>(activePlayer, observedActions.size()));

        if (gameState.coreGameParameters.verbose) {
            System.out.println("Round: " + gameState.getRoundCounter());
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
                if (debug)
                    System.out.printf("About to get action for player %d%n", gameState.getCurrentPlayer());
                action = currentPlayer.getAction(observation, observedActions);
                if (debug)
                    System.out.printf("Game: %2d Tick: %3d\t%s%n", gameState.getGameID(), getTick(), action.getString(gameState));

                agentTime += (System.nanoTime() - s);
                nDecisions++;
            }
            if (gameState.coreGameParameters.competitionMode && action != null && !observedActions.contains(action)) {
                System.out.printf("Action played that was not in the list of available actions: %s%n", action.getString(gameState));
                action = null;
            }
            // We publish an ACTION_CHOSEN message before we implement the action, so that observers can record the state that led to the decision
            AbstractAction finalAction = action;
            listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ACTION_CHOSEN, gameState, finalAction, activePlayer)));

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
            action = forwardModel.disqualifyOrRandomAction(gameState.coreGameParameters.disqualifyPlayerOnTimeout, gameState);
        } else {
            // Resolve action and game rules, time it
            s = System.nanoTime();
            forwardModel.next(gameState, action);
            nextTime = (System.nanoTime() - s);
        }

        lastPlayer = activePlayer;

        // We publish an ACTION_TAKEN message once the action is taken so that observers can record the result of the action
        // (such as the next player)
        AbstractAction finalAction1 = action;
        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ACTION_TAKEN, gameState, finalAction1.copy(), activePlayer)));

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

        // Perform any end of game computations as required by the game
        forwardModel.endGame(gameState);
        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, gameState)));
        if (gameState.coreGameParameters.recordEventHistory) {
            gameState.recordHistory(Event.GameEvent.GAME_OVER.name());
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                gameState.recordHistory(String.format("Player %d finishes at position %d with score: %.0f", i, gameState.getOrdinalPosition(i), gameState.getGameScore(i)));
            }
        }
        if (gameState.coreGameParameters.verbose) {
            System.out.println("Game Over");
        }

        // Allow players to terminate
        for (AbstractPlayer player : players) {
            player.finalizePlayer(gameState.copy(player.getPlayerID()));
        }
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
        //  System.out.printf("Average copy time was %.3f microseconsds%n", copyTime / 1e3);
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

    public void addListener(IGameListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            gameState.addListener(listener);
            listener.setGame(this);
        }
    }

    public List<IGameListener> getListeners() {
        return listeners;
    }

    public void clearListeners() {
        listeners.clear();
        getGameState().clearListeners();
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

    /**
     * The recommended way to run a game is via evaluations.Frontend, however that may not work on
     * some games for some screen sizes due to the vagaries of Java Swing...
     * <p>
     * Test class used to run a specific game. The user must specify:
     * 1. Action controller for GUI interactions / null for no visuals
     * 2. Random seed for the game
     * 3. Players for the game
     * 4. Game parameter configuration
     * 5. Mode of running
     * and then run this class.
     */
    public static void main(String[] args) {
        String gameType = Utils.getArg(args, "game", "Pandemic");
        boolean useGUI = Utils.getArg(args, "gui", true);
        int turnPause = Utils.getArg(args, "turnPause", 0);
        long seed = Utils.getArg(args, "seed", System.currentTimeMillis());
        ActionController ac = new ActionController();

        /* Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new BasicMCTSPlayer());

//        RMHCParams params = new RMHCParams();
//        params.horizon = 15;
//        params.discountFactor = 0.99;
//        params.heuristic = AbstractGameState::getHeuristicScore;
//        AbstractPlayer rmhcPlayer = new RMHCPlayer(params);
//        players.add(rmhcPlayer);

//        MCTSParams params = new MCTSParams();
//        players.add(new MCTSPlayer(params));

//        players.add(new OSLAPlayer());
//        players.add(new RMHCPlayer());
//        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());
//        players.add(new FirstActionPlayer());

        /* Game parameter configuration. Set to null to ignore and use default parameters */
        String gameParams = null;

        /* Run! */
        runOne(GameType.valueOf(gameType), gameParams, players, seed, false, null, useGUI ? ac : null, turnPause);

        /* Run multiple games */
//        ArrayList<GameType> games = new ArrayList<>();
//        games.add(Connect4);
//        runMany(games, players, 100L, 5, false, false, null, turnPause);
//        runMany(new ArrayList<GameType>() {{add(Uno);}}, players, 100L, 100, false, false, null, turnPause);
    }

}
