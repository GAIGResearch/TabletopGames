package games.risk;

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
 * Game with the same seed. The count of card sets traded sets each trade's value, so it must start again from zero.
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
    public void setsTradedStartFromZeroAfterReuse() {
        List<AbstractPlayer> players = players(3);
        Game reused = GameType.Risk.createGameInstance(3, 1L);
        reused.reset(players, 100L);
        reused.run();
        RiskGameState state = (RiskGameState) reused.getGameState();
        assertTrue("test needs a game in which sets were traded", state.nSetsTraded > 0);

        reused.reset(players, 7L);
        assertEquals(0, state.nSetsTraded);
        assertFalse(state.capturedThisTurn);
        assertFalse(state.territoryBonusTaken);
    }
}
