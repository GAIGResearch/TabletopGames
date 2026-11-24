package games.resistance;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * A Game object reused for several games (as in every tournament) must start each game in the same state as a fresh
 * Game with the same seed. The result of the last team vote must not carry over.
 * Components are equal only by ID, so states from two Games are never equal: the fields that leaked are compared instead.
 */
public class ResetTest {

    private static List<AbstractPlayer> players(int n) {
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < n; p++)
            players.add(new RandomPlayer(new Random(42 + p)));
        return players;
    }

    @Test
    public void voteSuccessIsResetAfterReuse() {
        List<AbstractPlayer> players = players(5);
        Game fresh = GameType.Resistance.createGameInstance(5, 1L);
        fresh.reset(players, 7L);
        boolean freshValue = ((ResGameState) fresh.getGameState()).voteSuccess;

        Game reused = GameType.Resistance.createGameInstance(5, 1L);
        ResGameState state = (ResGameState) reused.getGameState();
        for (int i = 0; i < 10 && state.voteSuccess == freshValue; i++) {
            reused.reset(players, 100L + i);
            reused.run();
        }
        assertNotEquals("test needs a game ending with a different vote result", freshValue, state.voteSuccess);

        reused.reset(players, 7L);
        assertEquals(freshValue, state.voteSuccess);
    }
}
