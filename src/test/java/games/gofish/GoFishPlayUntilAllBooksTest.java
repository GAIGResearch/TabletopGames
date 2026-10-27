package games.gofish;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * The playUntilAllBooks variant on arranged states: play goes on past an empty hand or draw deck, "Go fish" on an
 * empty draw deck, the empty-handed player's draw or skip, the end when at most one player holds cards (which
 * includes the 13th book), and no empty-handed target.
 */
public class GoFishPlayUntilAllBooksTest {

    GoFishGameState state;
    GoFishForwardModel fm;

    @Before
    public void setup() {
        state = newState(playUntilAllBooks(), 3, 17);
        fm = new GoFishForwardModel();
        assertTrue(((GoFishParameters) state.getGameParameters()).playUntilAllBooks);
        assertEquals(0, state.getCurrentPlayer());
    }

    private static Set<AbstractAction> asks(GoFishAsk... asks) {
        return new HashSet<>(List.of(asks));
    }

    // Rule 1: neither an empty hand nor an empty draw deck ends the game

    @Test
    public void aTargetHandingOverTheirLastCardsDoesNotEndTheGame() {
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"));
        giveHand(state, 2, card("7H"), card("2C"));
        int deck = 52 - 2 - 1 - 2;                // 47
        assertEquals(deck, state.drawDeck.getSize());

        fm.next(state, new GoFishAsk(1, 5));      // success: P1 hands over the 5S, their last card
        assertEquals(0, state.playerHands.get(1).getSize());
        assertTrue("P1's hand is empty but the variant plays on", state.isNotTerminal());
        assertEquals("P0 takes another turn after a successful ask", 0, state.getCurrentPlayer());
        assertEquals(cards("5H", "5S", "KD"), cardSet(state.playerHands.get(0)));
        assertEquals("nothing drawn", deck, state.drawDeck.getSize());
    }

    @Test
    public void drawingTheLastCardDoesNotEndTheGame() {
        // kept ranks 5, 9, K; the other 10 ranks are P2's books (40 cards). 11 cards in hands + 1 to draw = 12
        bookAllRanksExcept(state, 2, 5, 9, 13);
        giveHand(state, 0, card("5H"), card("9H"), card("KH"));
        giveHand(state, 1, card("5S"), card("9S"));
        giveHand(state, 2, card("5C"), card("9C"), card("KC"), card("KS"), card("5D"), card("KD"));
        assertEquals("only the 9D is left to draw", 1, state.drawDeck.getSize());
        assertEquals(card("9D"), state.drawDeck.peek());
        assertAllCardsPresent(state);

        fm.next(state, new GoFishAsk(1, 13));     // Go fish: draws the 9D, not a King, so the turn passes
        assertEquals(0, state.drawDeck.getSize());
        assertEquals(cards("5H", "9H", "KH", "9D"), cardSet(state.playerHands.get(0)));
        assertTrue("the draw deck is empty but the variant plays on", state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
    }

    // Rule 2: "Go fish" with an empty draw deck

    /**
     * Kept ranks 5, 9, K, every card in a hand (10 ranks booked by P2 = 40, hands 3 + 3 + 6 = 12): the draw deck is
     * empty, and P1 has no Kings.
     */
    private void arrangeEmptyDrawDeck() {
        bookAllRanksExcept(state, 2, 5, 9, 13);
        giveHand(state, 0, card("5H"), card("9H"), card("KH"));
        giveHand(state, 1, card("5S"), card("9S"), card("9D"));
        giveHand(state, 2, card("5C"), card("9C"), card("KC"), card("KS"), card("5D"), card("KD"));
        assertEquals(0, state.drawDeck.getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void goFishOnAnEmptyDrawDeckDrawsNothingAndPassesTheTurnToTheLeft() {
        arrangeEmptyDrawDeck();
        fm.next(state, new GoFishAsk(1, 13));     // P1 has no Kings: Go fish, with nothing to draw
        assertEquals("P0 draws nothing", cards("5H", "9H", "KH"), cardSet(state.playerHands.get(0)));
        assertEquals(3, state.playerHands.get(1).getSize());
        assertEquals(6, state.playerHands.get(2).getSize());
        assertAllCardsPresent(state);
        assertTrue(state.isNotTerminal());
        assertEquals("no extra turn: the turn passes to the left", 1, state.getCurrentPlayer());
    }

    @Test
    public void goFishOnAnEmptyDrawDeckRecordsTheTargetVoidShowsTheAskersCardAndKeepsTheAskersVoids() {
        arrangeEmptyDrawDeck();
        state.getKnownVoids().record(0, 7);       // true: the 7s are in P2's books
        fm.next(state, new GoFishAsk(1, 13));
        assertEquals("the target is known void in Kings", Set.of(13), state.getKnownVoids().get(1));
        assertEquals("nothing was drawn, so the asker's voids are kept", Set.of(7), state.getKnownVoids().get(0));
        assertVisibleToAll(state, 0, card("KH"));
        assertVisibleOnlyToOwner(state, 0, card("5H"));
    }

    // Rule 3: an empty-handed player about to take a turn draws one card, or is skipped when the draw deck is empty

    @Test
    public void anEmptyHandedNextPlayerDrawsOneCardPrivatelyForgetsTheirVoidsAndTakesTheirTurn() {
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1);
        giveHand(state, 2, card("7H"), card("2C"));
        stackDrawDeck(state, card("3C"), card("8S"));   // P0 draws the 3C, then P1 the 8S
        state.getKnownVoids().record(1, 9);             // true: P1 holds nothing
        int deck = 52 - 2 - 0 - 2;                      // 48
        assertEquals(deck, state.drawDeck.getSize());

        fm.next(state, new GoFishAsk(2, 5));            // Go fish: P0 draws the 3C (not a 5), the turn passes to P1
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(cards("8S"), cardSet(state.playerHands.get(1)));
        assertVisibleOnlyToOwner(state, 1, card("8S"));
        assertEquals("a private draw forgets P1's voids", Set.of(), state.getKnownVoids().get(1));
        assertEquals("P0 and P1 drew one each", deck - 2, state.drawDeck.getSize());
        assertEquals(cards("5H", "KD", "3C"), cardSet(state.playerHands.get(0)));
    }

    @Test
    public void anAskerWhoEmptiesTheirHandButHasAnotherTurnDrawsOneCardAndStaysCurrent() {
        giveHand(state, 0, card("5H"), card("5D"), card("5C"));
        giveHand(state, 1, card("5S"), card("9C"));
        giveHand(state, 2, card("7H"), card("2C"));
        stackDrawDeck(state, card("8S"));
        state.getKnownVoids().record(0, 10);            // true: P0 holds no 10s
        int deck = 52 - 3 - 2 - 2;                      // 45

        fm.next(state, new GoFishAsk(1, 5));            // success: the 5S completes P0's book of 5s, P0 goes again
        assertEquals(4, state.playerBooks.get(0).getSize());
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(cards("8S"), cardSet(state.playerHands.get(0)));
        assertVisibleOnlyToOwner(state, 0, card("8S"));
        assertEquals("the draw forgets P0's voids", Set.of(), state.getKnownVoids().get(0));
        assertEquals(deck - 1, state.drawDeck.getSize());
    }

    @Test
    public void anEmptyHandedNextPlayerIsSkippedWhenTheDrawDeckIsEmpty() {
        GoFishGameState four = newState(playUntilAllBooks(), 4, 17);
        // kept ranks 5, 9: 11 ranks booked by P3 (44 cards), hands 2 + 0 + 3 + 3 = 8
        bookAllRanksExcept(four, 3, 5, 9);
        giveHand(four, 0, card("5H"), card("9H"));
        giveHand(four, 1);
        giveHand(four, 2, card("5S"), card("5C"), card("5D"));
        giveHand(four, 3, card("9S"), card("9C"), card("9D"));
        assertEquals(0, four.drawDeck.getSize());
        assertAllCardsPresent(four);

        fm.next(four, new GoFishAsk(3, 5));             // Go fish on an empty draw deck: the turn passes to P1
        assertTrue(four.isNotTerminal());
        assertEquals("P1 has no cards and none to draw: skipped", 2, four.getCurrentPlayer());
        assertEquals(0, four.playerHands.get(1).getSize());
    }

    @Test
    public void consecutiveEmptyHandedPlayersAreAllSkippedWhenTheDrawDeckIsEmpty() {
        GoFishGameState five = newState(playUntilAllBooks(), 5, 17);
        // kept ranks 5, 9: 11 ranks booked by P4 (44 cards), hands 2 + 0 + 0 + 3 + 3 = 8
        bookAllRanksExcept(five, 4, 5, 9);
        giveHand(five, 0, card("5H"), card("9H"));
        giveHand(five, 1);
        giveHand(five, 2);
        giveHand(five, 3, card("9S"), card("9C"), card("9D"));
        giveHand(five, 4, card("5S"), card("5C"), card("5D"));
        assertEquals(0, five.drawDeck.getSize());
        assertAllCardsPresent(five);

        fm.next(five, new GoFishAsk(3, 5));             // Go fish on an empty draw deck: the turn passes to P1
        assertTrue(five.isNotTerminal());
        assertEquals("P1 and P2 have no cards and none to draw: both skipped", 3, five.getCurrentPlayer());
    }

    @Test
    public void anAskerWhoEmptiesTheirHandWithAnotherTurnDueIsSkippedWhenTheDrawDeckIsEmpty() {
        // kept ranks 5, 9: 11 ranks booked by P2 (44 cards), hands 3 + 2 + 3 = 8
        bookAllRanksExcept(state, 2, 5, 9);
        giveHand(state, 0, card("5H"), card("5D"), card("5C"));
        giveHand(state, 1, card("5S"), card("9C"));
        giveHand(state, 2, card("9S"), card("9D"), card("9H"));
        assertEquals(0, state.drawDeck.getSize());
        assertAllCardsPresent(state);

        fm.next(state, new GoFishAsk(1, 5));            // success: P0 books the 5s and is due another turn, empty-handed
        assertEquals(0, state.playerHands.get(0).getSize());
        assertTrue("P1 and P2 still hold cards", state.isNotTerminal());
        assertEquals("P0 is skipped: the turn passes to P1", 1, state.getCurrentPlayer());
    }

    // Rule 4: the game ends when at most one player holds cards (so also at the 13th book)

    @Test
    public void theGameEndsWhenTheThirteenthBookIsLaidDown() {
        giveBooks(state, 0, 2, 3, 4, 6);                // P0: 4 books
        giveBooks(state, 1, 7, 8, 10, 11, 12);          // P1: 5 books
        giveBooks(state, 2, 9, 13, 14);                 // P2: 3 books; 12 ranks booked, the 5s left
        giveHand(state, 0, card("5H"), card("5D"), card("5C"));
        giveHand(state, 1, card("5S"));
        giveHand(state, 2);
        assertEquals(0, state.drawDeck.getSize());
        assertAllCardsPresent(state);

        fm.next(state, new GoFishAsk(1, 5));            // P0 completes the 13th book
        assertFalse("all 13 books are down: the game is over", state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        // books 4 + 1 = 5, 5, 3: P0 and P1 tie for most
        assertArrayEquals(new CoreConstants.GameResult[]{DRAW_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameEndsWhenOnlyOnePlayerHasCardsEvenWithCardsLeftToDraw() {
        giveBooks(state, 0, 2, 3, 4, 6);                // P0: 4 books
        giveBooks(state, 1, 7, 8, 10, 11, 12, 13, 14);  // P1: 7 books
        giveHand(state, 0, card("5H"), card("5D"), card("9H"), card("9S"), card("9C"));
        giveHand(state, 1, card("5S"), card("5C"));
        giveHand(state, 2);
        assertEquals("only the 9D is left to draw", 1, state.drawDeck.getSize());
        assertAllCardsPresent(state);

        fm.next(state, new GoFishAsk(1, 5));            // P0 takes the 5S 5C and books the 5s; P1 is left empty-handed
        assertEquals(cards("9H", "9S", "9C"), cardSet(state.playerHands.get(0)));
        assertFalse("only P0 holds cards: the game is over", state.isNotTerminal());
        // books 4 + 1 = 5, 7, 0: P1 wins
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    // Rule 5: a player with no cards is never a target

    @Test
    public void aPlayerWithNoCardsIsNotOfferedAsATarget() {
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1);
        giveHand(state, 2, card("7H"), card("2C"));
        assertEquals(asks(new GoFishAsk(2, 5), new GoFishAsk(2, 13)),
                new HashSet<>(fm.computeAvailableActions(state)));
    }
}
