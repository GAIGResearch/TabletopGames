package games;

import core.*;
import core.rules.AbstractRuleBasedForwardModel;
import games.battlelore.*;
import games.battlelore.gui.BattleloreGUI;
import games.blackjack.*;
import games.blackjack.gui.BlackjackGUIManager;
import games.cantstop.*;
import games.cantstop.gui.CantStopGUIManager;
import games.catan.*;
import games.catan.gui.CatanGUI;
import games.chinesecheckers.CCForwardModel;
import games.chinesecheckers.CCGameState;
import games.chinesecheckers.CCParameters;
import games.chinesecheckers.gui.CCGUIManager;
import games.coltexpress.*;
import games.coltexpress.gui.ColtExpressGUIManager;
import games.connect4.*;
import games.connect4.gui.Connect4GUIManager;
import games.diamant.*;
import games.dominion.gui.DominionGUIManager;
import games.dotsboxes.*;
import games.explodingkittens.*;
import games.explodingkittens.gui.ExplodingKittensGUIManager;
import games.hanabi.HanabiForwardModel;
import games.hanabi.HanabiGameState;
import games.hanabi.HanabiParameters;
import games.hanabi.gui.HanabiGUIManager;
import games.hearts.HeartsForwardModel;
import games.hearts.HeartsGameState;
import games.hearts.HeartsParameters;
import games.hearts.gui.HeartsGUIManager;
import games.loveletter.*;
import games.loveletter.gui.LoveLetterGUIManager;
import games.pandemic.*;
import games.pandemic.gui.PandemicGUIManager;
import games.puertorico.*;
import games.puertorico.gui.PuertoRicoGUI;
import games.resistance.ResForwardModel;
import games.resistance.ResGameState;
import games.resistance.ResParameters;
import games.resistance.gui.ResGUIManager;
import games.terraformingmars.*;
import games.terraformingmars.gui.TMGUI;
import games.poker.*;
import games.poker.gui.*;
import games.stratego.*;
import games.stratego.gui.StrategoGUIManager;
import games.sushigo.*;
import games.sushigo.gui.SGGUIManager;
import games.tictactoe.*;
import games.tictactoe.gui.*;
import games.uno.*;
import games.uno.gui.*;
import games.virus.*;
import games.dominion.*;
import games.wonders7.Wonders7ForwardModel;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import games.wonders7.gui.Wonders7GUI;
import gametemplate.GTForwardModel;
import gametemplate.GTGUIManager;
import gametemplate.GTGameState;
import gametemplate.GTParameters;
import gui.*;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static core.CoreConstants.*;
import static games.GameType.Category.Number;
import static games.GameType.Category.*;
import static games.GameType.Mechanic.*;

/**
 * Encapsulates all games available in the framework, with minimum and maximum number of players as per game rules.
 * All games further include a list of categories and mechanics, which can be used to filter the game collection.
 * Additionally: classes where the game state, forward model, parameters and GUI manager (optional, can be null) are implemented,
 * and path to where JSON data for the game is stored (optional).
 */
public enum GameType {

    /**
     * Game template example, see template in package {@link gametemplate}
     */
    GameTemplate(1, 8, null, null, GTGameState.class, GTForwardModel.class, GTParameters.class, GTGUIManager.class),
    /**
     * Each game in the framework corresponds to a enum value here, giving minimum players, maximum players,
     * a list of categories the game belongs to, and a list of mechanics the game uses.
     * Add here all games implemented.
     */
    Pandemic(2, 4,
            Arrays.asList(Strategy, Medical),
            Arrays.asList(ActionPoints, Cooperative, HandManagement, PointToPointMovement, SetCollection, Trading, VariablePlayerPowers),
            PandemicGameState.class, PandemicForwardModel.class, PandemicParameters.class, PandemicGUIManager.class,
            "data/pandemic/"),
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
    Dominion(2, 4,
            Arrays.asList(Cards, Strategy),
            Collections.singletonList(DeckManagement),
            DominionGameState.class, DominionForwardModel.class, DominionFGParameters.class, DominionGUIManager.class),
    DominionSizeDistortion(2, 4,
            Arrays.asList(Cards, Strategy),
            Collections.singletonList(DeckManagement),
            DominionGameState.class, DominionForwardModel.class, DominionSDParameters.class, DominionGUIManager.class),
    DominionImprovements(2, 4,
            Arrays.asList(Cards, Strategy),
            Collections.singletonList(DeckManagement),
            DominionGameState.class, DominionForwardModel.class, DominionIParameters.class, DominionGUIManager.class),
    Battlelore(2, 2,
            Arrays.asList(Fantasy, Miniatures, Wargame),
            Arrays.asList(Campaign, BattleCardDriven, CommandCards, DiceRolling, GridMovement, ModularBoard, VariablePlayerPowers),
            BattleloreGameState.class, BattleloreForwardModel.class, BattleloreGameParameters.class, BattleloreGUI.class,
            "data/battlelore/"),
    SushiGo(2, 5,
            Arrays.asList(Strategy, Cards),
            Arrays.asList(SetCollection, PushYourLuck, SimultaneousActionSelection),
            SGGameState.class, SGForwardModel.class, SGParameters.class, SGGUIManager.class),
    Catan(3, 4,
            Arrays.asList(Strategy, Cards),
            Arrays.asList(Memory, GridMovement, ModularBoard),
            CatanGameState.class, CatanForwardModel.class, CatanParameters.class, CatanGUI.class),
    TerraformingMars(1, 5,
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
            CantStopGameState.class, CantStopForwardModel.class, CantStopParameters.class, CantStopGUIManager.class),
    Hanabi(2, 5, new ArrayList<>(), new ArrayList<>(), HanabiGameState.class, HanabiForwardModel.class, HanabiParameters.class, HanabiGUIManager.class),
    PuertoRico(3, 5,
            Arrays.asList(Strategy, Economic, Manufacturing, TerritoryBuilding),
            Arrays.asList(EndGameBonus, TilePlacement, RoleSelection, EngineBuilding, TableauBuilding),
            PuertoRicoGameState.class, PuertoRicoForwardModel.class, PuertoRicoParameters.class, PuertoRicoGUI.class),
    Wonders7(3, 7,
            Arrays.asList(Strategy, Civilization, Ancient, Cards, CityBuilding, Economic),
            Arrays.asList(ClosedDrafting, HandManagement, NeighbourScope, SetCollection, SimultaneousActionSelection, VariablePlayerPowers),
            Wonders7GameState.class, Wonders7ForwardModel.class, Wonders7GameParameters.class, Wonders7GUI.class),
    Resistance(5, 10,
            Arrays.asList(Strategy, Bluffing, Deduction, Abstract),
            Arrays.asList(Memory, GridMovement),
            ResGameState.class, ResForwardModel.class, ResParameters.class, ResGUIManager.class),
    Hearts(3,7,Arrays.asList(Cards, Number),
            Arrays.asList(HandManagement, LoseATurn, TakeThat),
            HeartsGameState.class, HeartsForwardModel.class, HeartsParameters.class, HeartsGUIManager.class),
    ChineseCheckers(2, 6,
            Arrays.asList(Strategy, Abstract),
            Arrays.asList(GridMovement),
            CCGameState.class, CCForwardModel.class, CCParameters.class, CCGUIManager.class);


    // Core classes where the game is defined
    final Class<? extends AbstractGameState> gameStateClass;
    final Class<? extends AbstractForwardModel> forwardModelClass;
    final Class<? extends AbstractParameters> parameterClass;
    final Class<? extends AbstractGUIManager> guiManagerClass;

    // Minimum and maximum number of players supported in this game
    private final int minPlayers, maxPlayers;

    // boardgamegeek.com topic classification of games
    private final List<Category> categories;
    private final List<Mechanic> mechanics;

    // Data paths
    private final String dataPath;

    GameType(int minPlayers, int maxPlayers, List<Category> categories, List<Mechanic> mechanics,
             Class<? extends AbstractGameState> gameStateClass, Class<? extends AbstractForwardModel> forwardModelClass,
             Class<? extends AbstractParameters> parameterClass, Class<? extends AbstractGUIManager> guiManagerClass,
             String dataPath) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.categories = categories;
        this.mechanics = mechanics;
        this.gameStateClass = gameStateClass;
        this.forwardModelClass = forwardModelClass;
        this.parameterClass = parameterClass;
        this.guiManagerClass = guiManagerClass;
        this.dataPath = dataPath;
    }

    GameType(int minPlayers, int maxPlayers, List<Category> categories, List<Mechanic> mechanics,
             Class<? extends AbstractGameState> gameStateClass, Class<? extends AbstractForwardModel> forwardModelClass,
             Class<? extends AbstractParameters> parameterClass, Class<? extends AbstractGUIManager> guiManagerClass) {
        this(minPlayers, maxPlayers, categories, mechanics, gameStateClass, forwardModelClass, parameterClass, guiManagerClass, null);
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

    public String getDataPath() {
        return dataPath;
    }

    public AbstractGameState createGameState(AbstractParameters params, int nPlayers) {
        if (gameStateClass == null) throw new AssertionError("No game state class declared for the game: " + this);
        try {
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(gameStateClass, AbstractParameters.class, Integer.class);
            return (AbstractGameState) constructorGS.newInstance(params, nPlayers);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractForwardModel createForwardModel(AbstractParameters params, int nPlayers) {
        if (forwardModelClass == null)
            throw new AssertionError("No forward model class declared for the game: " + this);
        try {
            if (forwardModelClass.getSuperclass() == AbstractRuleBasedForwardModel.class) {
                Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(forwardModelClass, AbstractParameters.class, Integer.class);
                return (AbstractForwardModel) constructorGS.newInstance(params, nPlayers);
            } else {
                Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(forwardModelClass);
                return (AbstractForwardModel) constructorGS.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractParameters createParameters(long seed) {
        if (parameterClass == null) throw new AssertionError("No parameter class declared for the game: " + this);
        try {
            if (dataPath != null) {
                Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(parameterClass, String.class, Long.class);
                if (constructorGS == null) {
                    constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(parameterClass, String.class);
                    return (AbstractParameters) constructorGS.newInstance(dataPath);
                }
                return (AbstractParameters) constructorGS.newInstance(dataPath, seed);
            } else {
                Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(parameterClass, Long.class);
                if (constructorGS == null) {
                    constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(parameterClass);
                    return (AbstractParameters) constructorGS.newInstance();
                }
                return (AbstractParameters) constructorGS.newInstance(seed);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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
        if (guiManagerClass == null) throw new AssertionError("No GUI manager class declared for the game: " + this);

        // Find ID of human player, if any (-1 if none)
        Set<Integer> human = new HashSet<>();
        if (game != null && game.getPlayers() != null) {
            for (int i = 0; i < game.getPlayers().size(); i++) {
                if (game.getPlayers().get(i) instanceof HumanGUIPlayer) {
                    human.add(i);
                    break;
                }
            }
        }

        try {
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(guiManagerClass, GamePanel.class, Game.class, ActionController.class, Set.class);
            return (AbstractGUIManager) constructorGS.newInstance(parent, game, ac, human);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an instance of the given game type with nPlayers number of players and random seed.
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
        if (params == null) {
            params = createParameters(seed);
        } else {
            params.setRandomSeed(seed);
        }

        return new Game(this, createForwardModel(params, nPlayers), createGameState(params, nPlayers));
    }

    public Game createGameInstance(int nPlayers) {
        return createGameInstance(nPlayers, System.currentTimeMillis(), createParameters(System.currentTimeMillis()));
    }

    public Game createGameInstance(int nPlayers, long seed) {
        return createGameInstance(nPlayers, seed, createParameters(seed));
    }

    public Game createGameInstance(int nPlayers, AbstractParameters gameParams) {
        if (gameParams == null) {
            return createGameInstance(nPlayers, System.currentTimeMillis(), null);
        } else {
            return createGameInstance(nPlayers, gameParams.getRandomSeed(), gameParams);
        }
    }

    @Override
    public String toString() {
        boolean gui = guiManagerClass != null;
        boolean fm = forwardModelClass != null;
        boolean gs = gameStateClass != null;
        boolean params = parameterClass != null;
        return ANSI_GREEN + this.name() + ANSI_RESET + " {" +
                "\n\tminPlayers = " + minPlayers +
                "\n\tmaxPlayers = " + maxPlayers +
                "\n\tcategories = " + categories +
                "\n\tmechanics = " + mechanics +
                (gs ? ANSI_BLUE : ANSI_RED) + "\n\tGS = " + gs + ANSI_RESET +
                (fm ? ANSI_BLUE : ANSI_RED) + "\n\tFM = " + fm + ANSI_RESET +
                (params ? ANSI_BLUE : ANSI_RED) + "\n\tParams = " + params + ANSI_RESET +
                (gui ? ANSI_BLUE : ANSI_RED) + "\n\tGUI = " + gui + ANSI_RESET +
                "\n}\n";
    }

    public static void main(String[] args) {
        System.out.println("Games available in the framework: \n");
        for (GameType gt : GameType.values()) {
            System.out.println(gt.toString());
        }
    }

    @SuppressWarnings("unused")
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
        Wargame, Civilization, Ancient;

        /**
         * @return a list of all games within this category.
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
         * @return a list of all games that are NOT within this category.
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

    @SuppressWarnings("unused")
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
        MoveThroughDeck,
        TrickTaking,
        RoleSelection, ClosedDrafting, NeighbourScope;

        /**
         * @return a list of all games using this mechanic.
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
         * @return a list of all games that do NOT use this mechanic.
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
}
