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

    public AbstractGameState createGameState(AbstractParameters params, int nPlayers) {
        if (gameStateClass == null) throw new AssertionError("No game state class declared for the game: " + this);
        try {
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(gameStateClass, AbstractParameters.class, Integer.class);
            return (AbstractGameState) constructorGS.newInstance(params, nPlayers);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractForwardModel createForwardModel() {
        if (forwardModelClass == null) throw new AssertionError("No forward model class declared for the game: " + this);
        try {
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(forwardModelClass);
            return (AbstractForwardModel) constructorGS.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractParameters createParameters(long seed) {
        if (parameterClass == null) throw new AssertionError("No parameter class declared for the game: " + this);
        try {
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(parameterClass, Long.class);
            return (AbstractParameters) constructorGS.newInstance(seed);
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
        int human = -1;
        if (game != null && game.getPlayers() != null) {
            for (int i = 0; i < game.getPlayers().size(); i++) {
                if (game.getPlayers().get(i) instanceof HumanGUIPlayer) {
                    human = i;
                    break;
                }
            }
        }

        try {
            Constructor<?> constructorGS = ConstructorUtils.getMatchingAccessibleConstructor(guiManagerClass, GamePanel.class, Game.class, ActionController.class, Integer.class);
            return (AbstractGUIManager) constructorGS.newInstance(parent, game, ac, human);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

//                gui = new PandemicGUIManager(parent, game, ac);
//                gui = new TicTacToeGUIManager(parent, game, ac);
//                gui = new Connect4GUIManager(parent, game, ac);
//                    gui = new DBGUIManager(parent, game.getGameState(), ac);
//                gui = new CatanGUI(parent, game, ac);
//                gui = new TMGUI(parent, game, ac);
//                gui = new BattleloreGUI(parent, game, ac);
//                gui = new StrategoGUIManager(parent, game, ac);
//                gui = new CantStopGUIManager(parent, game, ac);

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

            //TODO: some games have a datapath extra argument
//                return new BattleloreGameParameters("data/battlelore/", seed);
//                return new PandemicParameters("data/pandemic/", seed);
        } else {
            params.setRandomSeed(seed);
        }

        return new Game(this, createForwardModel(), createGameState(params, nPlayers));
    }
    public Game createGameInstance(int nPlayers) {
        return createGameInstance(nPlayers, System.currentTimeMillis(), createParameters(System.currentTimeMillis()));
    }
    public Game createGameInstance(int nPlayers, long seed) {
        return createGameInstance(nPlayers, seed, createParameters(seed));
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
}
