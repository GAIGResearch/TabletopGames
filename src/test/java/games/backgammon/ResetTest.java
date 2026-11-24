package games.backgammon;

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
 * Game with the same seed, with no dice left over from the previous game's last roll. Components are equal only by
 * ID, so states from two Games are never equal: the fields that leaked are compared instead.
 */
public class ResetTest {

    @Test
    public void startStateMatchesFreshGameAfterReuse() {
        checkReset(GameType.Backgammon);
    }

    @Test
    public void xiiScriptaStartStateMatchesFreshGameAfterReuse() {
        checkReset(GameType.XIIScripta);
    }

    private void checkReset(GameType gameType) {
        List<AbstractPlayer> players = List.of(new RandomPlayer(new Random(42)), new RandomPlayer(new Random(43)));
        Game reused = gameType.createGameInstance(2, 1L);
        for (int i = 0; i < 3; i++) {
            reused.reset(players, 100L + i);
            reused.run();
        }
        reused.reset(players, 7L);
        BGGameState state = (BGGameState) reused.getGameState();

        Game fresh = gameType.createGameInstance(2, 1L);
        fresh.reset(players, 7L);

        assertNull(state.availableDiceValues);
        assertNull(state.diceUsed);
        assertEquals(fresh.getForwardModel().computeAvailableActions(fresh.getGameState()).toString(),
                reused.getForwardModel().computeAvailableActions(state).toString());
    }
}
