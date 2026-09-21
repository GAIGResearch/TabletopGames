package games.cuckoo;

import core.CoreConstants.GameResult;
import games.cuckoo.actions.KeepCard;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of the game, the results and ordinal positions.
 * <p>
 * Each test arranges round 2 of a 4-player game in which player 1 went out in round 0 and player 2 in round 1, so
 * players 0 and 3 are left; player 3 deals and decides now.
 */
public class CuckooGameEndTest {

    CuckooGameState state;
    CuckooForwardModel fm;

    @Before
    public void setup() {
        state = newState(4, 3, 42);
        fm = new CuckooForwardModel();
        advanceRoundCounter(state, fm, 2);
        knockOut(state, 1, 0);
        knockOut(state, 2, 1);
        setDealerAndTurn(state, 3, 3);
    }

    private int[] ordinals() {
        int[] result = new int[4];
        for (int p = 0; p < 4; p++)
            result[p] = state.getOrdinalPosition(p);
        return result;
    }

    @Test
    public void theLastPlayerLeftWinsAndEveryoneElseLoses() {
        state.lives[0] = 2;
        state.lives[3] = 1;
        dealCards(state, card("9H"), null, null, card("4S"));
        fm.next(state, new KeepCard());
        // 4S is lowest: player 3 loses their last life in round 2, leaving only player 0
        assertArrayEquals(new int[]{2, 0, 0, 0}, state.lives);
        assertArrayEquals(new int[]{-1, 0, 1, 2}, state.roundEliminated);
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        GameResult[] expected = {WIN_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME};
        assertArrayEquals(expected, state.getPlayerResults());
        // by lives, then the later round out is better: 0 (2 lives), 3 (out round 2), 2 (round 1), 1 (round 0)
        assertArrayEquals(new int[]{1, 4, 3, 2}, ordinals());

        // Game.terminate() calls endGame again: the results must not change
        fm.endGame(state);
        assertArrayEquals(expected, state.getPlayerResults());
    }

    @Test
    public void playersWhoGoOutTogetherInTheFinalRoundAreJointWinners() {
        state.lives[0] = 1;
        state.lives[3] = 1;
        dealCards(state, card("4H"), null, null, card("4S"));
        fm.next(state, new KeepCard());
        // 4H and 4S tie for lowest: both lose their last life in round 2 and nobody is left
        assertArrayEquals(new int[]{0, 0, 0, 0}, state.lives);
        assertArrayEquals(new int[]{2, 0, 1, 2}, state.roundEliminated);
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        // players 0 and 3 went out together in the final round, so they are joint winners
        GameResult[] expected = {WIN_GAME, LOSE_GAME, LOSE_GAME, WIN_GAME};
        assertArrayEquals(expected, state.getPlayerResults());
        assertArrayEquals(new int[]{1, 4, 3, 1}, ordinals());

        fm.endGame(state);
        assertArrayEquals("results after Game.terminate() calls endGame again", expected, state.getPlayerResults());
    }

    @Test
    public void theGameGoesOnWhileTwoPlayersAreLeft() {
        state.lives[0] = 2;
        state.lives[3] = 2;
        dealCards(state, card("9H"), null, null, card("4S"));
        fm.next(state, new KeepCard());
        assertArrayEquals(new int[]{2, 0, 0, 1}, state.lives);
        assertTrue(state.isNotTerminal());
        assertEquals(GAME_ONGOING, state.getGameStatus());
        // old dealer 3: player 0 deals and, player 1 and 2 being out, player 3 decides first
        assertEquals(0, state.getDealer());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getRoundCounter());
        assertOneCardPerPlayerInGame(state);
    }

    @Test
    public void ordinalPositionsRankByLivesThenByTheRoundOut() {
        // arranged directly (no play): 0 has 2 lives, 3 has 1, 2 went out in round 1 and 1 in round 0
        state.lives[0] = 2;
        state.lives[3] = 1;
        assertArrayEquals(new int[]{1, 4, 3, 2}, ordinals());
        // with equal lives, the players in the game share a place
        state.lives[3] = 2;
        assertArrayEquals(new int[]{1, 4, 3, 1}, ordinals());
    }
}
