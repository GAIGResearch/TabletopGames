package games.loyalist;

import static org.junit.Assert.*;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;

import games.GameType;

import org.junit.Test;

import players.PlayerConstants;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Exercises TAG's actual observation -> agent -> action -> real-state path. */
public class LoyalistIntegrationTest {
    @Test
    public void randomAgentsCompleteThirtyTwoGamesWithLegalObservationActions() {
        for (int seed = 1; seed <= 32; seed++) {
            List<AbstractPlayer> players = new ArrayList<>();
            for (int seat = 0; seat < 5; seat++)
                players.add(new RandomPlayer(new Random(1000L * seed + seat)));
            runBoundedGame(seed, players);
        }
    }

    @Test
    public void informationSetMctsAgentsCompleteGames() {
        for (int seed : new int[] {101, 202}) {
            List<AbstractPlayer> players = new ArrayList<>();
            for (int seat = 0; seat < 5; seat++) {
                MCTSParams params = new MCTSParams();
                params.setParameterValue("randomSeed", seed * 100 + seat);
                params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
                params.setParameterValue("budget", 8);
                params.setParameterValue("rolloutLength", 12);
                params.setParameterValue("maxTreeDepth", 8);
                players.add(new MCTSPlayer(params));
            }
            runBoundedGame(seed, players);
        }
    }

    @Test
    public void factoryRejectsUnsupportedPlayerCounts() {
        for (int count : new int[] {3, 4, 6}) {
            try {
                GameType.Loyalist.createGameInstance(count, 123L);
                fail("Only five-player rules are implemented");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("Unsupported number of players"));
            }
        }
    }

    private void runBoundedGame(long seed, List<AbstractPlayer> players) {
        Game game = GameType.Loyalist.createGameInstance(5, seed);
        game.reset(players, seed);
        AbstractGameState state = game.getGameState();
        assertTrue(
                "Private action history must never be passed to agents",
                state.getCoreGameParameters().competitionMode);
        int decisions = 0;
        while (state.isNotTerminal() && decisions < 4000) {
            String context = "seed=" + seed + ", decision=" + decisions;
            assertPhysicalInventory((LoyalistGameState) state, context);
            int seat = state.getCurrentPlayer();
            List<AbstractAction> actual = game.getForwardModel().computeAvailableActions(state);
            assertFalse(context, actual.isEmpty());
            AbstractGameState observation = state.copy(seat);
            assertTrue(context, observation.getHistory().isEmpty());
            assertEquals(
                    context,
                    new HashSet<>(actual),
                    new HashSet<>(game.getForwardModel().computeAvailableActions(observation)));
            assertEquals(
                    context + ", second copy",
                    new HashSet<>(actual),
                    new HashSet<>(
                            game.getForwardModel()
                                    .computeAvailableActions(observation.copy(seat))));
            AbstractAction action = game.oneAction();
            assertTrue(context + ", chosen action=" + action, actual.contains(action));
            decisions++;
        }
        assertFalse(
                "Game did not terminate: seed=" + seed + ", decisions=" + decisions,
                state.isNotTerminal());
        assertFalse("A completed game must have a winner", state.getWinners().isEmpty());
        assertPhysicalInventory((LoyalistGameState) state, "terminal seed=" + seed);
    }

    private void assertPhysicalInventory(LoyalistGameState state, String context) {
        List<Integer> actualCards = new ArrayList<>(state.deck);
        actualCards.addAll(state.dead);
        actualCards.addAll(state.bagCards);
        actualCards.addAll(state.keepSelected);
        state.hands.forEach(actualCards::addAll);
        Collections.sort(actualCards);
        List<Integer> expectedCards = new ArrayList<>(LoyalistData.powerCards());
        Collections.sort(expectedCards);
        assertEquals(context + ", forty-card multiset", expectedCards, actualCards);
        assertTrue(context, state.bagTokens >= 0);
        assertTrue(context, state.reserve >= 0);
        for (int value : state.treasury) assertTrue(context, value >= 0);
        for (int value : state.wealth) assertTrue(context, value >= 0);
    }
}
