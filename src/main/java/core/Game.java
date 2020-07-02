package core;

import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.GameType;
import players.*;
import utilities.StatSummary;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.PARTIAL_OBSERVABLE;
import static core.CoreConstants.VERBOSE;
import static games.GameType.*;

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

    /**
     * Game constructor. Receives a list of players, a forward model and a game state. Sets unique and final
     * IDs to all players in the game, and performs initialisation of the game state and forward model objects.
     * @param players - players taking part in this game.
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
     * @param model - forward model used to apply game rules.
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
    }

    /**
     * Resets the game. Sets up the game state to the initial state as described by game rules,
     * and initialises all players.
     * @param newRandomSeed - random seed is updated in the game parameters object and used throughout the game.
     */
    public final void reset(long newRandomSeed) {
        gameState.reset(newRandomSeed);
        forwardModel.abstractSetup(gameState);
        if (players != null) {
            for (AbstractPlayer player : players) {
                AbstractGameState observation = gameState.copy(player.getPlayerID());
                player.initializePlayer(observation);
            }
        }
    }

    /**
     * Resets the game. Sets up the game state to the initial state as described by game rules, assigns players
     * and their IDs, and initialises all players.
     * @param players - new players for the game
     */
    public final void reset(List<AbstractPlayer> players) {
        gameState.reset();
        forwardModel.abstractSetup(gameState);
        this.players = players;
        int id = 0;
        for (AbstractPlayer player: players) {
            // Create a FM copy for this player (different random seed)
            player.forwardModel = this.forwardModel.copy();
            // Create initial state observation
            AbstractGameState observation = gameState.copy(id);
            // Give player their ID
            player.playerID = id++;
            // Allow player to initialize

            player.initializePlayer(observation);
        }
    }

    /**
     * Resets the game. Sets up the game state to the initial state as described by game rules, assigns players
     * and their IDs, and initialises all players.
     * @param players - new players for the game
     * @param newRandomSeed - random seed is updated in the game parameters object and used throughout the game.
     */
    public final void reset(List<AbstractPlayer> players, long newRandomSeed) {
        gameState.reset(newRandomSeed);
        forwardModel.abstractSetup(gameState);
        this.players = players;
        int id = 0;
        for (AbstractPlayer player: players) {
            // Create a FM copy for this player (different random seed)
            player.forwardModel = this.forwardModel.copy();
            // Create initial state observation
            AbstractGameState observation = gameState.copy(id);
            // Give player their ID
            player.playerID = id++;
            // Allow player to initialize

            player.initializePlayer(observation);
        }
    }

    /**
     * Runs the game, given a GUI. If this is null, the game runs automatically without visuals.
     * @param gui - graphical user interface.
     */
    public final void run(AbstractGUI gui) {

        boolean firstEnd = true;

        while (gameState.isNotTerminal() || gui != null && gui.isWindowOpen()){
            if (gui != null && !gui.isWindowOpen()) {
                // Playing with GUI and closed window
                terminate();
                break;
            }

            // Get player to ask for actions next
            int activePlayer = gameState.getCurrentPlayer();
            currentPlayer = players.get(activePlayer);

            // GUI update
            updateGUI(gui);

            if (gameState.isNotTerminal()) {
                if (VERBOSE) {
                    System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());
                }

                // Get actions for the player
                List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
                AbstractGameState observation = gameState.copy(activePlayer);
                if (observation instanceof IPrintable && VERBOSE) {
                    ((IPrintable) observation).printToConsole();
                }

                // Either ask player which action to use or, in case no actions are available, report the updated observation
                AbstractAction action = null;
                if (actions.size() > 0) {
                    if (actions.size() == 1) {
                        // Can only do 1 action, so do it.
                        action = actions.get(0);
                        currentPlayer.registerUpdatedObservation(observation);

                    } else {
                        if (currentPlayer instanceof HumanGUIPlayer && gui != null) {
                            while (action == null && gui.isWindowOpen()) {
                                action = currentPlayer.getAction(observation);
                                updateGUI(gui);
                            }
                        } else {
                            action = currentPlayer.getAction(observation);
                        }
                    }
                } else {
                    currentPlayer.registerUpdatedObservation(observation);
                }

                // Resolve action and game rules
                forwardModel.next(gameState, action);
            } else {
                if (firstEnd) {
                    System.out.println("Ended");
                    terminate();
                    firstEnd = false;
                }
            }
        }

        if (gui == null) {
            terminate();
        }
    }

    /**
     * Performs GUI update.
     * @param gui - gui to update.
     */
    private void updateGUI(AbstractGUI gui) {
        if (gui != null) {
            if (PARTIAL_OBSERVABLE) {
                // Copying again to get the player's observation, in case player modifies the object received directly.
                gui.update(currentPlayer, gameState.copy(currentPlayer.getPlayerID()));
            } else {
                gui.update(currentPlayer, gameState);
            }
            try {
                Thread.sleep(100);
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
        if (VERBOSE) {
            System.out.println("Game Over");
        }

        // Allow players to terminate
        for (AbstractPlayer player: players) {
            player.finalizePlayer(gameState.copy(player.getPlayerID()));
        }
    }

    /**
     * Retrieves the current game state.
     * @return - current game state.
     */
    public final AbstractGameState getGameState() {
        return gameState;
    }

    /**
     * Which game is this?
     * @return type of game.
     */
    public GameType getGameType() {
        return gameType;
    }

    @Override
    public String toString() {
        return gameType.toString();
    }


    /**
     * Runs one game.
     * @param gameToPlay - game to play
     * @param players - list of players for the game
     * @param seed - random seed for the game
     * @param ac - Action Controller object allowing GUI interaction. If null, runs without visuals.
     * @param randomizeParameters - if true, parameters are randomized for each run of each game (if possible).
     * @return - game instance created for the run
     */
    private static Game runOne(GameType gameToPlay, List<AbstractPlayer> players, long seed, ActionController ac,
                               boolean randomizeParameters) {
        // Creating game instance (null if not implemented)
        Game game = gameToPlay.createGameInstance(players.size(), seed);
        if (game != null) {

            // Randomize parameters
            if (randomizeParameters) {
                AbstractGameParameters gameParameters = game.getGameState().getGameParameters();
                gameParameters.randomize();
            }

            // Reset game instance, passing the players for this game
            game.reset(players);

            AbstractGUI gui = null;
            if (ac != null) {
                // Create GUI (null if not implemented; running without visuals)
                gui = gameToPlay.createGUI(game.getGameState(), ac);
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
     * @param gamesToPlay - list of games to play.
     * @param players - list of players for the game.
     * @param nRepetitions - number of repetitions of each game.
     * @param seed - random seed for all games. If null, a new random seed is used for each game.
     * @param ac - action controller for GUI interactions, null if playing without visuals.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     * @param detailedStatistics - if true, detailed statistics are printed, otherwise just average of wins
     */
    private static void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, Long seed,
                                int nRepetitions, ActionController ac, boolean randomizeParameters,
                                boolean detailedStatistics) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        StatSummary[] overall = new StatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            overall[i] = new StatSummary("Overall Player " + i);
        }

        // For each game...
        for (GameType gt: gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            StatSummary[] statSummaries = new StatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new StatSummary("{Game: " + gt.name() + "; Player: " + i + "}");
            }

            // Play n repetitions of this game and record player results
            Game game = null;
            int offset = 0;
            for (int i = 0; i < nRepetitions; i++) {
                Long s = seed;
                if (s == null) s = System.currentTimeMillis();
                s += offset;
                game = runOne(gt, players, s, ac, randomizeParameters);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                    offset = game.getGameState().getTurnOrder().getRoundCounter() * game.getGameState().getNPlayers();
                } else {
                    break;
                }
            }

            if (game != null) {
                for (int i = 0; i < nPlayers; i++) {
                    // Print statistics for this game
                    if (detailedStatistics) {
                        System.out.println(statSummaries[i].toString());
                    } else {
                        System.out.println(statSummaries[i].name + ": " + statSummaries[i].mean());
                    }

                    // Record in overall statistics
                    overall[i].add(statSummaries[i]);
                }
            }
        }

        // Print final statistics
        System.out.println("\n---------------------\n");
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
     * @param gamesToPlay - list of games to play.
     * @param players - list of players for the game.
     * @param nRepetitions - number of repetitions of each game.
     * @param seeds - random seeds array, one for each repetition of a game.
     * @param ac - action controller for GUI interactions, null if playing without visuals.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     */
    private static void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, int nRepetitions,
                                long[] seeds, ActionController ac, boolean randomizeParameters) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        StatSummary[] overall = new StatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            overall[i] = new StatSummary("Overall Player " + i);
        }

        // For each game...
        for (GameType gt: gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            StatSummary[] statSummaries = new StatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new StatSummary("Game: " + gt.name() + "; Player: " + i);
            }

            // Play n repetitions of this game and record player results
            for (int i = 0; i < nRepetitions; i++) {
                Game game = runOne(gt, players, seeds[i], ac, randomizeParameters);
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
     * @param statSummaries - object recording statistics
     * @param game - finished game
     */
    private static void recordPlayerResults(StatSummary[] statSummaries, Game game) {
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
     *      1. Action controller for GUI interactions / null for no visuals
     *      2. Random seed for the game
     *      3. Players for the game
     *      4. Mode of running
     * and then run this class.
     */
    public static void main(String[] args) {
        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
        ActionController ac = new ActionController(); //null;

        /* 2. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 3. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();
//        players.add(new OSLA());
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new OSLA());
//        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());

        /* 4. Run! */
//        runOne(TicTacToe, players, seed, ac, false);
//        runMany(GameType.Category.Strategy.getAllGames(), players, null, 50, null, false);

//        ArrayList<GameType> games = new ArrayList<>(Arrays.asList(GameType.values()));
//        games.remove(Pandemic);
//        games.remove(TicTacToe);
//        runMany(games, players, null, 50, null, false, false);
        runMany(new ArrayList<GameType>() {{add(ColtExpress);}}, players, null, 50, null, false, false);
    }
}
