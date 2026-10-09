package games.scarto;

import core.components.TarotCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.TarotCard.*;
import static core.components.TarotCard.Suit.Cups;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Full and per-player copies with the dealer's exchange, before and after the dealer's discards.
 */
public class ScartoExchangeCopyTest {

    private static final int N_COPIES = 50;

    ScartoForwardModel fm;
    ScartoGameState state;

    @Before
    public void setup() {
        // the dealer (2) will discard sword 2, baton 3 and cup 6
        fm = new ScartoForwardModel();
        state = newState(43, fm, exchange());
        putInHand(state, 2, sword(2), baton(3), cup(6));
    }

    void discard(TarotCard card) {
        assertEquals("player to act before discarding " + card, 2, state.getCurrentPlayer());
        fm.next(state, new Discard(card));
    }

    void exchangeAll() {
        discard(sword(2));
        discard(baton(3));
        discard(cup(6));
        assertEquals(0, state.getCurrentPlayer());
    }

    private static Set<TarotCard> union(ScartoGameState s, boolean scarto, int... hands) {
        Set<TarotCard> all = new HashSet<>();
        for (int p : hands)
            all.addAll(s.getPlayerHand(p).getComponents());
        if (scarto)
            all.addAll(s.getScarto().getComponents());
        return all;
    }

    @Test
    public void aNonDealersCopySharesOutTheScartoWithTheHiddenHands() {
        exchangeAll();
        Set<TarotCard> hidden = union(state, true, 1, 2);
        int changed = 0;
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            assertEquals(25, copy.getPlayerHand(1).getSize());
            assertEquals(25, copy.getPlayerHand(2).getSize());
            assertEquals(3, copy.getScarto().getSize());
            assertEquals(hidden, union(copy, true, 1, 2));
            assertAllCardsPresent(copy);
            if (!setOf(copy.getScarto()).equals(setOf(state.getScarto())))
                changed++;
        }
        // player 0 has not seen the discards: 3 of 53 hidden cards, so some copy must hold others
        assertTrue("the scarto never changed in " + N_COPIES + " copies for player 0", changed > 0);
    }

    @Test
    public void aNonDealersCopyNeverConfinesTheScartoByTheDealersVoids() {
        // the dealer discarded cup 6 and later is known void in Cups (as is player 1): the discard came before
        // play, so the scarto may still hold a Cup. Every other Cup is moved into player 0's pile (seen), so cup 6
        // can go nowhere but the scarto: every copy for player 0 must keep it there
        exchangeAll();
        for (int n = 1; n <= KING; n++) {
            if (n == 6) continue;
            TarotCard c = cup(n);
            take(state, c);
            state.cardsWon.get(0).add(c);
        }
        state.getKnownVoids().get(1).add(Cups);
        state.getKnownVoids().get(2).add(Cups);
        assertAllCardsPresent(state);
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(0);
            assertTrue("copy " + i + " moved cup 6 out of the scarto: " + copy.getScarto(),
                    copy.getScarto().contains(cup(6)));
            assertEquals(3, copy.getScarto().getSize());
        }
    }

    @Test
    public void theDealersCopyKeepsTheirHandAndTheScarto() {
        exchangeAll();
        Set<TarotCard> hidden = union(state, false, 0, 1);
        int changed = 0;
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(2);
            assertEquals(state.getPlayerHand(2).getComponents(), copy.getPlayerHand(2).getComponents());
            assertEquals(state.getScarto().getComponents(), copy.getScarto().getComponents());
            // only the two other hands are unknown to the dealer
            assertEquals(hidden, union(copy, false, 0, 1));
            if (!setOf(copy.getPlayerHand(0)).equals(setOf(state.getPlayerHand(0))))
                changed++;
        }
        assertTrue("player 0's hand never changed in " + N_COPIES + " copies for the dealer", changed > 0);
    }

    @Test
    public void midExchangeCopiesKeepTheSizesAndTheDealerKnowsTheirDiscard() {
        discard(sword(2));
        // the dealer holds 28 - 1 = 27 and the scarto 1
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState dealers = (ScartoGameState) state.copy(2);
            assertEquals(state.getPlayerHand(2).getComponents(), dealers.getPlayerHand(2).getComponents());
            assertEquals(List.of(sword(2)), dealers.getScarto().getComponents());

            ScartoGameState others = (ScartoGameState) state.copy(0);
            assertEquals(27, others.getPlayerHand(2).getSize());
            assertEquals(1, others.getScarto().getSize());
            assertEquals(25, others.getPlayerHand(1).getSize());
            assertTrue(others.isExchanging());
            assertEquals(2, others.getCurrentPlayer());
            assertAllCardsPresent(others);
        }
    }

    @Test
    public void aFullCopyMidExchangeIsEqualAndIndependent() {
        discard(sword(2));
        ScartoGameState copy = (ScartoGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertTrue(copy.isExchanging());

        fm.next(copy, new Discard(baton(3)));
        assertNotEquals(state, copy);
        // the original: 27 in hand, still holding baton 3; the scarto only sword 2; the dealer still to act
        assertEquals(27, state.getPlayerHand(2).getSize());
        assertTrue(state.getPlayerHand(2).contains(baton(3)));
        assertEquals(List.of(sword(2)), state.getScarto().getComponents());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(26, copy.getPlayerHand(2).getSize());
    }
}
