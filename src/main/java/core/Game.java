package core;

import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import core.interfaces.IPrintable;
import core.turnorders.ReactiveTurnOrder;
import games.GameType;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSParams;
import players.simple.RandomPlayer;
import utilities.Pair;
import utilities.TAGStatSummary;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static core.CoreConstants.*;
import static games.GameType.DotsAndBoxes;

public class Game {

    // Type of game
    private GameType gameType;

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;
    // Current player acting
    AbstractPlayer currentPlayer;

    // Real game state and forward model
    protected AbstractGameState gameState;
    protected AbstractForwardModel forwardModel;
    protected List<IGameListener> listeners = new ArrayList<>();

    /* Game Statistics */

    // Timers for various function calls
    private double nextTime, copyTime, agentTime, actionComputeTime;
    // Keeps track of action spaces for each game tick, pairs of (player ID, #actions)
    private ArrayList<Pair<Integer, Integer>> actionSpaceSize;
    // Game tick, number of iterations of game loop
    private int tick;
    // Number of times an agent is asked for decisions
    private int nDecisions;
    // Number of actions taken in a turn by a player
    private int nActionsPerTurn, nActionsPerTurnSum, nActionsPerTurnCount;

    private static final AtomicInteger idFountain = new AtomicInteger(0);
    private int gameID;

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
        reset();
    }

    /**
     * Resets the game. Sets up the game state to the initial state as described by game rules,
     * and initialises all players.
     */
    public final void reset() {
        gameState.reset();
        forwardModel.abstractSetup(gameState);
        if (players != null) {
            for (AbstractPlayer player : players) {
                AbstractGameState observation = gameState.copy(player.getPlayerID());
                player.initializePlayer(observation);
            }
        }
        resetStats();
    }

    /**
     * Resets the game. Sets up the game state to the initial state as described by game rules, assigns players
     * and their IDs, and initialises all players.
     *
     * @param players - new players for the game
     */
    public final void reset(List<AbstractPlayer> players) {
        gameState.reset();
        forwardModel.abstractSetup(gameState);
        this.players = players;
        int id = 0;
        for (AbstractPlayer player : players) {
            // Create a FM copy for this player (different random seed)
            player.forwardModel = this.forwardModel.copy();
            // Create initial state observation
            AbstractGameState observation = gameState.copy(id);
            // Give player their ID
            player.playerID = id++;
            // Allow player to initialize

            player.initializePlayer(observation);
        }
        gameID = idFountain.incrementAndGet();
        gameState.setGameID(gameID);

        resetStats();
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
        this.players = players;
        int id = 0;
        for (AbstractPlayer player : players) {
            // Create a FM copy for this player (different random seed)
            player.forwardModel = this.forwardModel.copy();
            // Create initial state observation
            AbstractGameState observation = gameState.copy(id);
            // Give player their ID
            player.playerID = id++;
            // Allow player to initialize

            player.initializePlayer(observation);
        }
        gameID = idFountain.incrementAndGet();
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
        tick = 0;
        nDecisions = 0;
        actionSpaceSize = new ArrayList<>();
        nActionsPerTurnSum = 0;
        nActionsPerTurn = 1;
        nActionsPerTurnCount = 0;
    }

    /**
     * Runs the game, given a GUI. If this is null, the game runs automatically without visuals.
     *
     * @param gui - graphical user interface.
     */
    public final void run(AbstractGUI gui) {

        boolean firstEnd = true;

        while (gameState.isNotTerminal() || gui != null && gui.isWindowOpen()) {
            if (gui != null && !gui.isWindowOpen()) {
                // Playing with GUI and closed window
                terminate();
                break;
            }

            // Get player to ask for actions next
            boolean reacting = (gameState.getTurnOrder() instanceof ReactiveTurnOrder
                    && ((ReactiveTurnOrder) gameState.getTurnOrder()).getReactivePlayers().size() > 0);
            int activePlayer = gameState.getCurrentPlayer();

            // Check if this is the same player as last, count number of actions per turn
            if (!reacting) {
                if (currentPlayer != null && activePlayer == currentPlayer.getPlayerID()) {
                    nActionsPerTurn++;
                } else {
                    nActionsPerTurnSum += nActionsPerTurn;
                    nActionsPerTurn = 1;
                    nActionsPerTurnCount++;
                }
            }

            // This is the next player to be asked for a decision
            currentPlayer = players.get(activePlayer);

            // Get player observation, and time how long it takes
            double s = System.nanoTime();
            AbstractGameState observation = gameState.copy(activePlayer);
            copyTime += (System.nanoTime() - s);

            // Get actions for the player
            s = System.nanoTime();
            List<AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);
            actionComputeTime += (System.nanoTime() - s);
            actionSpaceSize.add(new Pair<>(activePlayer, observedActions.size()));

            // GUI update
            updateGUI(gui);

            if (gameState.isNotTerminal()) {
                if (VERBOSE) {
                    System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());
                }

                if (observation instanceof IPrintable && VERBOSE) {
                    ((IPrintable) observation).printToConsole();
                }

                // Either ask player which action to use or, in case no actions are available, report the updated observation
                AbstractAction action = null;
                if (observedActions.size() > 0) {
                    if (observedActions.size() == 1 && !(currentPlayer instanceof HumanGUIPlayer)) {
                        // Can only do 1 action, so do it.
                        action = observedActions.get(0);
                        currentPlayer.registerUpdatedObservation(observation);
                    } else {
                        if (currentPlayer instanceof HumanGUIPlayer && gui != null) {
                            while (action == null && gui.isWindowOpen()) {
                                action = currentPlayer.getAction(observation, observedActions);
                                updateGUI(gui);
                            }
                        } else {
                            // Get action from player, and time it
                            s = System.nanoTime();
                            action = currentPlayer.getAction(observation, observedActions);
                            agentTime += (System.nanoTime() - s);
                            nDecisions++;
                        }
                    }
                    if (COMPETITION_MODE && !observedActions.contains(action)) {
                        System.out.printf("Action played that was not in the list of available actions: %s%n", action.getString(gameState));
                        action = null;
                    }
                    AbstractAction finalAction = action;
                    listeners.forEach(l -> l.onEvent(GameEvents.ACTION_CHOSEN, gameState, finalAction));
                } else {
                    currentPlayer.registerUpdatedObservation(observation);
                }

                if (VERBOSE) {
                    if (action != null) {
                        System.out.println(action.toString());
                    } else {
                        System.out.println("NULL action (player " + activePlayer + ")");
                    }
                }

                // Resolve action and game rules, time it
                s = System.nanoTime();
                forwardModel.next(gameState, action);
                nextTime += (System.nanoTime() - s);
            } else {
                if (firstEnd) {
                    if (VERBOSE) {
                        System.out.println("Ended");
                    }
                    terminate();
                    firstEnd = false;
                }
            }
            tick++;
        }

        if (gui == null) {
            if (VERBOSE) {
                System.out.println("Ended");
            }
            terminate();
        }
    }

    // Run function shortcut
    public final void run() {
        run(null);
    }

    /**
     * Performs GUI update.
     *
     * @param gui - gui to update.
     */
    private void updateGUI(AbstractGUI gui) {
        if (gui != null) {
            gui.update(currentPlayer, gameState);
            try {
                Thread.sleep(FRAME_SLEEP_MS);
            } catch (Exception e) {
                System.out.println("EXCEPTION " + e);
            }
        }
    }

    /**
     * Called at the end of game loop execution, when the game is over.
     */
    private void terminate() {
        // Print last state
        if (gameState instanceof IPrintable && VERBOSE) {
            ((IPrintable) gameState).printToConsole();
        }

        // Perform any end of game computations as required by the game
        forwardModel.endGame(gameState);
        listeners.forEach(l -> l.onEvent(GameEvents.GAME_OVER, gameState, null));
        if (VERBOSE) {
            System.out.println("Game Over");
        }

        // Allow players to terminate
        for (AbstractPlayer player : players) {
            player.finalizePlayer(gameState.copy(player.getPlayerID()));
        }

        // Timers should average
        terminateTimers();
    }

    /**
     * Timers average at the end of the game.
     */
    private void terminateTimers() {
        nextTime /= tick;
        copyTime /= tick;
        actionComputeTime /= tick;
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
        return tick;
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
            gameState.turnOrder.addListener(listener);
        }
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

    @Override
    public String toString() {
        return gameType.toString();
    }


    /**
     * Runs one game.
     *
     * @param gameToPlay          - game to play
     * @param players             - list of players for the game
     * @param seed                - random seed for the game
     * @param ac                  - Action Controller object allowing GUI interaction. If null, runs without visuals.
     * @param randomizeParameters - if true, parameters are randomized for each run of each game (if possible).
     * @return - game instance created for the run
     */
    public static Game runOne(GameType gameToPlay, List<AbstractPlayer> players, long seed, ActionController ac,
                              boolean randomizeParameters, List<IGameListener> listeners) {
        // Creating game instance (null if not implemented)
        Game game = gameToPlay.createGameInstance(players.size(), seed);
        if (game != null) {
            if (listeners != null)
                listeners.forEach(game::addListener);

            // Randomize parameters
            if (randomizeParameters) {
                AbstractParameters gameParameters = game.getGameState().getGameParameters();
                gameParameters.randomize();
            }

            // Reset game instance, passing the players for this game
            game.reset(players);

            AbstractGUI gui = null;
            if (ac != null) {
                // Create GUI (null if not implemented; running without visuals)
                gui = gameToPlay.createGUI(game, ac);
            }

            // Run!
            game.run(gui);
        } else {
            System.out.println("Error game: " + gameToPlay);
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
     * @param ac                  - action controller for GUI interactions, null if playing without visuals.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     * @param detailedStatistics  - if true, detailed statistics are printed, otherwise just average of wins
     */
    public static void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, Long seed,
                               int nRepetitions, ActionController ac, boolean randomizeParameters,
                               boolean detailedStatistics, List<IGameListener> listeners) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGStatSummary[] overall = new TAGStatSummary[nPlayers];
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            String[] split = players.get(i).getClass().toString().split("\\.");
            String agentName = split[split.length - 1] + "-" + i;
            overall[i] = new TAGStatSummary("Overall " + agentName);
            agentNames[i] = agentName;
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGStatSummary[] statSummaries = new TAGStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGStatSummary("{Game: " + gt.name() + "; Player: " + agentNames[i] + "}");
            }

            // Play n repetitions of this game and record player results
            Game game = null;
            int offset = 0;
            for (int i = 0; i < nRepetitions; i++) {
                Long s = seed;
                if (s == null) s = System.currentTimeMillis();
                s += offset;
                game = runOne(gt, players, s, ac, randomizeParameters, listeners);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                    offset = game.getGameState().getTurnOrder().getRoundCounter() * game.getGameState().getNPlayers();
                } else {
                    break;
                }
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
                               long[] seeds, ActionController ac, boolean randomizeParameters, List<IGameListener> listeners) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGStatSummary[] overall = new TAGStatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            overall[i] = new TAGStatSummary("Overall Player " + i);
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGStatSummary[] statSummaries = new TAGStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGStatSummary("Game: " + gt.name() + "; Player: " + i);
            }

            // Play n repetitions of this game and record player results
            for (int i = 0; i < nRepetitions; i++) {
                Game game = runOne(gt, players, seeds[i], ac, randomizeParameters, listeners);
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
    public static void recordPlayerResults(TAGStatSummary[] statSummaries, Game game) {
        int nPlayers = statSummaries.length;
        Utils.GameResult[] results = game.getGameState().getPlayerResults();
        for (int p = 0; p < nPlayers; p++) {
            if (results[p] == Utils.GameResult.WIN || results[p] == Utils.GameResult.LOSE || results[p] == Utils.GameResult.DRAW) {
                statSummaries[p].add(results[p].value);
            }
        }
    }

    /**
     * Main class used to run the framework. The user must specify:
     * 1. Action controller for GUI interactions / null for no visuals
     * 2. Random seed for the game
     * 3. Players for the game
     * 4. Mode of running
     * and then run this class.
     */
    public static void main(String[] args) {
        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
        ActionController ac = new ActionController(); //null;

        /* 2. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 3. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();

        MCTSParams params1 = new MCTSParams();

        players.add(new RandomPlayer());
//        players.add(new RMHCPlayer());
//        players.add(new MCTSPlayer(params1));
        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());

        /* 4. Run! */
        runOne(DotsAndBoxes, players, seed, ac, false, null);
        //       runMany(Collections.singletonList(Dominion), players, 100L,100, null, false, false, listeners);
//        ArrayList<GameType> games = new ArrayList<>();
//        games.add(TicTacToe);
//        games.add(ExplodingKittens);
//        games.add(LoveLetter);
//        runMany(games, players, null, 50, null, false, false);

//        ArrayList<GameType> games = new ArrayList<>(Arrays.asList(GameType.values()));
//        games.remove(LoveLetter);
//        games.remove(Pandemic);
//        games.remove(TicTacToe);
//        runMany(games, players, null, 100, ac, false, true);
//        runMany(new ArrayList<GameType>() {{add(Uno);}}, players, null, 1000, null, false, false);

    }
}
