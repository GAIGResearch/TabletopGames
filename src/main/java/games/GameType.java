package games;

import core.*;
import games.coltexpress.ColtExpressForwardModel;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
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
import gui.PrototypeGUI;
import players.ActionController;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.*;
import static games.GameType.Category.*;
import static games.GameType.Mechanic.*;

/**
 * Encapsulates all games available in the framework, with minimum and maximum number of players as per game rules.
 * All games further include a list of categories and mechanics, which can be used to filter the game collection.
 */
public enum GameType {

    /**
     * Each game in the framework corresponds to a enum value here, giving minimum players, maximum players,
     * a list of categories the game belongs to, and a list of mechanics the game uses.
     * Add here all games, planned or implemented.
     */
    Pandemic(2, 4,
            new ArrayList<Category>() {{ add(Strategy); add(Medical); }},
            new ArrayList<Mechanic>() {{ add(ActionPoints); add(Cooperative); add(HandManagement);
            add(PointToPointMovement); add(SetCollection); add(Trading); add(VariablePlayerPowers); }}),
    TicTacToe (2, 2,
            new ArrayList<Category>() {{ add(Simple); add(Abstract); }},
            new ArrayList<Mechanic>() {{ add(PatternBuilding); }}),
    ExplodingKittens (2, 5,
            new ArrayList<Category>() {{ add(Strategy); add(Animals); add(Cards); add(ComicBook); add(Humour); }},
            new ArrayList<Mechanic>() {{ add(HandManagement); add(HotPotato); add(PlayerElimination); add(PushYourLuck);
            add(SetCollection); add(TakeThat); }}),
    LoveLetter (2, 4,
            new ArrayList<Category>() {{ add(Cards); add(Deduction); add(Renaissance); }},
            new ArrayList<Mechanic>() {{ add(HandManagement); add(PlayerElimination); }}),
    Uno (2, 10,
            new ArrayList<Category>() {{ add(Cards); add(ComicBook); add(Number); add(MoviesTVRadio); }},
            new ArrayList<Mechanic>() {{ add(HandManagement); add(LoseATurn); add(TakeThat); }}),
    Virus (2, 6,
            new ArrayList<Category>() {{ add(Cards); add(Medical); }},
            new ArrayList<Mechanic>() {{ add(CardDrafting); add(SetCollection); add(TakeThat); }}),
    ColtExpress (2, 6,
            new ArrayList<Category>() {{ add(Strategy); add(AmericanWest); add(Fighting); add(Trains); }},
            new ArrayList<Mechanic>() {{ add(ActionQueue); add(HandManagement); add(Memory); add(ProgrammedEvent);
            add(SimultaneousActionSelection); add(TakeThat); add(VariablePlayerPowers); }});
//    Carcassonne (2, 5,
//            new ArrayList<Category>() {{ add(Strategy); add(CityBuilding); add(Medieval); add(TerritoryBuilding); }},
//            new ArrayList<Mechanic>() {{ add(Influence); add(MapAddition); add(TilePlacement); }}),

    /**
     * Converts a given string to the enum type corresponding to the game.
     * Add here all games, planned or implemented.
     * @param game - string of a game type
     * @return - GameType corresponding to String
     */
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
        }
        System.out.println("Game type not found, returning null. ");
        return null;
    }

    /**
     * Creates an instance of the given game type, with a specific number of players and game seed.
     * Add here all games implemented.
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
            case ColtExpress:
                params = new ColtExpressParameters(seed);
                forwardModel = new ColtExpressForwardModel();
                gameState = new ColtExpressGameState(params, nPlayers);
                break;
        }

        return new Game(this, forwardModel, gameState);
    }

    /**
     * Creates a graphical user interface for the given game type. Add here all games with a GUI available.
     * @param gameState - initial game state from the game.
     * @param ac - ActionController object allowing for user interaction with the GUI.
     * @return - GUI for the given game type.
     */
    public AbstractGUI createGUI(AbstractGameState gameState, ActionController ac) {

        AbstractGUI gui = null;

        switch(this) {
            case Pandemic:
                gui = new PandemicGUI(gameState, ac);
                break;
//            case ExplodingKittens:
//                if (gameState != null) {
//                    gui = new PrototypeGUI(this, gameState, ac, 500);
//                } else {
//                    gui = new PrototypeGUI(this,null, ac, 0);
//                }
        }

        return gui;
    }


    // Minimum and maximum number of players supported in this game
    private int minPlayers, maxPlayers;

    // boardgamegeek.com topic classification of games
    private ArrayList<Category> categories;
    private ArrayList<Mechanic> mechanics;

    public enum Category {
        Strategy,
        Simple,
        Abstract,
        Animals,
        Cards,
        ComicBook,
        Humour,
        Medical,
        Deduction,
        Renaissance,
        MoviesTVRadio,
        Number,
        AmericanWest,
        Fighting,
        Trains,
        CityBuilding,
        Medieval,
        TerritoryBuilding,
        Adventure,
        Exploration,
        Fantasy,
        Miniatures;

        /**
         * Retrieves a list of all games within this category.
         * @return - list of game types.
         */
        public List<GameType> getAllGames() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt: GameType.values()) {
                if (gt.getCategories().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }

        /**
         * Retrieves a list of all games that are NOT within this category.
         * @return - list of game types.
         */
        public List<GameType> getAllGamesExcluding() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt: GameType.values()) {
                if (!gt.getCategories().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }
    }

    public enum Mechanic {
        Cooperative,
        ActionPoints,
        HandManagement,
        PointToPointMovement,
        SetCollection,
        Trading,
        VariablePlayerPowers,
        HotPotato,
        PlayerElimination,
        PushYourLuck,
        TakeThat,
        LoseATurn,
        CardDrafting,
        ActionQueue,
        Memory,
        SimultaneousActionSelection,
        ProgrammedEvent,
        Influence,
        MapAddition,
        TilePlacement,
        PatternBuilding,
        GameMaster,
        DiceRolling,
        GridMovement,
        LineOfSight,
        ModularBoard,
        MovementPoints,
        MultipleMaps,
        Campaign;

        /**
         * Retrieves a list of all games using this mechanic.
         * @return - list of game types.
         */
        public List<GameType> getAllGames() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt: GameType.values()) {
                if (gt.getMechanics().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }

        /**
         * Retrieves a list of all games that do NOT use this mechanic.
         * @return - list of game types.
         */
        public List<GameType> getAllGamesExcluding() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt: GameType.values()) {
                if (!gt.getMechanics().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }
    }

    GameType(int minPlayers, int maxPlayers, ArrayList<Category> categories, ArrayList<Mechanic> mechanics) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.categories = categories;
        this.mechanics = mechanics;
    }

    // Getters
    public int getMinPlayers() {
        return minPlayers;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public ArrayList<Category> getCategories() {
        return categories;
    }
    public ArrayList<Mechanic> getMechanics() {
        return mechanics;
    }

    /**
     * Creates an instance of the given game type with nPlayers number of players and a new random seed.
     * @param nPlayers - number of players for the game.
     * @return - instance of Game object; null if game not implemented.
     */
    public Game createGameInstance(int nPlayers) {
        return createGameInstance(nPlayers, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        boolean implemented = createGameInstance(minPlayers) != null;
        AbstractGUI g = createGUI(null, null);
        boolean gui = g != null;
        boolean prototypeGUI = g instanceof PrototypeGUI;
        return (gui? prototypeGUI? ANSI_CYAN : ANSI_BLUE : implemented? ANSI_GREEN : ANSI_RED) + this.name() + ANSI_RESET + " {" +
                "\n\tminPlayers = " + minPlayers +
                "\n\tmaxPlayers = " + maxPlayers +
                "\n\tcategories = " + categories +
                "\n\tmechanics = " + mechanics +
                (implemented? ANSI_GREEN: ANSI_RED) +
                "\n\timplemented = " + implemented + ANSI_RESET +
                (gui? prototypeGUI? ANSI_CYAN : ANSI_BLUE : ANSI_RED) +
                "\n\tGUI = " + gui + ANSI_RESET +
                "\n}\n";
    }

    public static void main(String[] args) {
        System.out.println("Games available in the framework: \n");
        for (GameType gt: GameType.values()) {
            System.out.println(gt.toString());
        }
    }
}
