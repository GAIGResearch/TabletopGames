package games.puertorico;

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
 * Game with the same seed, with no role owner left from the previous game.
 * Components are equal only by ID, so states from two Games are never equal: the field that leaked is compared instead.
 */
public class ResetTest {

    @Test
    public void roleOwnerIsResetBetweenGames() {
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            players.add(new RandomPlayer(new Random(42 + p)));
        Game fresh = GameType.PuertoRico.createGameInstance(4, 1L);
        fresh.reset(players, 7L);
        PuertoRicoGameState freshState = (PuertoRicoGameState) fresh.getGameState();

        Game reused = GameType.PuertoRico.createGameInstance(4, 1L);
        for (int i = 0; i < 3; i++) {
            reused.reset(players, 100L + i);
            reused.run();
            if (((PuertoRicoGameState) reused.getGameState()).getRoleOwner() != freshState.getRoleOwner())
                break;  // the leak is now observable
        }
        assertNotEquals("test needs a game that ends with a different role owner",
                freshState.getRoleOwner(), ((PuertoRicoGameState) reused.getGameState()).getRoleOwner());
        reused.reset(players, 7L);
        PuertoRicoGameState state = (PuertoRicoGameState) reused.getGameState();

        assertNull(state.getCurrentRole());
        assertEquals(freshState.getRoleOwner(), state.getRoleOwner());
        assertEquals(fresh.getForwardModel().computeAvailableActions(fresh.getGameState()).toString(),
                reused.getForwardModel().computeAvailableActions(state).toString());
    }
}
