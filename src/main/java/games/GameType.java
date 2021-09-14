package games;

import core.*;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import games.blackjack.gui.BlackjackGUIManager;
import games.catan.CatanForwardModel;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.gui.CatanGUI;
import games.coltexpress.ColtExpressForwardModel;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.gui.ColtExpressGUIManager;
import games.diamant.DiamantForwardModel;
import games.diamant.DiamantGameState;
import games.diamant.DiamantParameters;
import games.dominion.gui.DominionGUIManager;
import games.dotsboxes.DBForwardModel;
import games.dotsboxes.DBGUIManager;
import games.dotsboxes.DBGameState;
import games.dotsboxes.DBParameters;
import games.explodingkittens.ExplodingKittensParameters;
import games.explodingkittens.ExplodingKittensForwardModel;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.gui.ExplodingKittensGUIManager;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.gui.LoveLetterGUIManager;
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.gui.PandemicGUIManager;
import games.poker.PokerForwardModel;
import games.poker.PokerGameParameters;
import games.poker.PokerGameState;
import games.poker.gui.PokerGUIManager;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.gui.TicTacToeGUIManager;
import games.uno.UnoForwardModel;
import games.uno.UnoGameParameters;
import games.uno.UnoGameState;
import games.uno.gui.UnoGUIManager;
import games.virus.VirusForwardModel;
import games.virus.VirusGameParameters;
import games.virus.VirusGameState;
import games.dominion.*;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.PrototypeGUIManager;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import java.util.*;
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
            add(SimultaneousActionSelection); add(TakeThat); add(VariablePlayerPowers); }}),
    DotsAndBoxes(2, 6,
            new ArrayList<Category>() {{
                add(Simple);
                add(Abstract);
                add(TerritoryBuilding);
            }},
            new ArrayList<Mechanic>() {{
                add(Enclosure);
            }}),
    Poker(2, 14,
            new ArrayList<Category>() {{
                add(Cards);
                add(ComicBook);
                add(Number);
                add(MoviesTVRadio);
            }},

            new ArrayList<Mechanic>() {{
                add(HandManagement);
                add(LoseATurn);
                add(TakeThat);
            }}),
    Blackjack(2, 7,
            new ArrayList<games.GameType.Category>() {{
                add(Cards);
                add(ComicBook);
                add(Number);
                add(MoviesTVRadio);
            }},
            new ArrayList<games.GameType.Mechanic>() {{
                add(HandManagement);
                add(LoseATurn);
                add(TakeThat);
            }}),
    Diamant( 2, 6,
            new ArrayList<Category>() {{
                add(Adventure);
                add(Bluffing);
                add(Exploration);
            }},
            new ArrayList<Mechanic>() {{
                add(MoveThroughDeck);
                add(PushYourLuck);
                add(SimultaneousActionSelection);
            }}),
    Dominion (2, 4,
            new ArrayList<Category>() {{ add(Cards); add(Strategy);}},
            new ArrayList<Mechanic>() {{ add(DeckManagement); }}),
    DominionSizeDistortion (2, 4,
            new ArrayList<Category>() {{ add(Cards); add(Strategy);}},
            new ArrayList<Mechanic>() {{ add(DeckManagement); }}),
    DominionImprovements (2, 4,
            new ArrayList<Category>() {{ add(Cards); add(Strategy);}},
            new ArrayList<Mechanic>() {{ add(DeckManagement); }}),
    Catan (2, 5,
            new ArrayList<Category>() {{ add(Strategy); add(CityBuilding); add(Medieval); add(TerritoryBuilding); }},
            new ArrayList<Mechanic>() {{ add(Influence); }})
    ;

//    Carcassonne (2, 5,
//            new ArrayList<Category>() {{ add(Strategy); add(CityBuilding); add(Medieval); add(TerritoryBuilding); }},
//            new ArrayList<Mechanic>() {{ add(Influence); add(MapAddition); add(TilePlacement); }}),

    /**
     * Converts a given string to the enum type corresponding to the game.
     * Add here all games, planned or implemented.
     *
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
            case "blackjack":
                return Blackjack;
            case "virus":
                return Virus;
            case "coltexpress":
                return ColtExpress;
            case "dotsandboxes":
                return DotsAndBoxes;
            case "diamant":
                return Diamant;
            case "poker":
                return Poker;
            case "dominion":
                return Dominion;
            case "dominionsizedistortion":
                return DominionSizeDistortion;
            case "dominionimprovements" :
                return DominionImprovements;
            case "catan":
                return Catan;
        }
        System.out.println("Game type not found, returning null. ");
        return null;
    }

    /**
     * Creates an instance of the given game type, with a specific number of players and game seed.
     * Add here all games implemented.
     *
     * @param nPlayers - number of players taking part in the game, used for initialisation.
     * @param seed     - seed for this game.
     * @param params   - Parameters to use for the game. If not specified then we use the default.
     * @return - instance of Game object; null if game not implemented.
     */
    public Game createGameInstance(int nPlayers, long seed, AbstractParameters params) {
        if (nPlayers < minPlayers || nPlayers > maxPlayers) {
            return null;
        }

        params = (params == null) ? createParameterSet(seed) : params;
        AbstractForwardModel forwardModel;
        AbstractGameState gameState;

        switch (this) {
            case Pandemic:
                forwardModel = new PandemicForwardModel(params, nPlayers);
                gameState = new PandemicGameState(params, nPlayers);
                break;
            case TicTacToe:
                forwardModel = new TicTacToeForwardModel();
                gameState = new TicTacToeGameState(params, nPlayers);
                break;
            case ExplodingKittens:
                forwardModel = new ExplodingKittensForwardModel();
                gameState = new ExplodingKittensGameState(params, nPlayers);
                break;
            case LoveLetter:
                forwardModel = new LoveLetterForwardModel();
                gameState = new LoveLetterGameState(params, nPlayers);
                break;
            case Uno:
                forwardModel = new UnoForwardModel();
                gameState = new UnoGameState(params, nPlayers);
                break;
            case Blackjack:
                forwardModel = new BlackjackForwardModel();
                gameState = new BlackjackGameState(params, nPlayers);
                break;
            case Poker:
                forwardModel = new PokerForwardModel();
                gameState = new PokerGameState(params, nPlayers);
                break;
            case Virus:
                forwardModel = new VirusForwardModel();
                gameState = new VirusGameState(params, nPlayers);
                break;
            case ColtExpress:
                forwardModel = new ColtExpressForwardModel();
                gameState = new ColtExpressGameState(params, nPlayers);
                break;
            case DotsAndBoxes:
                forwardModel = new DBForwardModel();
                gameState = new DBGameState(params, nPlayers);
                break;
            case Diamant:
                forwardModel = new DiamantForwardModel();
                gameState = new DiamantGameState(params, nPlayers);
                break;
            case Dominion:
            case DominionImprovements:
            case DominionSizeDistortion:
                forwardModel = new DominionForwardModel();
                gameState = new DominionGameState(params, nPlayers);
                break;
            case Catan:
                forwardModel = new CatanForwardModel();
                gameState = new CatanGameState(params, nPlayers);
                break;
            default:
                throw new AssertionError("Game not yet supported : " + this);
        }

        return new Game(this, forwardModel, gameState);
    }

    public AbstractParameters createParameterSet(long seed) {
        switch (this) {
            case Pandemic:
                return new PandemicParameters("data/pandemic/", seed);
            case TicTacToe:
                return new TicTacToeGameParameters(seed);
            case ExplodingKittens:
                return new ExplodingKittensParameters(seed);
            case LoveLetter:
                return new LoveLetterParameters(seed);
            case Uno:
                return new UnoGameParameters(seed);
            case Blackjack:
                return new BlackjackParameters(seed);
            case Poker:
                return new PokerGameParameters(seed);
            case Virus:
                return new VirusGameParameters(seed);
            case ColtExpress:
                return new ColtExpressParameters(seed);
            case DotsAndBoxes:
                return new DBParameters(seed);
            case Diamant:
                return new DiamantParameters(seed);
            case Dominion:
                return DominionParameters.firstGame(seed);
            case DominionSizeDistortion:
                return DominionParameters.sizeDistortion(seed);
            case DominionImprovements:
                return DominionParameters.improvements(seed);
            case Catan:
                return new CatanParameters(seed);
            default:
                throw new AssertionError("No default Parameters specified for Game " + this);
        }
    }

    /**
     * Creates a graphical user interface for the given game type. Add here all games with a GUI available.
     *
     * @param game - game to create a GUI for.
     * @param ac   - ActionController object allowing for user interaction with the GUI.
     * @return - GUI for the given game type.
     */
    public AbstractGUIManager createGUIManager(GamePanel parent, Game game, ActionController ac) {

        AbstractGUIManager gui = null;

        // Find ID of human player, if any (-1 if none)
        int human = -1;
        if (game != null && game.getPlayers() != null) {
            for (int i = 0; i < game.getPlayers().size(); i++) {
                if (game.getPlayers().get(i) instanceof HumanGUIPlayer) {
                    human = i;
                    break;
                }
            }
        }

        switch (this) {
            case Pandemic:
                gui = new PandemicGUIManager(parent, game, ac);
                break;
//            case ExplodingKittens:
//                if (gameState != null) {
//                    gui = new PrototypeGUI(this, gameState, ac, 500);
//                } else {
//                    gui = new PrototypeGUI(this,null, ac, 0);
//                }
            case Uno:
                gui = new UnoGUIManager(parent, game, ac, human);
                break;
            case Blackjack:
                gui = new BlackjackGUIManager(parent, game, ac, human);
                break;
            case Poker:
                gui = new PokerGUIManager(parent, game, ac, human);
                break;
            case ColtExpress:
                gui = new ColtExpressGUIManager(parent, game, ac, human);
                break;
            case ExplodingKittens:
                gui = new ExplodingKittensGUIManager(parent, game, ac, human);
                break;
            case LoveLetter:
                gui = new LoveLetterGUIManager(parent, game, ac, human);
                break;
            case TicTacToe:
                gui = new TicTacToeGUIManager(parent, game, ac);
                break;
            case DotsAndBoxes:
                if (game != null) {
                    gui = new DBGUIManager(parent, game.getGameState(), ac);
                } else {
                    gui = new PrototypeGUIManager(parent, null, null, ac, 100);
                }
                break;
            case Dominion:
            case DominionImprovements:
            case DominionSizeDistortion:
                gui = new DominionGUIManager(parent, game, ac, human);
                break;
            case Catan:
                gui = new CatanGUI(game, ac, parent);

            // TODO: Diamant GUI
        }

        return gui;
    }


    // Minimum and maximum number of players supported in this game
    private final int minPlayers, maxPlayers;

    // boardgamegeek.com topic classification of games
    private final ArrayList<Category> categories;
    private final ArrayList<Mechanic> mechanics;

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
        Miniatures,
        Bluffing;

        /**
         * Retrieves a list of all games within this category.
         *
         * @return - list of game types.
         */
        public List<GameType> getAllGames() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt : GameType.values()) {
                if (gt.getCategories().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }

        /**
         * Retrieves a list of all games that are NOT within this category.
         *
         * @return - list of game types.
         */
        public List<GameType> getAllGamesExcluding() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt : GameType.values()) {
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
        Campaign,
        Enclosure,
        MoveThroughDeck,
        DeckManagement;

        /**
         * Retrieves a list of all games using this mechanic.
         *
         * @return - list of game types.
         */
        public List<GameType> getAllGames() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt : GameType.values()) {
                if (gt.getMechanics().contains(this)) {
                    games.add(gt);
                }
            }
            return games;
        }

        /**
         * Retrieves a list of all games that do NOT use this mechanic.
         *
         * @return - list of game types.
         */
        public List<GameType> getAllGamesExcluding() {
            ArrayList<GameType> games = new ArrayList<>();
            for (GameType gt : GameType.values()) {
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

    public static int getMinPlayersAllGames() {
        int min = Integer.MAX_VALUE;
        for (GameType gt : GameType.values()) {
            if (gt.minPlayers < min) min = gt.minPlayers;
        }
        return min;
    }

    public static int getMaxPlayersAllGames() {
        int max = Integer.MIN_VALUE;
        for (GameType gt : GameType.values()) {
            if (gt.minPlayers > max) max = gt.minPlayers;
        }
        return max;
    }

    /**
     * Creates an instance of the given game type with nPlayers number of players and a new random seed.
     *
     * @param nPlayers - number of players for the game.
     * @return - instance of Game object; null if game not implemented.
     */
    public Game createGameInstance(int nPlayers) {
        return createGameInstance(nPlayers, System.currentTimeMillis(), createParameterSet(System.currentTimeMillis()));
    }

    public Game createGameInstance(int nPlayers, long seed) {
        return createGameInstance(nPlayers, seed, createParameterSet(seed));
    }

    public Game createGameInstance(int nPlayers, AbstractParameters gameParams) {
        if (gameParams == null) {
            return createGameInstance(nPlayers, System.currentTimeMillis(), gameParams);
        } else {
            return createGameInstance(nPlayers, gameParams.getRandomSeed(), gameParams);
        }
    }

    public AbstractParameters defaultParams(long seed) {
        switch (this) {
            case Pandemic:
                return new PandemicParameters("data/pandemic/", seed);
            case TicTacToe:
                return new TicTacToeGameParameters(seed);
            case ExplodingKittens:
                return new ExplodingKittensParameters(seed);
            case LoveLetter:
                return new LoveLetterParameters(seed);
            case Uno:
                return new UnoGameParameters(seed);
            case Virus:
                return new VirusGameParameters(seed);
            case ColtExpress:
                return new ColtExpressParameters(seed);
            case DotsAndBoxes:
                return new DBParameters(seed);
            default:
                throw new AssertionError("No default Parameters specified for GameType : " + this.name());
        }
    }

    @Override
    public String toString() {
        boolean implemented = createGameInstance(minPlayers) != null;
        AbstractGUIManager g = createGUIManager(null,null, null);
        boolean gui = g != null;
        boolean prototypeGUI = g instanceof PrototypeGUIManager;
        return (gui ? prototypeGUI ? ANSI_CYAN : ANSI_BLUE : implemented ? ANSI_GREEN : ANSI_RED) + this.name() + ANSI_RESET + " {" +
                "\n\tminPlayers = " + minPlayers +
                "\n\tmaxPlayers = " + maxPlayers +
                "\n\tcategories = " + categories +
                "\n\tmechanics = " + mechanics +
                (implemented ? ANSI_GREEN : ANSI_RED) +
                "\n\timplemented = " + implemented + ANSI_RESET +
                (gui ? prototypeGUI ? ANSI_CYAN : ANSI_BLUE : ANSI_RED) +
                "\n\tGUI = " + gui + ANSI_RESET +
                "\n}\n";
    }

    public static void main(String[] args) {
        System.out.println("Games available in the framework: \n");
        for (GameType gt : GameType.values()) {
            System.out.println(gt.toString());
        }
    }
}
