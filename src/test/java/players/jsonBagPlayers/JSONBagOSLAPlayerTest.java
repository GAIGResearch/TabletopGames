package players.jsonBagPlayers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import games.GameType;
import org.junit.Test;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link JSONBagOSLAPlayer}: a one-step-look-ahead player that evaluates
 * states with a {@link JSONBagHeuristic} instead of a game-specific heuristic.
 */
public class JSONBagOSLAPlayerTest {

    private static List<Map<String, Integer>> prototypesFor(GameType gameType, int nPlayers, long seed) {
        AbstractGameState gs = gameType.createGameInstance(nPlayers, seed).getGameState();
        Map<String, Integer> prototype = Tokenizer.tokenize(gs);
        List<Map<String, Integer>> prototypes = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++)
            prototypes.add(new HashMap<>(prototype));
        return prototypes;
    }

    @Test
    public void playsAFullGameAgainstARandomPlayer() {
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new JSONBagOSLAPlayer(prototypesFor(GameType.DotsAndBoxes, 2, 11L),
                Collections.emptyList(), new Random(1)));
        players.add(new RandomPlayer(new Random(2)));

        Game game = Game.runOne(GameType.DotsAndBoxes, null, players, 1234L, false, null, null, 0);

        assertNotNull(game);
        assertNotEquals("game should have finished", CoreConstants.GameResult.GAME_ONGOING,
                game.getGameState().getGameStatus());
        assertTrue("the JSON-Bag bag should have accumulated tokens",
                ((JSONBagOSLAPlayer) players.get(0)).heuristic.currentJSONBag.size() > 0);
    }

    @Test
    public void playsAFullGameOfSeaSaltAndPaperAgainstAnOSLAPlayer() {
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new JSONBagOSLAPlayer(prototypesFor(GameType.SeaSaltPaper, 2, 21L),
                Collections.emptyList(), new Random(3)));
        players.add(new OSLAPlayer(new Random(4)));

        Game game = Game.runOne(GameType.SeaSaltPaper, null, players, 4321L, false, null, null, 0);

        assertNotNull(game);
        assertNotEquals(CoreConstants.GameResult.GAME_ONGOING, game.getGameState().getGameStatus());
    }

    @Test
    public void copyCarriesTheAccumulatedBagButNotTheReference() {
        AbstractGameState gs = GameType.DotsAndBoxes.createGameInstance(2, 5L).getGameState();
        JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypesFor(GameType.DotsAndBoxes, 2, 5L),
                Collections.emptyList(), new Random(7));

        player.initializePlayer(gs);
        JSONBagOSLAPlayer copy = player.copy();

        assertNotSame(player.heuristic, copy.heuristic);
        assertEquals(player.heuristic.currentJSONBag, copy.heuristic.currentJSONBag);
        assertEquals(player.toString(), copy.toString());
    }

    @Test
    public void copyGetsTheForwardModelFromTheGame() {
        List<AbstractPlayer> players = new ArrayList<>();
        JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypesFor(GameType.DotsAndBoxes, 2, 5L),
                Collections.emptyList(), new Random(9));
        players.add(player);
        players.add(new RandomPlayer(new Random(10)));

        Game game = Game.runOne(GameType.DotsAndBoxes, null, players, 99L, false, null, null, 0);

        assertNotNull(player.getForwardModel());
        assertEquals(player.getForwardModel(), player.copy().getForwardModel());
        assertNotNull(game);
    }

    @Test
    public void aCopyCarriesSimultaneousMode() {
        JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypesFor(GameType.Wonders7, 3, 17L),
                Collections.emptyList(), new Random(21));
        player.setSimultaneousMode(true);

        JSONBagOSLAPlayer copy = player.copy();

        assertTrue("a copy must keep the simultaneous setting", copy.simultaneousMode);
        assertTrue(player.simultaneousMode);
    }

    @Test
    public void aCopyOfASequentialPlayerStaysSequential() {
        JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypesFor(GameType.DotsAndBoxes, 2, 5L),
                Collections.emptyList(), new Random(22));

        assertFalse(player.copy().simultaneousMode);
    }

    @Test
    public void setFilterListRestrictsTheBag() {
        AbstractGameState gs = GameType.DotsAndBoxes.createGameInstance(2, 5L).getGameState();
        Map<String, Integer> tokens = Tokenizer.tokenize(gs);
        String keptToken = tokens.keySet().iterator().next();

        JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypesFor(GameType.DotsAndBoxes, 2, 5L),
                Collections.emptyList(), new Random(13));
        player.setFilterList(Collections.singletonList(keptToken));
        player.initializePlayer(gs);

        assertEquals(Collections.singleton(keptToken), player.heuristic.currentJSONBag.keySet());
    }


    /** The six games used in the JSON-Bag VF paper. */
    private static final GameType[] PAPER_GAMES = {
            GameType.Wonders7, GameType.Dominion, GameType.SeaSaltPaper,
            GameType.CantStop, GameType.Connect4, GameType.DotsAndBoxes
    };

    private static int paperPlayerCount(GameType gt) {
        return gt == GameType.Wonders7 ? 3 : 2;
    }

    @Test
    public void playsAFullGameOfEveryGameInThePaper() {
        for (GameType gameType : PAPER_GAMES) {
            int nPlayers = paperPlayerCount(gameType);
            List<AbstractPlayer> players = new ArrayList<>();
            JSONBagOSLAPlayer jsonBagPlayer = new JSONBagOSLAPlayer(
                    prototypesFor(gameType, nPlayers, 1234L), Collections.emptyList(), new Random(31));
            if (gameType == GameType.Wonders7)
                jsonBagPlayer.setSimultaneousMode(true);
            players.add(jsonBagPlayer);
            for (int i = 1; i < nPlayers; i++)
                players.add(new RandomPlayer(new Random(40 + i)));

            Game game = Game.runOne(gameType, null, players, 9876L, false, null, null, 0);

            assertNotNull(gameType + " should have produced a game", game);
            assertNotEquals(gameType + " should have finished",
                    CoreConstants.GameResult.GAME_ONGOING, game.getGameState().getGameStatus());
            assertTrue(gameType + " should have accumulated JSON-Bag tokens",
                    jsonBagPlayer.heuristic.currentJSONBag.size() > 0);
            assertEquals(gameType + " should have played a full round count",
                    nPlayers, game.getGameState().getNPlayers());
        }
    }

    @Test
    public void simultaneousModeCanBeToggled() {
        JSONBagOSLAPlayer player = new JSONBagOSLAPlayer(prototypesFor(GameType.Wonders7, 3, 17L),
                Collections.emptyList(), new Random(15));

        assertFalse(player.simultaneousMode);
        player.setSimultaneousMode(true);
        assertTrue(player.simultaneousMode);
    }
}
