package games.cribbage;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * A Game object reused for several games (as in every tournament) must start each game in the same state as a fresh
 * Game with the same seed. A game usually ends mid-round, so the last starter card must not survive into the next game.
 * Components are equal only by ID, so states from two Games are never equal: the fields that leaked are compared instead.
 */
public class ResetTest {

    @Test
    public void startStateMatchesFreshGameAfterReuse() {
        List<AbstractPlayer> players = List.of(new RandomPlayer(new Random(42)), new RandomPlayer(new Random(43)));
        Game reused = GameType.Cribbage.createGameInstance(2, 1L);
        for (int i = 0; i < 3; i++) {
            reused.reset(players, 100L + i);
            reused.run();
        }
        reused.reset(players, 7L);
        CribbageGameState state = (CribbageGameState) reused.getGameState();

        Game fresh = GameType.Cribbage.createGameInstance(2, 1L);
        fresh.reset(players, 7L);

        assertNull(state.getStarter());
        assertEquals(fresh.getGameState().getAllComponents().size(), state.getAllComponents().size());
        assertEquals(fresh.getForwardModel().computeAvailableActions(fresh.getGameState()).toString(),
                reused.getForwardModel().computeAvailableActions(state).toString());
    }
}
