package games.powergrid;

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
 * Game with the same seed. The plants run and the discount card of the previous game must not carry over.
 * Components are equal only by ID, so states from two Games are never equal: the fields that leaked are compared instead.
 */
public class ResetTest {

    private static List<AbstractPlayer> players(int n) {
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < n; p++)
            players.add(new RandomPlayer(new Random(42 + p)));
        return players;
    }

    private static int plantsRun(PowerGridGameState state) {
        int n = 0;
        for (int id = 0; id < 1000; id++)
            if (state.hasPlantRun(id)) n++;
        return n;
    }

    @Test
    public void plantsRanAndDiscountCardAreResetAfterReuse() {
        List<AbstractPlayer> players = players(3);
        Game fresh = GameType.PowerGrid.createGameInstance(3, 1L);
        fresh.reset(players, 7L);
        PowerGridGameState freshState = (PowerGridGameState) fresh.getGameState();

        Game reused = GameType.PowerGrid.createGameInstance(3, 1L);
        reused.reset(players, 100L);
        reused.run();
        PowerGridGameState state = (PowerGridGameState) reused.getGameState();
        assertTrue("test needs a game that ends with plants run", plantsRun(state) > 0);

        reused.reset(players, 7L);
        assertEquals(0, plantsRun(freshState));
        assertEquals(0, plantsRun(state));
        assertEquals(freshState.getDiscountCard(), state.getDiscountCard());
    }
}
