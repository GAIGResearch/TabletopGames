package games.president;

import core.CoreConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of a single-deal game, when only one player holds cards: the finishing order, scores, ordinal positions
 * and results.
 */
public class PresidentGameEndTest {

    PresidentForwardModel fm;

    @Before
    public void setup() {
        fm = new PresidentForwardModel();
    }

    @Test
    public void theGameEndsWhenOnlyOnePlayerHoldsCards() {
        // 4 players: 0 (2 points) and 2 (1 point) are out; player 3 leads a 5 and player 1 goes out on it with a 6
        PresidentGameState state = newState(4, 41, fm);
        arrangeOut(state, 0, 2);
        state.playerScores[0] = 2;
        state.playerScores[2] = 1;
        giveHand(state, 3, "5H", "8C");
        giveHand(state, 1, "6D");
        state.setTurnOwner(3);

        play(state, fm, 3, 5, 1);
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());   // 0 is out
        play(state, fm, 1, 6, 1);

        // only player 3 holds cards: they are the Scum, last in the finishing order
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(List.of(0, 2, 1, 3), state.getFinishingOrder());
        // scores: 0 -> 2, 2 -> 1, 1 and 3 -> 0
        assertEquals(2, state.getPlayerScore(0));
        assertEquals(0, state.getPlayerScore(1));
        assertEquals(1, state.getPlayerScore(2));
        assertEquals(0, state.getPlayerScore(3));
        // players 1 and 3 tie on 0: player 1 went out first, so is 3rd; the Scum is 4th
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(2));
        assertEquals(3, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(3));
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void ordinalPositionsFollowTheFinishingOrder() {
        // 5 players: 4, 1 and 0 are out in that order; player 2 leads their last card, leaving only player 3
        PresidentGameState state = newState(5, 42, fm);
        arrangeOut(state, 4, 1, 0);
        state.playerScores[4] = 2;
        state.playerScores[1] = 1;
        giveHand(state, 2, "3C");
        giveHand(state, 3, "5D", "9H");
        state.setTurnOwner(2);

        play(state, fm, 2, 3, 1);

        assertFalse(state.isNotTerminal());
        assertEquals(List.of(4, 1, 0, 2, 3), state.getFinishingOrder());
        // the fourth out and the Scum score 0
        assertEquals(0, state.getPlayerScore(2));
        assertEquals(0, state.getPlayerScore(3));
        // 0, 2 and 3 all have 0 points: ranked 3rd, 4th, 5th by finishing order
        assertEquals(1, state.getOrdinalPosition(4));
        assertEquals(2, state.getOrdinalPosition(1));
        assertEquals(3, state.getOrdinalPosition(0));
        assertEquals(4, state.getOrdinalPosition(2));
        assertEquals(5, state.getOrdinalPosition(3));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME, WIN_GAME},
                state.getPlayerResults());
    }
}
