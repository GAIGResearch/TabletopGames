package register;

import core.*;
import games.battlelore.BattleloreForwardModel;
import games.battlelore.BattleloreGameParameters;
import games.battlelore.BattleloreGameState;
import games.battlelore.gui.BattleloreGUI;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import games.blackjack.gui.BlackjackGUIManager;
import games.cantstop.CantStopForwardModel;
import games.cantstop.CantStopGameState;
import games.cantstop.CantStopParameters;
import games.cantstop.gui.CantStopGUIManager;
import games.catan.CatanForwardModel;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.gui.CatanGUI;
import games.coltexpress.ColtExpressForwardModel;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.gui.ColtExpressGUIManager;
import games.connect4.Connect4ForwardModel;
import games.connect4.Connect4GameParameters;
import games.connect4.Connect4GameState;
import games.connect4.gui.Connect4GUIManager;
import games.diamant.DiamantForwardModel;
import games.diamant.DiamantGameState;
import games.diamant.DiamantParameters;
import games.dominion.gui.DominionGUIManager;
import games.dotsboxes.DBForwardModel;
import games.dotsboxes.DBGUIManager;
import games.dotsboxes.DBGameState;
import games.dotsboxes.DBParameters;
import games.explodingkittens.ExplodingKittensForwardModel;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensParameters;
import games.explodingkittens.gui.ExplodingKittensGUIManager;
import games.loveletter.*;
import games.loveletter.gui.LoveLetterGUIManager;
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.gui.PandemicGUIManager;
import games.stratego.StrategoParams;
import games.sushigo.SGParameters;
import games.terraformingmars.TMForwardModel;
import games.terraformingmars.TMGameParameters;
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
import games.sushigo.gui.SGGUIManager;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.gui.*;
import games.uno.UnoForwardModel;
import games.uno.UnoGameParameters;
import games.uno.UnoGameState;
import games.uno.gui.*;
import games.virus.VirusForwardModel;
import games.virus.VirusGameParameters;
import games.virus.VirusGameState;
import games.dicemonastery.*;
import games.dominion.*;
import gui.*;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static core.CoreConstants.*;
import static register.GameType.Category.Number;
import static register.GameType.Category.*;
import static register.GameType.Mechanic.*;

/**
 * Encapsulates all games available in the framework, with minimum and maximum number of players as per game rules.
 * All games further include a list of categories and mechanics, which can be used to filter the game collection.
 */
public enum GameType {

    /**
     * Each game in the framework corresponds to a enum value here, giving minimum players, maximum players,
     * a list of categories the game belongs to, and a list of mechanics the game uses.
     * Add here all games implemented.
     */
    Pandemic(2, 4,
            Arrays.asList(Strategy, Medical),
            Arrays.asList(ActionPoints, Cooperative, HandManagement, PointToPointMovement, SetCollection, Trading, VariablePlayerPowers),
            PandemicGameState.class, PandemicForwardModel.class, PandemicParameters.class, PandemicGUIManager.class),
    TicTacToe(2, 2,
            Arrays.asList(Simple, Abstract),
            Collections.singletonList(PatternBuilding),
            TicTacToeGameState.class, TicTacToeForwardModel.class, TicTacToeGameParameters.class, TicTacToeGUIManager.class),
    Connect4(2, 2,
            Arrays.asList(Simple, Abstract),
            Collections.singletonList(PatternBuilding),
            Connect4GameState.class, Connect4ForwardModel.class, Connect4GameParameters.class, Connect4GUIManager.class),
    ExplodingKittens(2, 5,
            Arrays.asList(Strategy, Animals, Cards, ComicBook, Humour),
            Arrays.asList(HandManagement, HotPotato, PlayerElimination, PushYourLuck, SetCollection, TakeThat),
            ExplodingKittensGameState.class, ExplodingKittensForwardModel.class, ExplodingKittensParameters.class, ExplodingKittensGUIManager.class),
    LoveLetter(2, 4,
            Arrays.asList(Cards, Deduction, Renaissance),
            Arrays.asList(HandManagement, PlayerElimination),
            LoveLetterGameState.class, LoveLetterForwardModel.class, LoveLetterParameters.class, LoveLetterGUIManager.class),
    Uno(2, 10,
            Arrays.asList(Cards, ComicBook, Number, MoviesTVRadio),
            Arrays.asList(HandManagement, LoseATurn, TakeThat),
            UnoGameState.class, UnoForwardModel.class, UnoGameParameters.class, UnoGUIManager.class),
    Virus(2, 6,
            Arrays.asList(Cards, Medical),
            Arrays.asList(CardDrafting, SetCollection, TakeThat),
            VirusGameState.class, VirusForwardModel.class, VirusGameParameters.class, null),
    ColtExpress(2,
            6,
            Arrays.asList(Strategy, AmericanWest, Fighting, Trains),
            Arrays.asList(ActionQueue, HandManagement, Memory, ProgrammedEvent, SimultaneousActionSelection, TakeThat, VariablePlayerPowers),
            ColtExpressGameState.class, ColtExpressForwardModel.class, ColtExpressParameters.class, ColtExpressGUIManager.class),
    DotsAndBoxes(2, 6,
            Arrays.asList(Simple, Abstract, TerritoryBuilding),
            Collections.singletonList(Enclosure),
            DBGameState.class, DBForwardModel.class, DBParameters.class, DBGUIManager.class),
    Poker(2, 14,
            Arrays.asList(Cards, ComicBook, Number, MoviesTVRadio, Bluffing),
            Arrays.asList(HandManagement, LoseATurn, TakeThat),
            PokerGameState.class, PokerForwardModel.class, PokerGameParameters.class, PokerGUIManager.class),
    Blackjack(2, 7,
            Arrays.asList(Cards, ComicBook, Number, MoviesTVRadio),
            Arrays.asList(HandManagement, LoseATurn, TakeThat),
            BlackjackGameState.class, BlackjackForwardModel.class, BlackjackParameters.class, BlackjackGUIManager.class),
    Diamant(2, 6,
            Arrays.asList(Adventure, Bluffing, Exploration),
            Arrays.asList(MoveThroughDeck, PushYourLuck, SimultaneousActionSelection),
            DiamantGameState.class, DiamantForwardModel.class, DiamantParameters.class, null),
    DiceMonastery(2, 4,
            Arrays.asList(Strategy, Medieval),
            Arrays.asList(SetCollection, WorkerPlacement, EngineBuilding),
            DiceMonasteryGameState.class, DiceMonasteryForwardModel.class, DiceMonasteryParams.class, DiceMonasteryGUI.class),
    Dominion(2, 4,
            Arrays.asList(Cards, Strategy),
            Collections.singletonList(DeckManagement),
            DominionGameState.class, DominionForwardModel.class, DominionParameters.class, DominionGUIManager.class),
    DominionSizeDistortion(2, 4,
            Arrays.asList(Cards, Strategy),
            Collections.singletonList(DeckManagement),
            DominionGameState.class, DominionForwardModel.class, DominionParameters.class, DominionGUIManager.class),
    DominionImprovements(2, 4,
            Arrays.asList(Cards, Strategy),
            Collections.singletonList(DeckManagement),
            DominionGameState.class, DominionForwardModel.class, DominionParameters.class, DominionGUIManager.class),
    Battlelore(2, 2,
            Arrays.asList(Fantasy, Miniatures, Wargame),
            Arrays.asList(Campaign, BattleCardDriven, CommandCards, DiceRolling, GridMovement, ModularBoard, VariablePlayerPowers),
            BattleloreGameState.class, BattleloreForwardModel.class, BattleloreGameParameters.class, BattleloreGUI.class),
    SushiGo(2, 5,
            Arrays.asList(Strategy, Cards),
            Arrays.asList(SetCollection, PushYourLuck, SimultaneousActionSelection),
            SGGameState.class, SGForwardModel.class, SGParameters.class, SGGUIManager.class),
    Catan(3, 4,
            Arrays.asList(Strategy, Cards),
            Arrays.asList(Memory, GridMovement, ModularBoard),
            CatanGameState.class, CatanForwardModel.class, CatanParameters.class, CatanGUI.class),
    TerraformingMars (1, 5,
            Arrays.asList(Economic, Environmental, Manufacturing, TerritoryBuilding, Cards, Strategy, Exploration),
            Arrays.asList(Drafting, EndGameBonus, HandManagement, HexagonGrid, Income, SetCollection, TakeThat, TilePlacement, ProgressiveTurnOrder, VariablePlayerPowers, EngineBuilding, TableauBuilding),
            TMGameState.class, TMForwardModel.class, TMGameParameters.class, TMGUI.class),
    Stratego(2, 2,
            Arrays.asList(Strategy, Bluffing, Deduction, Abstract),
            Arrays.asList(Memory, GridMovement),
            StrategoGameState.class, StrategoForwardModel.class, StrategoParams.class, StrategoGUIManager.class),
    CantStop(2, 4,
            Arrays.asList(Dice, Abstract),
            Collections.singletonList(PushYourLuck),
            CantStopGameState.class, CantStopForwardModel.class, CantStopParameters.class, CantStopGUIManager.class
    );

    /**
     * Creates an instance of the given game type, with a specific number of players and game seed.
     * Add here all games implemented.
     *
     * @param nPlayers - number of players taking part in the game, used for initialisation.
     * @param seed     - seed for this game.
     * @param params   - Parameters to use for the game. If not specified then we use the default.
     * @return - instance of Game object
     */
    public Game createGameInstance(int nPlayers, long seed, AbstractParameters params) {
        if (nPlayers < minPlayers || nPlayers > maxPlayers) {
            throw new IllegalArgumentException("Unsupported number of players: " + nPlayers
                    + ". Should be in range [" + minPlayers + "," + maxPlayers + "].");
        }
        try {
            if (params == null) {
                Constructor<?> constructorParams = ConstructorUtils.getMatchingAccessibleConstructor(parameterClass, Long.class);
                    params = (AbstractParameters) constructorParams.newInstance(seed);

                    //TODO: some games have a datapath extra argument
//                return new BattleloreGameParameters("data/battlelore/", seed);
//                return new PandemicParameters("data/pandemic/", seed);
    //            params = ParameterFactory.getDefaultParams(this, seed);
            } else {
                params.setRandomSeed(seed);
            }

            Constructor<?> constructorFM = ConstructorUtils.getMatchingAccessibleConstructor(forwardModelClass);
            AbstractForwardModel forwardModel = (AbstractForwardModel) constructorFM.newInstance();
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(gameStateClass, AbstractParameters.class, Integer.class);
            AbstractGameState gameState = (AbstractGameState) constructorGS.newInstance(params, nPlayers);

            return new Game(this, forwardModel, gameState);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

//        switch (this) {
//            case Pandemic:
//                forwardModel = new PandemicForwardModel(params, nPlayers);
//                gameState = new PandemicGameState(params, nPlayers);
//                break;
//            case TicTacToe:
//                forwardModel = new TicTacToeForwardModel();
//                gameState = new TicTacToeGameState(params, nPlayers);
//                break;
//            case Connect4:
//                forwardModel = new Connect4ForwardModel();
//                gameState = new Connect4GameState(params, nPlayers);
//                break;
//            case ExplodingKittens:
//                forwardModel = new ExplodingKittensForwardModel();
//                gameState = new ExplodingKittensGameState(params, nPlayers);
//                break;
//            case LoveLetter:
//                forwardModel = new LoveLetterForwardModel();
//                gameState = new LoveLetterGameState(params, nPlayers);
//                break;
//            case Uno:
//                forwardModel = new UnoForwardModel();
//                gameState = new UnoGameState(params, nPlayers);
//                break;
//            case Blackjack:
//                forwardModel = new BlackjackForwardModel();
//                gameState = new BlackjackGameState(params, nPlayers);
//                break;
//            case Poker:
//                forwardModel = new PokerForwardModel();
//                gameState = new PokerGameState(params, nPlayers);
//                break;
//            case Virus:
//                forwardModel = new VirusForwardModel();
//                gameState = new VirusGameState(params, nPlayers);
//                break;
//            case ColtExpress:
//                forwardModel = new ColtExpressForwardModel();
//                gameState = new ColtExpressGameState(params, nPlayers);
//                break;
//            case DotsAndBoxes:
//                forwardModel = new DBForwardModel();
//                gameState = new DBGameState(params, nPlayers);
//                break;
//            case Diamant:
//                forwardModel = new DiamantForwardModel();
//                gameState = new DiamantGameState(params, nPlayers);
//                break;
//            case DiceMonastery:
//                forwardModel = new DiceMonasteryForwardModel();
//                gameState = new DiceMonasteryGameState(params, nPlayers);
//                break;
//            case Dominion:
//            case DominionImprovements:
//            case DominionSizeDistortion:
//                forwardModel = new DominionForwardModel();
//                gameState = new DominionGameState(params, nPlayers);
//                break;
//            case TerraformingMars:
//                forwardModel = new TMForwardModel();
//                gameState = new TMGameState(params, nPlayers);
//                break;
//            case Catan:
//                forwardModel = new CatanForwardModel();
//                gameState = new CatanGameState(params, nPlayers);
//                break;
//            case Battlelore:
//                forwardModel = new BattleloreForwardModel();
//                gameState = new BattleloreGameState(params, nPlayers);
//                break;
//            case SushiGo:
//                forwardModel = new SGForwardModel();
//                gameState = new SGGameState(params, nPlayers);
//                break;
//            case Stratego:
//                forwardModel = new StrategoForwardModel();
//                gameState = new StrategoGameState(params, nPlayers);
//                break;
//            case CantStop:
//                forwardModel = new CantStopForwardModel();
//                gameState = new CantStopGameState(params, nPlayers);
//                break;
//            default:
//                throw new AssertionError("Game not yet supported : " + this);
//        }
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
                gui = new SGGUIManager(parent, game, ac, human);
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
    private final List<Category> categories;
    private final List<Mechanic> mechanics;

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
    Class<? extends AbstractGameState> gameStateClass;
    Class<? extends AbstractForwardModel> forwardModelClass;
    Class<? extends AbstractParameters> parameterClass;
    Class<? extends AbstractGUIManager> guiManagerClass;

    GameType(int minPlayers, int maxPlayers, List<Category> categories, List<Mechanic> mechanics,
             Class<? extends AbstractGameState> gameStateClass, Class<? extends AbstractForwardModel> forwardModelClass,
             Class<? extends AbstractParameters> parameterClass, Class<? extends AbstractGUIManager> guiManagerClass) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.categories = categories;
        this.mechanics = mechanics;
        this.gameStateClass = gameStateClass;
        this.forwardModelClass = forwardModelClass;
        this.parameterClass = parameterClass;
        this.guiManagerClass = guiManagerClass;
    }

    // Getters
    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Mechanic> getMechanics() {
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
