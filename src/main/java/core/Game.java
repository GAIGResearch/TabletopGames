package core;

import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.GameType;
import players.ActionController;
import players.HumanGUIPlayer;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static games.GameType.*;

public class Game {

    // Type of game
    private GameType gameType;

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;

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

        while (gameState.isNotTerminal()){
            if (CoreConstants.VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            AbstractPlayer player = players.get(activePlayer);

            // Get actions for the player
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            AbstractGameState observation = gameState.copy(activePlayer);
            if (observation instanceof IPrintable && CoreConstants.VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            // Either ask player which action to use or, in case no actions are available, report the updated observation
            AbstractAction action = null;
            if (actions.size() > 0) {
                if (actions.size() == 1) {
                    // Can only do 1 action, so do it.
                    action = actions.get(0);
                } else {
                    if (player instanceof HumanGUIPlayer) {
                        while (action == null) {
                            action = getPlayerAction(gui, player, observation);
                        }
                    } else {
                        action = getPlayerAction(gui, player, observation);
                    }
                }

                // Resolve action and game rules
                forwardModel.next(gameState, action);
            } else {
                player.registerUpdatedObservation(observation);
            }
        }

        // Print last state
        if (gameState instanceof IPrintable && CoreConstants.VERBOSE) {
            ((IPrintable) gameState).printToConsole();
        }

        // Perform any end of game computations as required by the game
        forwardModel.endGame(gameState);
        System.out.println("Game Over");

        // Allow players to terminate
        for (AbstractPlayer player: players) {
            player.finalizePlayer(gameState.copy(player.getPlayerID()));
        }
    }

    /**
     * Queries the player for an action, after performing of GUI update (if running with visuals).
     * @param gui - graphical user interface.
     * @param player - player being asked for an action.
     * @param observation - observation the player receives from the current game state.
     * @return - int, index of action chosen by player (from the list of actions).
     */
    private AbstractAction getPlayerAction(AbstractGUI gui, AbstractPlayer player, AbstractGameState observation) {
        if (gui != null) {
            gui.update(player, gameState);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println("EXCEPTION " + e);
            }
        }

        return player.getAction(observation);
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
     * Main class used to run the framework. The user must specify:
     *      1. Game type to play
     *      2. Visuals on/off
     *      3. Random seed for the game
     *      4. Players for the game
     * and then run this class.
     */
    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) {
        // Action controller for GUI interactions
        ActionController ac = new ActionController();

        /* 1. Choose game to play */
        GameType gameToPlay = TicTacToe;
//        List<GameType> games = GameType.Mechanic.Cooperative.getAllGames();

        /* 2. Running with visuals? */
        boolean visuals = false;

        /* 3. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 4. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
//        players.add(new RandomPlayer(new Random()));
//        players.add(new OSLA());
//        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());

        // Creating game instance (null if not implemented)
        Game game = gameToPlay.createGameInstance(players.size(), seed);
        if (game != null) {
            AbstractGUI gui = null;

            // Randomize parameters
//            AbstractGameParameters gameParameters = game.getGameState().getGameParameters();
//            gameParameters.randomize();

            // Reset game instance, passing the players for this game
            game.reset(players);

            if (visuals) {
                // Create GUI (null if not implemented; running without visuals)
                gui = gameToPlay.createGUI(game.getGameState(), ac);
            }

            // Run!
            game.run(gui);
        } else {
            System.out.println("Game " + gameToPlay + " not yet implemented!");
        }
    }
}
