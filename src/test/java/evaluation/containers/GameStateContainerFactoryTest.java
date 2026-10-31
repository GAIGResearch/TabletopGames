package evaluation.containers;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import games.GameType;
import games.cantstop.CantStopGameStateContainer;
import games.catan.CatanGameStateContainer;
import games.connect4.Connect4GameStateContainer;
import games.dominion.DominionGameStateContainer;
import games.dotsboxes.DBGameStateContainer;
import games.seasaltpaper.SSPGameStateContainer;
import games.uno.UnoGameStateContainer;
import games.wonders7.Wonders7GameStateContainer;
import org.junit.Test;
import players.jsonBagPlayers.Tokenizer;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GameStateContainerFactory} and for tokenizing a game state through its
 * container. The container is serialised reflectively by Gson, so these tests also guard against
 * field-name clashes in the game state hierarchy (Gson rejects a class that declares two fields
 * with the same JSON name).
 */
public class GameStateContainerFactoryTest {

    /** Game types supported by JSON-Bag, with a valid player count for each. */
    private static Map<GameType, Integer> supportedGames() {
        Map<GameType, Integer> games = new LinkedHashMap<>();
        games.put(GameType.CantStop, 2);
        games.put(GameType.Catan, 3);
        games.put(GameType.Connect4, 2);
        games.put(GameType.Dominion, 2);
        games.put(GameType.DominionSizeDistortion, 2);
        games.put(GameType.DominionImprovements, 2);
        games.put(GameType.DotsAndBoxes, 2);
        games.put(GameType.SeaSaltPaper, 2);
        games.put(GameType.Uno, 2);
        games.put(GameType.Wonders7, 3);
        return games;
    }

    @Test
    public void hasContainerIsTrueForEverySupportedGame() {
        supportedGames().keySet().forEach(gt ->
                assertTrue(gt + " should have a JSON-Bag container", GameStateContainerFactory.hasContainer(gt)));
    }

    @Test
    public void hasContainerIsFalseForUnsupportedGame() {
        assertFalse(GameStateContainerFactory.hasContainer(GameType.Poker));
        assertFalse(GameStateContainerFactory.hasContainer(GameType.Poker.createGameInstance(2, 7L).getGameState()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void createContainerFailsWithAClearErrorForUnsupportedGame() {
        GameStateContainerFactory.createContainer(GameType.Poker.createGameInstance(2, 7L).getGameState());
    }

    @Test
    public void createContainerReturnsTheMatchingContainerType() {
        Map<GameType, Class<? extends AbstractGameStateContainer>> expected = new LinkedHashMap<>();
        expected.put(GameType.CantStop, CantStopGameStateContainer.class);
        expected.put(GameType.Catan, CatanGameStateContainer.class);
        expected.put(GameType.Connect4, Connect4GameStateContainer.class);
        expected.put(GameType.Dominion, DominionGameStateContainer.class);
        expected.put(GameType.DotsAndBoxes, DBGameStateContainer.class);
        expected.put(GameType.SeaSaltPaper, SSPGameStateContainer.class);
        expected.put(GameType.Uno, UnoGameStateContainer.class);
        expected.put(GameType.Wonders7, Wonders7GameStateContainer.class);

        expected.forEach((gt, containerClass) -> {
            AbstractGameState gs = gt.createGameInstance(supportedGames().get(gt), 99L).getGameState();
            AbstractGameStateContainer container = GameStateContainerFactory.createContainer(gs);
            assertEquals(containerClass, container.getClass());
            assertEquals(gt, container.gameType);
            assertEquals(gs.getNPlayers(), container.nPlayers);
        });
    }

    @Test
    public void everySupportedGameStateTokenizesToANonEmptyBag() {
        supportedGames().forEach((gt, nPlayers) -> {
            AbstractGameState gs = gt.createGameInstance(nPlayers, 2024L).getGameState();
            Map<String, Integer> bag = Tokenizer.tokenize(gs);
            assertFalse(gt + " should produce tokens", bag.isEmpty());
            assertTrue(gt + " should record its game type", bag.keySet().stream().anyMatch(k -> k.contains("gameType")));
        });
    }

    @Test
    public void tokenizedGameStateIsDeterministicForAGivenSeed() {
        AbstractGameState first = GameType.SeaSaltPaper.createGameInstance(2, 77L).getGameState();
        AbstractGameState second = GameType.SeaSaltPaper.createGameInstance(2, 77L).getGameState();

        assertEquals(Tokenizer.tokenize(first), Tokenizer.tokenize(second));
    }
}
