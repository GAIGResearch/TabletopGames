package games.crazyeights;

import core.CoreConstants.GameResult;
import games.crazyeights.actions.DrawCard;
import games.crazyeights.actions.Pass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.GameResult.*;
import static games.crazyeights.CZETestUtils.*;
import static org.junit.Assert.*;

/**
 * Phase B: the game ends when every player passes in succession; the fewest cards in hand wins.
 * <p>
 * With a full 52-card deck a genuine all-pass sequence cannot arise (Pass needs every other card to be in a hand,
 * and whoever holds an Eight can always play it), so the earlier passes are arranged with the test-only
 * setConsecutivePasses hook and only the final Pass is legal play.
 */
public class CZEBlockedGameTest {

    CZEGameState state;
    CZEForwardModel fm;

    @Before
    public void setup() {
        CZEParameters params = new CZEParameters();
        params.setRandomSeed(42);
        state = new CZEGameState(params, 3);
        fm = new CZEForwardModel();
        fm.setup(state);
    }

    /**
     * Player 0 to move with no playable card and nothing to draw; player 2 holds every remaining card.
     */
    private void arrangeNothingToPlayOrDrawForPlayerZero() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("2C"), card("3S"));   // 2 cards, penalty 5
        giveHand(state, 1, card("KD"));                // 1 card, penalty 10
        leaveNothingToDraw(state, 2);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new Pass(0)), fm.computeAvailableActions(state));
    }

    @Test
    public void passingIsCountedButDoesNotEndTheGameUntilEveryPlayerHasPassed() {
        arrangeNothingToPlayOrDrawForPlayerZero();
        state.setConsecutivePasses(1);

        fm.next(state, new Pass(0));

        assertEquals(2, state.getConsecutivePasses());
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void drawingResetsTheCountOfPasses() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));   // stock still has cards
        state.setConsecutivePasses(2);

        fm.next(state, new DrawCard(0));

        assertEquals(0, state.getConsecutivePasses());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void whenEveryPlayerHasPassedThePlayerWithFewestCardsWins() {
        arrangeNothingToPlayOrDrawForPlayerZero();
        state.setConsecutivePasses(2);

        fm.next(state, new Pass(0));   // third pass in succession with 3 players

        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        // fewest cards, not fewest penalty points: player 1 (1 card, 10 points) beats player 0 (2 cards, 5 points)
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void allPlayersTiedOnFewestCardsWinABlockedGame() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("2C"), card("3S"));
        giveHand(state, 1, card("KD"), card("QS"));
        leaveNothingToDraw(state, 2);
        state.setConsecutivePasses(2);

        fm.next(state, new Pass(0));

        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{WIN_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }
}
