package core;

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
import players.ActionController;

import java.util.ArrayList;
import java.util.List;

import static core.GameType.Topic.*;

/**
 * Encapsulates all games available in the framework, with minimum and maximum number of players as per game rules.
 */
public enum GameType {

    Pandemic(2, 4, new ArrayList<Topic>() {{ add(Strategy); }}),
    TicTacToe (2, 2, new ArrayList<Topic>() {{ add(Simple); }}),
    ExplodingKittens (2, 5, new ArrayList<Topic>() {{ add(Cards); }}),
    LoveLetter (2, 4, new ArrayList<Topic>() {{ add(Cards); }}),
    Uno (2, 10, new ArrayList<Topic>() {{ add(Cards); }}),
    Virus (2, 6, new ArrayList<Topic>() {{ add(Cards); }}),
    ColtExpress (2, 6, new ArrayList<Topic>() {{ add(Strategy); add(Planning); }}),
    Carcassonne (2, 5, new ArrayList<Topic>() {{ add(Strategy); }});

    private int minPlayers, maxPlayers;
    private ArrayList<Topic> topics;

    public enum Topic {
        Strategy,
        Simple,
        Cards,
        Planning;

        public List<GameType> getAllGamesOfTopic() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt: GameType.values()) {
                if (gt.getTopics().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }
    }

    GameType(int minPlayers, int maxPlayers, ArrayList<Topic> topics) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.topics = topics;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public ArrayList<Topic> getTopics() {
        return topics;
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

    /**
     * Creates an instance of the given game type with nPlayers number of players and a new random seed.
     * @param nPlayers - number of players for the game.
     * @return - instance of Game object; null if game not implemented.
     */
    public Game createGameInstance(int nPlayers) {
        return createGameInstance(nPlayers, System.currentTimeMillis());
    }

    /**
     * Creates an instance of the given game type, with a specific number of players and game seed.
     * @param nPlayers - number of players taking part in the game, used for initialisation.
     * @param seed - seed for this game.
     * @return - instance of Game object; null if game not implemented.
     */
    public Game createGameInstance(int nPlayers, long seed) {
        if (nPlayers < minPlayers || nPlayers > maxPlayers) {
            System.out.println("Unsupported number of players: " + nPlayers
                    + ". Should be in range [" + minPlayers + "," + maxPlayers + "].");
            return null;
        }

        AbstractGameParameters params;
        AbstractForwardModel forwardModel = null;
        AbstractGameState gameState = null;

        switch(this) {
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
                forwardModel = new ExplodingKittensForwardModel();
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
            return new Game(this, forwardModel, gameState);
        }
        return null;
    }

    /**
     * Creates a graphical user interface for the given game type.
     * @param gameState - initial game state from the game.
     * @param ac - ActionController object allowing for user interaction with the GUI.
     * @return - GUI for the given game type.
     */
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public AbstractGUI createGUI(AbstractGameState gameState, ActionController ac) {

        AbstractGUI gui = null;

        switch(this) {
            case Pandemic:
                gui = new PandemicGUI(gameState, ac);
                break;
        }

        return gui;
    }
}
