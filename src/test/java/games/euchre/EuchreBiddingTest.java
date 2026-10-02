package games.euchre;

import games.euchre.actions.CallTrump;
import games.euchre.actions.Discard;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.euchre.EuchreTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Choosing trumps: the two bidding rounds (every call offered with and without going alone), stick the dealer, the
 * dealer taking the up-card and discarding. The standard deal: up-card 9H, dealer 3 holding KD KH AC KC JC, player 0
 * (on the dealer's left) first to bid.
 */
public class EuchreBiddingTest {

    EuchreGameState state;
    EuchreForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new EuchreForwardModel();
        standardDeal(state);
        assertEquals(3, state.getDealer());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void inRoundOneEveryPlayerMayPassOrCallTheUpCardsSuit() {
        for (int p = 0; p < 4; p++) {
            assertEquals("current player", p, state.getCurrentPlayer());   // 0, 1, 2, then dealer 3
            assertTrue(state.isFirstBiddingRound());
            // Pass, Hearts, Hearts alone
            assertEquals("actions of player " + p, passAndCalls(Hearts), available(state, fm));
            pass(state, fm, 1);
            assertEquals("passes", p + 1, state.getPasses());
        }
        // all four passed: round 2, back to the dealer's left, no trumps yet and the up-card still on the kitty
        assertFalse(state.isFirstBiddingRound());
        assertNull(state.getTrumpSuit());
        assertEquals(-1, state.getMaker());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(4, state.getKitty().getSize());
        assertEquals(card("9H"), state.getKitty().peek());
    }

    @Test
    public void inRoundTwoPlayersMayPassOrCallAnyOtherSuitButTheDealerMayNotPass() {
        pass(state, fm, 4);
        for (int p = 0; p < 3; p++) {
            assertEquals("current player", p, state.getCurrentPlayer());
            // any suit but the up-card's (Hearts), each alone or not: 1 + 3 x 2 = 7 actions
            assertEquals("actions of player " + p, passAndCalls(Diamonds, Clubs, Spades), available(state, fm));
            pass(state, fm, 1);
        }
        // stick the dealer: after 4 + 3 passes, dealer 3 must call one of the three suits (and may go alone): 6 actions
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(7, state.getPasses());
        assertEquals(calls(Diamonds, Clubs, Spades), available(state, fm));
    }

    @Test
    public void aRoundOneCallMakesTheDealerTakeTheUpCardAndChooseADiscard() {
        pass(state, fm, 1);
        fm.next(state, new CallTrump(Hearts, false));      // player 1 calls
        assertEquals(Hearts, state.getTrumpSuit());
        assertEquals(1, state.getMaker());
        assertFalse(state.isAlone());

        // dealer 3: 5 + the up-card = 6 cards; the kitty keeps its other 3
        assertEquals(6, state.getPlayerHand(3).getSize());
        assertTrue(state.getPlayerHand(3).contains(card("9H")));
        assertEquals(3, state.getKitty().getSize());
        assertFalse(state.getKitty().contains(card("9H")));
        for (int p = 0; p < 3; p++)
            assertEquals(5, state.getPlayerHand(p).getSize());
        assertEquals(3, state.getCurrentPlayer());
        // any of the dealer's 6 cards, the up-card included
        assertEquals(discards("KD", "KH", "AC", "KC", "JC", "9H"), available(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealerMayCallInRoundOneAndThenDiscards() {
        pass(state, fm, 3);
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new CallTrump(Hearts, false));
        assertEquals(3, state.getMaker());
        assertEquals(Hearts, state.getTrumpSuit());
        assertEquals(6, state.getPlayerHand(3).getSize());
        assertEquals(3, state.getKitty().getSize());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(discards("KD", "KH", "AC", "KC", "JC", "9H"), available(state, fm));
    }

    @Test
    public void theDiscardGoesToTheKittyAndTheDealersLeftLeads() {
        fm.next(state, new CallTrump(Hearts, false));      // player 0 calls at once
        fm.next(state, new Discard(card("KC")));
        assertEquals(Set.copyOf(cards("KD", "KH", "AC", "JC", "9H")), Set.copyOf(cardsOf(state.getPlayerHand(3))));
        assertEquals(4, state.getKitty().getSize());
        assertTrue(state.getKitty().contains(card("KC")));
        assertEquals(card("KC"), state.getDealerDiscard());
        assertEquals(Hearts, state.getTrumpSuit());
        assertEquals(0, state.getMaker());

        // play: player 0 on the dealer's left leads the first trick, with any card
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(plays("9S", "10S", "JS", "AH", "9D"), available(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealerMayDiscardTheUpCardItself() {
        fm.next(state, new CallTrump(Hearts, false));
        fm.next(state, new Discard(card("9H")));
        assertEquals(Set.copyOf(cards("KD", "KH", "AC", "KC", "JC")), Set.copyOf(cardsOf(state.getPlayerHand(3))));
        assertEquals(4, state.getKitty().getSize());
        assertTrue(state.getKitty().contains(card("9H")));
        assertEquals(card("9H"), state.getDealerDiscard());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void aRoundTwoCallHasNoPickupAndPlayStartsAtOnce() {
        pass(state, fm, 5);                                 // round 1, then player 0 in round 2
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new CallTrump(Diamonds, false));
        assertEquals(Diamonds, state.getTrumpSuit());
        assertEquals(1, state.getMaker());
        for (int p = 0; p < 4; p++)
            assertEquals("hand " + p, 5, state.getPlayerHand(p).getSize());
        assertEquals(4, state.getKitty().getSize());
        assertEquals(card("9H"), state.getKitty().peek());  // the up-card stays (turned down) on the kitty
        assertNull(state.getDealerDiscard());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(plays("9S", "10S", "JS", "AH", "9D"), available(state, fm));
    }

    @Test
    public void theStuckDealersCallStartsPlayWithTheDealersLeft() {
        pass(state, fm, 7);
        fm.next(state, new CallTrump(Spades, false));
        assertEquals(Spades, state.getTrumpSuit());
        assertEquals(3, state.getMaker());
        assertEquals(5, state.getPlayerHand(3).getSize());
        assertEquals(4, state.getKitty().getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("9S", "10S", "JS", "AH", "9D"), available(state, fm));
    }
}
