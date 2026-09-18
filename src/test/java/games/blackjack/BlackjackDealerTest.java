package games.blackjack;

import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * The dealer's play once the last player has finished: turn the hole card up, draw while under 17, stand on any 17
 * (soft 17 included, by default), and draw nothing when every player hand is bust.
 */
public class BlackjackDealerTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        state = new BlackjackGameState(params, 1);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    /**
     * One player on 18 (bet 2) stands against the given dealer cards and draw deck.
     */
    private void playerStandsOn18(String up, String hole, String drawDeckTop) {
        arrangePlay(state, new int[]{2}, new String[]{"10C 8C"}, up, hole, drawDeckTop);
        fm.next(state, new Stand());
    }

    @Test
    public void theHoleCardIsTurnedUpAfterTheLastPlayer() {
        playerStandsOn18("9S", "10D", "2H");        // 19: no draw
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(setOf("9S 10D"), setOf(state.getDealerHand()));
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealerDrawsWhileUnderSeventeen() {
        // 3 + 10 = 13, +2 = 15, +5 = 20: stops, the 9 stays on the draw deck
        playerStandsOn18("3S", "10D", "2H 5C 9H");
        assertEquals(setOf("3S 10D 2H 5C"), setOf(state.getDealerHand()));
        assertEquals(card("9H"), state.getDrawDeck().peek());
        assertFalse(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void aSoftHandThatTurnsHardKeepsDrawing() {
        // 5 + A = soft 16, +10 = hard 16 (the Ace now counts 1), +2 = 18: stops
        playerStandsOn18("5S", "AD", "10H 2C 9H");
        assertEquals(setOf("5S AD 10H 2C"), setOf(state.getDealerHand()));
        assertEquals(card("9H"), state.getDrawDeck().peek());
    }

    @Test
    public void theDealerStandsOnSoftSeventeen() {
        // 6 + A = soft 17
        playerStandsOn18("6S", "AD", "5H");
        assertEquals(setOf("6S AD"), setOf(state.getDealerHand()));
        assertEquals(card("5H"), state.getDrawDeck().peek());
    }

    @Test
    public void theDealerStandsOnASoftSeventeenReachedByDrawing() {
        // 5 + A = soft 16, + A = soft 17
        playerStandsOn18("5S", "AD", "AH 3C");
        assertEquals(setOf("5S AD AH"), setOf(state.getDealerHand()));
        assertEquals(card("3C"), state.getDrawDeck().peek());
    }

    @Test
    public void theDealerStandsOnHardSeventeen() {
        playerStandsOn18("7S", "10D", "2H");
        assertEquals(setOf("7S 10D"), setOf(state.getDealerHand()));
        assertEquals(card("2H"), state.getDrawDeck().peek());
    }

    @Test
    public void whenEveryPlayerIsBustTheDealerTurnsTheHoleCardButDoesNotDraw() {
        // 3 players all bust; the dealer's 3 + 10 = 13 would otherwise draw the 4
        state = new BlackjackGameState(params, 3);
        fm.setup(state);
        arrangePlay(state, new int[]{2, 2, 2}, new String[]{"10H 6C", "9S 8D", "7C 7D"}, "3S", "10D", "KH QC JS 4H");
        fm.next(state, new Hit());      // 26
        fm.next(state, new Hit());      // 27
        fm.next(state, new Hit());      // 24
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(setOf("3S 10D"), setOf(state.getDealerHand()));
        assertEquals(card("4H"), state.getDrawDeck().peek());
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealerDrawsIfAnyPlayerIsNotBust() {
        // as above, but player 1 stands on 17: the dealer draws the 4 (17) and stops
        state = new BlackjackGameState(params, 3);
        fm.setup(state);
        arrangePlay(state, new int[]{2, 2, 2}, new String[]{"10H 6C", "9S 8D", "7C 7D"}, "3S", "10D", "KH JS 4H 9C");
        fm.next(state, new Hit());      // 26
        fm.next(state, new Stand());    // 17
        fm.next(state, new Hit());      // 24
        assertEquals(setOf("3S 10D 4H"), setOf(state.getDealerHand()));
        assertEquals(card("9C"), state.getDrawDeck().peek());
    }
}
