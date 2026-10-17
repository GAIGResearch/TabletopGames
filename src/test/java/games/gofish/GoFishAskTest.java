package games.gofish;

import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * One ask: a successful ask, "Go fish", who plays next, and books laid down by either.
 * Three players; P0 to play. The draw deck is left large, so no ask here ends the game.
 */
public class GoFishAskTest {

    GoFishParameters params;
    GoFishGameState state;
    GoFishForwardModel fm;

    @Before
    public void setup() {
        params = new GoFishParameters();
        state = newState(params, 3, 17);
        fm = new GoFishForwardModel();
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"), card("5C"), card("9D"));
        giveHand(state, 2, card("7H"), card("2C"));
        stackDrawDeck(state, card("8D"));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(52 - 2 - 3 - 2, state.drawDeck.getSize());   // 45
    }

    @Test
    public void successfulAskMovesAllTheTargetsCardsOfTheRankAndTheAskerGoesAgain() {
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "KD", "5S", "5C"), cardSet(state.playerHands.get(0)));
        assertEquals(cards("9D"), cardSet(state.playerHands.get(1)));
        assertEquals(cards("7H", "2C"), cardSet(state.playerHands.get(2)));
        assertEquals("no draw after a successful ask", 45, state.drawDeck.getSize());
        assertTrue(state.isNotTerminal());
        assertEquals("continueOnSuccess (default true): P0 goes again", 0, state.getCurrentPlayer());
    }

    @Test
    public void withoutContinueOnSuccessTheTurnPassesAfterASuccessfulAsk() {
        params.setParameterValue("continueOnSuccess", false);
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "KD", "5S", "5C"), cardSet(state.playerHands.get(0)));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void goFishDrawsTheTopCardAndTheTurnPassesToTheLeft() {
        fm.next(state, new GoFishAsk(2, 5));     // P2 holds no 5
        assertEquals("P0 drew the top card, the 8D", cards("5H", "KD", "8D"), cardSet(state.playerHands.get(0)));
        assertEquals(cards("7H", "2C"), cardSet(state.playerHands.get(2)));
        assertEquals(cards("5S", "5C", "9D"), cardSet(state.playerHands.get(1)));
        assertEquals(45 - 1, state.drawDeck.getSize());
        assertTrue(state.isNotTerminal());
        assertEquals("8 is not the rank asked for: P1 (to the left) plays", 1, state.getCurrentPlayer());
    }

    @Test
    public void goFishDrawingTheRankAskedForGivesAnotherTurn() {
        stackDrawDeck(state, card("5D"));
        fm.next(state, new GoFishAsk(2, 5));
        assertEquals(cards("5H", "KD", "5D"), cardSet(state.playerHands.get(0)));
        assertEquals(45 - 1, state.drawDeck.getSize());   // stacking moved the 5D within the draw deck
        assertEquals("continueOnDrawingSameRank (default true): P0 goes again", 0, state.getCurrentPlayer());
    }

    @Test
    public void withoutContinueOnDrawingSameRankDrawingTheRankAskedForPassesTheTurn() {
        params.setParameterValue("continueOnDrawingSameRank", false);
        stackDrawDeck(state, card("5D"));
        fm.next(state, new GoFishAsk(2, 5));
        assertEquals(cards("5H", "KD", "5D"), cardSet(state.playerHands.get(0)));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void aBookCompletedByAnAskIsLaidDownAtOnce() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        // P0 has 5H 5D; P1 hands over 5S 5C: four 5s go to P0's books, the K stays in hand
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "5D", "5S", "5C"), cardSet(state.playerBooks.get(0)));
        assertEquals(4, state.playerBooks.get(0).getSize());
        assertEquals(cards("KD"), cardSet(state.playerHands.get(0)));
        assertEquals(1.0, state.getGameScore(0), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
        assertAllCardsPresent(state);
    }

    @Test
    public void aBookCompletedByTheDrawIsLaidDownAtOnce() {
        giveHand(state, 0, card("5H"), card("5D"), card("5S"), card("KD"));
        giveHand(state, 1, card("9D"), card("9S"));
        stackDrawDeck(state, card("5C"));
        // P1 holds no 5: Go fish, P0 draws the 5C, the fourth 5
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "5D", "5S", "5C"), cardSet(state.playerBooks.get(0)));
        assertEquals(4, state.playerBooks.get(0).getSize());
        assertEquals(cards("KD"), cardSet(state.playerHands.get(0)));
        assertEquals(1.0, state.getGameScore(0), 0.0);
        assertEquals("drew the rank asked for: P0 goes again", 0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }
}
