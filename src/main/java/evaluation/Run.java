package evaluation;

import core.*;
import games.explodingkittens.ExplodingKittenParameters;
import games.explodingkittens.ExplodingKittensForwardModel;
import games.explodingkittens.ExplodingKittensGameState;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.gui.PandemicGUI;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import games.uno.UnoForwardModel;
import games.uno.UnoGameParameters;
import games.uno.UnoGameState;
import games.virus.VirusForwardModel;
import games.virus.VirusGameParameters;
import games.virus.VirusGameState;
import players.*;

import java.util.ArrayList;
import java.util.Random;

import static evaluation.Run.GameType.*;

/**
 * Main class used to run the framework. The user must specify:
 *      1. Game type to play
 *      2. Visuals on/off
 *      3. Random seed for the game
 *      4. Players for the game
 * and then run this class.
 */
public class Run {

    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) {
        // Action controller for GUI interactions
        ActionController ac = new ActionController();

        /* 1. Choose game to play */
        GameType gameToPlay = Virus;

        /* 2. Running with visuals? */
        boolean visuals = false;

        /* 3. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 4. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new OSLA());
//        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());

        // Creating game instance (null if not implemented)
        Game game = createGameInstance(gameToPlay, players.size(), seed);
        if (game != null) {
            AbstractGUI gui = null;

            // Reset game instance, passing the players for this game
            game.reset(players);

            if (visuals) {
                // Create GUI (null if not implemented; running without visuals)
                gui = createGUI(gameToPlay, game.getGameState(), ac);
            }

            // Run!
            game.run(gui);
        } else {
            System.out.println("Game " + gameToPlay + " not yet implemented!");
        }
    }

    /**
     * Encapsulates all games available in the framework, with minimum and maximum number of players as per game rules.
     */
    public enum GameType {
        Pandemic(2, 4),
        TicTacToe (2, 2),
        ExplodingKittens (2, 5),
        LoveLetter (2, 4),
        Uno (2, 10),
        Virus (2, 6),
        ColtExpress (2, 6),
        Carcassonne (2, 5);

        private int minPlayers, maxPlayers;

        GameType(int minPlayers, int maxPlayers) {
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
        }

        public int getMinPlayers() {
            return minPlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public GameType stringToGameType(String game) {
            switch (game.toLowerCase()) {
                case "pandemic":
                    return Pandemic;
                case "tictactoe":
                    return TicTacToe;
                case "explodingkittens":
                    return ExplodingKittens;
                case "loveletter":
                    return LoveLetter;
                case "uno":
                    return Uno;
                case "virus":
                    return Virus;
                case "coltexpress":
                    return ColtExpress;
                case "carcassonne":
                    return Carcassonne;
            }
            System.out.println("Game type not found, returning null. ");
            return null;
        }
    }

    /**
     * Creates an instance of the given game type with nPlayers number of players and a new random seed.
     * @param gameType - type of game to create.
     * @param nPlayers - number of players for the game.
     * @return - instance of Game object; null if game not implemented.
     */
    public static Game createGameInstance(GameType gameType, int nPlayers) {
        return createGameInstance(gameType, nPlayers, System.currentTimeMillis());
    }

    /**
     * Creates an instance of the given game type, with a specific number of players and game seed.
     * @param gameType - type of game to create.
     * @param nPlayers - number of players taking part in the game, used for initialisation.
     * @param seed - seed for this game.
     * @return - instance of Game object; null if game not implemented.
     */
    public static Game createGameInstance(GameType gameType, int nPlayers, long seed) {
        if (nPlayers < gameType.getMinPlayers() || nPlayers > gameType.getMaxPlayers()) {
            System.out.println("Unsupported number of players: " + nPlayers
                    + ". Should be in range [" + gameType.getMinPlayers() + "," + gameType.getMaxPlayers() + "].");
            return null;
        }

        AbstractGameParameters params;
        AbstractForwardModel forwardModel = null;
        AbstractGameState gameState = null;

        switch(gameType){
            case Pandemic:
                params = new PandemicParameters("data/pandemic/", seed);
                forwardModel = new PandemicForwardModel(params, nPlayers);
                gameState = new PandemicGameState(params, nPlayers);
                break;
            case TicTacToe:
                params = new TicTacToeGameParameters(seed);
                forwardModel = new TicTacToeForwardModel();
                gameState = new TicTacToeGameState(params, nPlayers);
                break;
            case ExplodingKittens:
                params = new ExplodingKittenParameters(seed);
                forwardModel = new ExplodingKittensForwardModel(params.getGameSeed());
                gameState = new ExplodingKittensGameState(params, nPlayers);
                break;
            case LoveLetter:
                params = new LoveLetterParameters(seed);
                forwardModel = new LoveLetterForwardModel();
                gameState = new LoveLetterGameState(params, nPlayers);
                break;
            case Uno:
                params = new UnoGameParameters(seed);
                forwardModel = new UnoForwardModel();
                gameState = new UnoGameState(params, nPlayers);
                break;
            case Virus:
                params = new VirusGameParameters(seed);
                forwardModel = new VirusForwardModel();
                gameState = new VirusGameState(params, nPlayers);
                break;
        }

        if (forwardModel != null) {
            return new Game(gameType, forwardModel, gameState);
        }
        return null;
    }

    /**
     * Creates a graphical user interface for the given game type.
     * @param gameType - game to create a GUI for.
     * @param gameState - initial game state from the game.
     * @param ac - ActionController object allowing for user interaction with the GUI.
     * @return - GUI for the given game type.
     */
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public static AbstractGUI createGUI(GameType gameType, AbstractGameState gameState, ActionController ac) {

        AbstractGUI gui = null;

        switch(gameType){
            case Pandemic:
                gui = new PandemicGUI(gameState, ac);
                break;
        }

        return gui;
    }

}
