package games.gofish;

import core.CoreConstants;
import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of the game (a hand or the draw deck is empty), the result, the score and the heuristic.
 */
public class GoFishGameEndTest {

    GoFishGameState state;
    GoFishForwardModel fm;

    @Before
    public void setup() {
        state = newState(3, 17);
        fm = new GoFishForwardModel();
    }

    private void assertResults(CoreConstants.GameResult... expected) {
        assertFalse("the game should have ended", state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(expected, state.getPlayerResults());
    }

    @Test
    public void aTargetHandingOverTheirLastCardsEndsTheGameAndATieForMostBooksIsADraw() {
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"));
        giveHand(state, 2, card("7H"), card("2C"));
        giveBooks(state, 0, 12);                  // P0: 1 book (Queens)
        giveBooks(state, 2, 3);                   // P2: 1 book (3s)
        assertAllCardsPresent(state);
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(0, state.playerHands.get(1).getSize());
        assertTrue("draw deck still has cards", state.drawDeck.getSize() > 0);
        // books 1, 0, 1: P0 and P2 tie for most
        assertResults(DRAW_GAME, LOSE_GAME, DRAW_GAME);
    }

    @Test
    public void anAskerLayingDownTheirLastCardsAfterAnAskEndsTheGame() {
        giveHand(state, 0, card("5H"), card("5D"), card("5C"));
        giveHand(state, 1, card("5S"), card("9C"));
        giveHand(state, 2, card("7H"), card("2C"));
        giveBooks(state, 2, 3);                   // P2: 1 book, as many as P0 will have
        giveBooks(state, 1, 4, 6);                // P1: 2 books, the most
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(4, state.playerBooks.get(0).getSize());
        assertEquals(0, state.playerHands.get(0).getSize());
        // books 1, 2, 1: P1 wins
        assertResults(LOSE_GAME, WIN_GAME, LOSE_GAME);
    }

    @Test
    public void anAskerLayingDownTheirLastCardsAfterDrawingEndsTheGame() {
        giveHand(state, 0, card("5H"), card("5D"), card("5C"));
        giveHand(state, 1, card("9S"), card("9C"));
        giveHand(state, 2, card("7H"), card("2C"));
        stackDrawDeck(state, card("5S"));
        fm.next(state, new GoFishAsk(1, 5));      // Go fish: draws the 5S, the fourth 5
        assertEquals(4, state.playerBooks.get(0).getSize());
        assertEquals(0, state.playerHands.get(0).getSize());
        // books 1, 0, 0: P0 wins (although drawing the rank asked for would otherwise give another turn)
        assertResults(WIN_GAME, LOSE_GAME, LOSE_GAME);
    }

    @Test
    public void drawingTheLastCardEndsTheGameEvenWhenItIsTheRankAskedFor() {
        GoFishGameState two = newState(2, 17);
        // every card placed: 10 ranks booked (40 cards) + 11 in hands + 1 in the draw deck = 52
        giveBooks(two, 0, 2, 3, 4, 6, 12);                                   // P0: 5 books
        giveBooks(two, 1, 7, 8, 10, 11, 14);                                 // P1: 5 books
        giveHand(two, 0, card("5H"), card("5S"), card("5C"), card("9H"), card("9S"), card("KH"));
        giveHand(two, 1, card("9D"), card("9C"), card("KD"), card("KS"), card("KC"));
        assertEquals("only the 5D is left to draw", 1, two.drawDeck.getSize());
        assertEquals(card("5D"), two.drawDeck.peek());
        assertAllCardsPresent(two);
        assertEquals(0, two.getCurrentPlayer());

        fm.next(two, new GoFishAsk(1, 5));        // Go fish: draws the 5D, the rank asked for, and books the 5s
        assertEquals(0, two.drawDeck.getSize());
        assertEquals(cards("9H", "9S", "KH"), cardSet(two.playerHands.get(0)));
        assertEquals(6 * 4, two.playerBooks.get(0).getSize());
        assertFalse("the draw deck is empty: the game is over", two.isNotTerminal());
        // books 6 - 5: P0 wins
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME}, two.getPlayerResults());
    }

    @Test
    public void theGameScoreIsTheNumberOfBooks() {
        giveBooks(state, 1, 4, 6);
        giveBooks(state, 2, 14);
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(2.0, state.getGameScore(1), 0.0);
        assertEquals(1.0, state.getGameScore(2), 0.0);
    }

    @Test
    public void theHeuristicIsBooksOverThirteenWhileTheGameRuns() {
        giveHand(state, 0, card("5H"), card("KD"), card("9C"));
        giveHand(state, 1, card("5S"), card("9D"));
        giveHand(state, 2, card("7H"), card("2C"));
        giveBooks(state, 1, 4, 6);
        assertTrue(state.isNotTerminal());
        assertEquals("no books, 3 cards in hand", 0.0, state.getHeuristicScore(0), 1e-9);
        assertEquals("2 books, 2 cards in hand", 2.0 / 13, state.getHeuristicScore(1), 1e-9);
    }
}
