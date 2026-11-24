package games.hearts;

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
 * Game with the same seed. The first round has a passing phase, so the trick left from the end of the previous game
 * (led by its last trick winner) must not survive into it. Components are equal only by ID, so states from two Games
 * are never equal: the fields that leaked are compared instead.
 */
public class ResetTest {

    @Test
    public void startStateMatchesFreshGameAfterReuse() {
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            players.add(new RandomPlayer(new Random(42 + p)));
        Game reused = GameType.Hearts.createGameInstance(4, 1L);
        for (int i = 0; i < 3; i++) {
            reused.reset(players, 100L + i);
            reused.run();
        }
        reused.reset(players, 7L);
        HeartsGameState state = (HeartsGameState) reused.getGameState();

        Game fresh = GameType.Hearts.createGameInstance(4, 1L);
        fresh.reset(players, 7L);
        HeartsGameState freshState = (HeartsGameState) fresh.getGameState();

        assertEquals(HeartsGameState.Phase.PASSING, state.getGamePhase());
        assertEquals(freshState.currentTrick.getLeader(), state.currentTrick.getLeader());
        assertEquals(0, state.currentTrick.getSize());
        assertEquals(fresh.getForwardModel().computeAvailableActions(fresh.getGameState()).toString(),
                reused.getForwardModel().computeAvailableActions(state).toString());
        // a state equals its own copy (this used to recurse until the stack overflowed)
        assertEquals(state, state.copy());
    }
}
