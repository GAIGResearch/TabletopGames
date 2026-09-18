package games.blackjack;

import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * The dealerHitsSoft17 option. With false (the default) the dealer stands on every 17; with true the dealer
 * also draws on a soft 17 (an Ace counted 11), and still stands on a hard 17 and on any 18 or more. A soft 17 that
 * turns hard after a draw is played on by the normal rule. Each test is one player on 18 (bet 2, 8 chips left)
 * standing against the arranged dealer cards and draw deck, so the dealer's final cards also decide the chips.
 */
public class BlackjackSoft17Test {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
    }

    private void playerStandsOn18(boolean hitsSoft17, String up, String hole, String drawDeckTop) {
        params.setParameterValue("dealerHitsSoft17", hitsSoft17);
        state = new BlackjackGameState(params, 1);
        fm = new BlackjackForwardModel();
        fm.setup(state);
        arrangePlay(state, new int[]{2}, new String[]{"10C 8C"}, up, hole, drawDeckTop);
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getHoleCard().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void byDefaultTheDealerStandsOnSoftSeventeen() {
        // 6 + A = soft 17: stands, and 18 beats it: 8 + 2 + 2 = 12
        playerStandsOn18(false, "6S", "AD", "3H 9C");
        assertEquals(setOf("6S AD"), setOf(state.getDealerHand()));
        assertEquals(card("3H"), state.getDrawDeck().peek());
        assertEquals(12, state.getChips(0));
        assertArrayEquals(new Object[]{WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void withDealerHitsSoft17TheDealerDrawsOnSoftSeventeen() {
        // the pair of the test above: 6 + A = soft 17 draws the 3: soft 20, which beats 18: 8 chips
        playerStandsOn18(true, "6S", "AD", "3H 9C");
        assertEquals(setOf("6S AD 3H"), setOf(state.getDealerHand()));
        assertEquals(card("9C"), state.getDrawDeck().peek());
        assertEquals(8, state.getChips(0));
        assertArrayEquals(new Object[]{LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void aSoftSeventeenReachedByDrawingIsStoodOnOnlyByDefault() {
        // 5 + A = soft 16 draws the A: 5 + A + A = soft 17
        playerStandsOn18(false, "5S", "AD", "AH 3C 9H");
        assertEquals(setOf("5S AD AH"), setOf(state.getDealerHand()));
        assertEquals(card("3C"), state.getDrawDeck().peek());
        // with dealerHitsSoft17 the soft 17 draws the 3: soft 20, stands
        playerStandsOn18(true, "5S", "AD", "AH 3C 9H");
        assertEquals(setOf("5S AD AH 3C"), setOf(state.getDealerHand()));
        assertEquals(card("9H"), state.getDrawDeck().peek());
        assertEquals(8, state.getChips(0));
    }

    @Test
    public void aHardSeventeenIsStoodOnWithEitherSetting() {
        for (boolean hitsSoft17 : new boolean[]{false, true}) {
            // 7 + 10
            playerStandsOn18(hitsSoft17, "7S", "10D", "2H");
            assertEquals("hitsSoft17 " + hitsSoft17, setOf("7S 10D"), setOf(state.getDealerHand()));
            assertEquals(card("2H"), state.getDrawDeck().peek());
            assertEquals(12, state.getChips(0));
            // 6 + 10 = 16 draws the A: 17 with the Ace counted 1, a hard 17
            playerStandsOn18(hitsSoft17, "6S", "10D", "AH 2H");
            assertEquals("hitsSoft17 " + hitsSoft17, setOf("6S 10D AH"), setOf(state.getDealerHand()));
            assertEquals(card("2H"), state.getDrawDeck().peek());
            assertEquals(12, state.getChips(0));
        }
    }

    @Test
    public void withDealerHitsSoft17ASoftEighteenIsStoodOn() {
        // 7 + A = soft 18: no draw, and it pushes the player's 18
        playerStandsOn18(true, "7S", "AD", "2H");
        assertEquals(setOf("7S AD"), setOf(state.getDealerHand()));
        assertEquals(card("2H"), state.getDrawDeck().peek());
        assertEquals(10, state.getChips(0));
    }

    @Test
    public void withDealerHitsSoft17ASoftSeventeenThatTurnsHardIsPlayedByTheNormalRule() {
        // 6 + A = soft 17 draws the 9: hard 16 (the Ace now 1), so it draws the 2: 18, stands
        playerStandsOn18(true, "6S", "AD", "9H 2C 5S");
        assertEquals(setOf("6S AD 9H 2C"), setOf(state.getDealerHand()));
        assertEquals(card("5S"), state.getDrawDeck().peek());
        assertEquals(10, state.getChips(0));
        // 6 + A = soft 17 draws the 10: hard 17, stands
        playerStandsOn18(true, "6S", "AD", "10H 5C");
        assertEquals(setOf("6S AD 10H"), setOf(state.getDealerHand()));
        assertEquals(card("5C"), state.getDrawDeck().peek());
        assertEquals(12, state.getChips(0));
    }

    @Test
    public void withDealerHitsSoft17TheDealerStillDoesNotDrawWhenEveryHandIsBust() {
        // the player hits 10 + 8 with the K (bust); the dealer's soft 17 would otherwise draw the 3
        params.setParameterValue("dealerHitsSoft17", true);
        state = new BlackjackGameState(params, 1);
        fm = new BlackjackForwardModel();
        fm.setup(state);
        arrangePlay(state, new int[]{2}, new String[]{"10C 8C"}, "6S", "AD", "KH 3H");
        fm.next(state, new Hit());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("6S AD"), setOf(state.getDealerHand()));
        assertEquals(card("3H"), state.getDrawDeck().peek());
        assertEquals(8, state.getChips(0));
    }
}
