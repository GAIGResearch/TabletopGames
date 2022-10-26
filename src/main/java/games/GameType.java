package games;

import core.*;
import games.battlelore.BattleloreForwardModel;
import games.battlelore.BattleloreGameState;
import games.battlelore.gui.BattleloreGUI;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.gui.BlackjackGUIManager;
import games.cantstop.CantStopForwardModel;
import games.cantstop.CantStopGameState;
import games.cantstop.gui.CantStopGUIManager;
import games.catan.CatanForwardModel;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.gui.CatanGUI;
import games.coltexpress.ColtExpressForwardModel;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.gui.ColtExpressGUIManager;
import games.connect4.Connect4ForwardModel;
import games.connect4.Connect4GameState;
import games.connect4.gui.Connect4GUIManager;
import games.diamant.DiamantForwardModel;
import games.diamant.DiamantGameState;
import games.dominion.gui.DominionGUIManager;
import games.dotsboxes.DBForwardModel;
import games.dotsboxes.DBGUIManager;
import games.dotsboxes.DBGameState;
import games.explodingkittens.ExplodingKittensForwardModel;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.gui.ExplodingKittensGUIManager;
import games.loveletter.*;
import games.loveletter.gui.LoveLetterGUIManager;
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGameState;
import games.pandemic.gui.PandemicGUIManager;
import games.terraformingmars.TMForwardModel;
import games.terraformingmars.TMGameState;
import games.terraformingmars.gui.TMGUI;
import games.poker.*;
import games.poker.gui.*;
import games.dicemonastery.gui.*;
import games.stratego.StrategoForwardModel;
import games.stratego.StrategoGameState;
import games.stratego.gui.StrategoGUIManager;
import games.sushigo.SGForwardModel;
import games.sushigo.SGGameState;
import games.sushigo.gui.SGGUI;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.gui.*;
import games.uno.UnoForwardModel;
import games.uno.UnoGameState;
import games.uno.gui.*;
import games.virus.VirusForwardModel;
import games.virus.VirusGameState;
import games.dicemonastery.*;
import games.dominion.*;
import gui.*;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import java.util.*;

import static core.CoreConstants.*;
import static games.GameType.Category.Number;
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
            new ArrayList<Category>() {{
                add(Strategy);
                add(Medical);
            }},
            new ArrayList<Mechanic>() {{
                add(ActionPoints);
                add(Cooperative);
                add(HandManagement);
                add(PointToPointMovement);
                add(SetCollection);
                add(Trading);
                add(VariablePlayerPowers);
            }}),
    TicTacToe(2, 2,
            new ArrayList<Category>() {{
                add(Simple);
                add(Abstract);
            }},
            new ArrayList<Mechanic>() {{
                add(PatternBuilding);
            }}),
    Connect4(2, 2,
            new ArrayList<Category>() {{
                add(Simple);
                add(Abstract);
            }},
            new ArrayList<Mechanic>() {{
                add(PatternBuilding);
            }}),
    ExplodingKittens(2, 5,
            new ArrayList<Category>() {{
                add(Strategy);
                add(Animals);
                add(Cards);
                add(ComicBook);
                add(Humour);
            }},
            new ArrayList<Mechanic>() {{
                add(HandManagement);
                add(HotPotato);
                add(PlayerElimination);
                add(PushYourLuck);
                add(SetCollection);
                add(TakeThat);
            }}),
    LoveLetter(2, 4,
            new ArrayList<Category>() {{
                add(Cards);
                add(Deduction);
                add(Renaissance);
            }},
            new ArrayList<Mechanic>() {{
                add(HandManagement);
                add(PlayerElimination);
            }}),
    Uno(2, 10,
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
    Virus(2, 6,
            new ArrayList<Category>() {{
                add(Cards);
                add(Medical);
            }},
            new ArrayList<Mechanic>() {{
                add(CardDrafting);
                add(SetCollection);
                add(TakeThat);
            }}),
    ColtExpress(2, 6,
            new ArrayList<Category>() {{
                add(Strategy);
                add(AmericanWest);
                add(Fighting);
                add(Trains);
            }},
            new ArrayList<Mechanic>() {{
                add(ActionQueue);
                add(HandManagement);
                add(Memory);
                add(ProgrammedEvent);
                add(SimultaneousActionSelection);
                add(TakeThat);
                add(VariablePlayerPowers);
            }}),
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
                add(Bluffing);
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
    Diamant(2, 6,
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
    DiceMonastery(2, 4,
            new ArrayList<Category>() {{
                add(Strategy);
                add(Medieval);
            }},
            new ArrayList<Mechanic>() {{
                add(SetCollection);
                add(WorkerPlacement);
                add(EngineBuilding);
            }}),
    Dominion(2, 4,
            new ArrayList<Category>() {{
                add(Cards);
                add(Strategy);
            }},
            new ArrayList<Mechanic>() {{
                add(DeckManagement);
            }}),
    DominionSizeDistortion(2, 4,
            new ArrayList<Category>() {{
                add(Cards);
                add(Strategy);
            }},
            new ArrayList<Mechanic>() {{
                add(DeckManagement);
            }}),
    DominionImprovements(2, 4,
            new ArrayList<Category>() {{
                add(Cards);
                add(Strategy);
            }},
            new ArrayList<Mechanic>() {{
                add(DeckManagement);
            }}),
    Battlelore(2, 2,
            new ArrayList<Category>() {{
                add(Fantasy);
                add(Miniatures);
                add(Wargame);
            }},
            new ArrayList<Mechanic>() {{
                add(Campaign);
                add(BattleCardDriven);
                add(CommandCards);
                add(DiceRolling);
                add(GridMovement);
                add(ModularBoard);
                add(VariablePlayerPowers);
            }}),
    SushiGo(2, 5,
            new ArrayList<Category>() {{
                add(Strategy);
                add(Cards);
            }},
            new ArrayList<Mechanic>() {{
                add(SetCollection);
                add(PushYourLuck);
                add(SimultaneousActionSelection);
            }}),
    Catan(3, 4,
            new ArrayList<Category>() {{
                add(Strategy);
                add(Cards);
            }},
            new ArrayList<Mechanic>() {{
                add(Memory);
                add(GridMovement);
                add(ModularBoard);
            }}),
    TerraformingMars (1, 5,
            new ArrayList<Category>() {{ add(Economic); add(Environmental); add(Manufacturing); add(TerritoryBuilding);
                add(Cards); add(Strategy); add(Exploration); }},
            new ArrayList<Mechanic>() {{ add(Drafting); add(EndGameBonus); add(HandManagement); add(HexagonGrid);
                add(Income); add(SetCollection); add(TakeThat); add(TilePlacement); add(ProgressiveTurnOrder);
                add(VariablePlayerPowers); add(EngineBuilding); add(TableauBuilding);}}),
    Stratego(2, 2,
            new ArrayList<Category>() {{
                add(Strategy);
                add(Bluffing);
                add(Deduction);
                add(Abstract);
            }},
            new ArrayList<Mechanic>() {{
                add(Memory);
                add(GridMovement);
            }}),
    CantStop(2, 4,
            new ArrayList<Category>() {{
                add(Dice);
                add(Abstract);
            }},
            new ArrayList<Mechanic>() {{
                add(PushYourLuck);
            }}
    );

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
            case "connect4":
                return Connect4;
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
            case "dominionimprovements":
                return DominionImprovements;
            case "catan":
                return Catan;
            case "battlelore":
                return Battlelore;
            case "dicemonastery":
                return DiceMonastery;
            case "sushigo":
                return SushiGo;
            case "stratego":
                return Stratego;
            case "cantstop":
                return CantStop;
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
            throw new IllegalArgumentException("Unsupported number of players: " + nPlayers
                    + ". Should be in range [" + minPlayers + "," + maxPlayers + "].");
        }

        if (params == null) {
            params = ParameterFactory.getDefaultParams(this, seed);
        } else {
            params.setRandomSeed(seed);
        }
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
            case Connect4:
                forwardModel = new Connect4ForwardModel();
                gameState = new Connect4GameState(params, nPlayers);
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
            case DiceMonastery:
                forwardModel = new DiceMonasteryForwardModel();
                gameState = new DiceMonasteryGameState(params, nPlayers);
                break;
            case Dominion:
            case DominionImprovements:
            case DominionSizeDistortion:
                forwardModel = new DominionForwardModel();
                gameState = new DominionGameState(params, nPlayers);
                break;
            case TerraformingMars:
                forwardModel = new TMForwardModel();
                gameState = new TMGameState(params, nPlayers);
                break;
            case Catan:
                forwardModel = new CatanForwardModel();
                gameState = new CatanGameState(params, nPlayers);
                break;
            case Battlelore:
                forwardModel = new BattleloreForwardModel();
                gameState = new BattleloreGameState(params, nPlayers);
                break;
            case SushiGo:
                forwardModel = new SGForwardModel();
                gameState = new SGGameState(params, nPlayers);
                break;
            case Stratego:
                forwardModel = new StrategoForwardModel();
                gameState = new StrategoGameState(params, nPlayers);
                break;
            case CantStop:
                forwardModel = new CantStopForwardModel();
                gameState = new CantStopGameState(params, nPlayers);
                break;
            default:
                throw new AssertionError("Game not yet supported : " + this);
        }

        return new Game(this, forwardModel, gameState);
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
            case Connect4:
                gui = new Connect4GUIManager(parent, game, ac);
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
                gui = new CatanGUI(parent, game, ac);
                break;
            // TODO: Diamant GUI
            case TerraformingMars:
                gui = new TMGUI(parent, game, ac);
                break;
            case Battlelore:
                gui = new BattleloreGUI(parent, game, ac);
                break;
            case DiceMonastery:
                gui = new DiceMonasteryGUI(parent, game, ac, human);
                break;
            case SushiGo:
                gui = new SGGUI(parent, game, ac, human);
                break;
            case Stratego:
                gui = new StrategoGUIManager(parent, game, ac);
                break;
            case CantStop:
                gui = new CantStopGUIManager(parent, game, ac);
                break;
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
        Dice,
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
        Bluffing,
        Economic,
        Environmental,
        Manufacturing,
        Wargame;

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
        WorkerPlacement,
        EngineBuilding,
        LineOfSight,
        ModularBoard,
        MovementPoints,
        MultipleMaps,
        Campaign,
        Enclosure,
        DeckManagement,
        Drafting,
        EndGameBonus,
        HexagonGrid,
        Income,
        ProgressiveTurnOrder,
        TableauBuilding,
        BattleCardDriven,
        CommandCards,
        MoveThroughDeck;

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
        return createGameInstance(nPlayers, System.currentTimeMillis(), ParameterFactory.getDefaultParams(this, System.currentTimeMillis()));
    }

    public Game createGameInstance(int nPlayers, long seed) {
        return createGameInstance(nPlayers, seed, ParameterFactory.getDefaultParams(this, seed));
    }

    public Game createGameInstance(int nPlayers, AbstractParameters gameParams) {
        if (gameParams == null) {
            return createGameInstance(nPlayers, System.currentTimeMillis(), gameParams);
        } else {
            return createGameInstance(nPlayers, gameParams.getRandomSeed(), gameParams);
        }
    }

    @Override
    public String toString() {
        boolean implemented = createGameInstance(minPlayers) != null;
        AbstractGUIManager g = createGUIManager(null, null, null);
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
