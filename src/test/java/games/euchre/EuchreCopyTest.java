package games.euchre;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static core.components.FrenchCard.Suite.*;
import static games.euchre.EuchreTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies, equality and redeterminisation. The standard deal (up-card 9H, dealer 3), with the position after the
 * bidding arranged by EuchreTestUtils.startPlay.
 */
public class EuchreCopyTest {

    static final int COPIES = 50;

    EuchreGameState state;
    EuchreForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new EuchreForwardModel();
        standardDeal(state);
    }

    private static Set<FrenchCard> hiddenFrom(EuchreGameState s, int observer) {
        Set<FrenchCard> cards = new HashSet<>(s.getKitty().getComponents());
        for (int p = 0; p < 4; p++)
            if (p != observer) cards.addAll(s.getPlayerHand(p).getComponents());
        return cards;
    }

    /**
     * What every redeterminised copy for the observer must keep: their own hand, the deck sizes, the cards hidden
     * from them (as a set), the trick and discard pile, and the public fields.
     */
    private void assertKeepsWhatTheObserverKnows(EuchreGameState copy, int observer) {
        assertEquals(cardsOf(state.getPlayerHand(observer)), cardsOf(copy.getPlayerHand(observer)));
        for (int p = 0; p < 4; p++)
            assertEquals("hand size " + p, state.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
        assertEquals("kitty size", state.getKitty().getSize(), copy.getKitty().getSize());
        assertEquals(hiddenFrom(state, observer), hiddenFrom(copy, observer));
        assertEquals(cardsOf(state.getCurrentTrick()), cardsOf(copy.getCurrentTrick()));
        assertEquals(cardsOf(state.getDiscardPile()), cardsOf(copy.getDiscardPile()));
        assertEquals(state.getUpCard(), copy.getUpCard());
        assertEquals(state.getDealerDiscard(), copy.getDealerDiscard());
        assertEquals(state.getTrumpSuit(), copy.getTrumpSuit());
        assertAllCardsPresent(copy);
    }

    @Test
    public void aFullCopyIsEqualAndEveryFieldCountsForEquality() {
        startPlay(state, Hearts, 1, "KC");
        // non-zero scores, so a copy that drops them is not equal by chance
        state.teamPoints[0] = 3;
        state.tricksTaken[2] = 1;
        EuchreGameState copy = (EuchreGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // each change to a fresh full copy makes it differ from the original
        Consumer<Consumer<EuchreGameState>> differs = change -> {
            EuchreGameState c = (EuchreGameState) state.copy();
            change.accept(c);
            assertNotEquals(state, c);
        };
        differs.accept(c -> c.passes++);
        differs.accept(c -> c.maker = 2);
        differs.accept(c -> c.trumpSuit = Spades);
        differs.accept(c -> c.dealerDiscard = card("KD"));
        differs.accept(c -> c.alone = true);
        differs.accept(c -> c.teamPoints[1] = 1);
        differs.accept(c -> c.tricksTaken[0] = 1);
        differs.accept(c -> c.upCard = card("10H"));
        differs.accept(c -> c.knownVoids.get(2).add(Spades));
        // and the change is not shared with the original
        assertEquals(copy, state);
    }

    @Test
    public void duringBiddingTheUpCardStaysOnTopOfTheKitty() {
        int p1Changed = 0;
        for (int i = 0; i < COPIES; i++) {
            for (int observer : new int[]{0, 3}) {   // a non-dealer and the dealer
                EuchreGameState copy = (EuchreGameState) state.copy(observer);
                assertKeepsWhatTheObserverKnows(copy, observer);
                assertEquals(card("9H"), copy.getKitty().peek());
                if (observer == 0 && !Set.copyOf(cardsOf(copy.getPlayerHand(1)))
                        .equals(Set.copyOf(cardsOf(state.getPlayerHand(1)))))
                    p1Changed++;
            }
        }
        assertTrue("player 1's hand was never redeterminised", p1Changed > 0);
    }

    @Test
    public void afterARoundTwoCallTheUpCardStaysInTheKitty() {
        startPlay(state, Diamonds, 1, null);
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(0);
            assertKeepsWhatTheObserverKnows(copy, 0);
            assertEquals(card("9H"), copy.getKitty().peek());
        }
    }

    @Test
    public void onceTakenTheUpCardStaysWithTheDealerAndOnlyTheDealerKnowsTheDiscard() {
        startPlay(state, Hearts, 1, "KC");               // dealer 3 took the 9H and discarded the KC
        int discardMovedForOthers = 0, kittyChangedForDealer = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(1);
            assertKeepsWhatTheObserverKnows(copy, 1);
            assertTrue(copy.getPlayerHand(3).contains(card("9H")));
            if (!copy.getKitty().contains(card("KC")))
                discardMovedForOthers++;

            EuchreGameState dealerCopy = (EuchreGameState) state.copy(3);
            assertKeepsWhatTheObserverKnows(dealerCopy, 3);
            assertTrue(dealerCopy.getKitty().contains(card("KC")));
            if (!Set.copyOf(cardsOf(dealerCopy.getKitty())).equals(Set.copyOf(cardsOf(state.getKitty()))))
                kittyChangedForDealer++;
        }
        // a non-dealer does not know the discard: it can be anywhere hidden
        assertTrue("the discard never left the kitty in a non-dealer's copy", discardMovedForOthers > 0);
        // the dealer knows only their own discard in the kitty: the other 3 kitty cards are redeterminised
        assertTrue("the dealer's copy never changed the rest of the kitty", kittyChangedForDealer > 0);
    }

    @Test
    public void anUpCardTheDealerDiscardedIsNotFixedForTheOthers() {
        startPlay(state, Hearts, 1, "9H");               // dealer 3 took the 9H and discarded it again
        int upCardMoved = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(1);
            assertKeepsWhatTheObserverKnows(copy, 1);
            if (!copy.getKitty().contains(card("9H")))
                upCardMoved++;

            EuchreGameState dealerCopy = (EuchreGameState) state.copy(3);
            assertKeepsWhatTheObserverKnows(dealerCopy, 3);
            assertTrue(dealerCopy.getKitty().contains(card("9H")));
        }
        // the others cannot tell whether the dealer kept the up-card
        assertTrue("the discarded up-card never left the kitty in a non-dealer's copy", upCardMoved > 0);
    }

    @Test
    public void aKnownVoidIsRespectedAndOtherwiseAnySuitCanArrive() {
        startPlay(state, Diamonds, 1, null);
        // player 2 holds no spades. With nothing known, a copy for player 0 sometimes gives them one
        int spadesToPlayer2 = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(0);
            if (copy.getPlayerHand(2).getComponents().stream().anyMatch(c -> c.suite == Spades))
                spadesToPlayer2++;
        }
        assertTrue("player 2 never given a spade", spadesToPlayer2 > 0);

        state.knownVoids.get(2).add(Spades);
        int p2Changed = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(0);
            assertKeepsWhatTheObserverKnows(copy, 0);
            for (FrenchCard c : copy.getPlayerHand(2).getComponents())
                assertNotEquals("player 2 given " + c, Spades, c.suite);
            if (!Set.copyOf(cardsOf(copy.getPlayerHand(2))).equals(Set.copyOf(cardsOf(state.getPlayerHand(2)))))
                p2Changed++;
        }
        // the allowed (non-spade) cards still reach player 2
        assertTrue("player 2's hand was never redeterminised", p2Changed > 0);
    }

    @Test
    public void aLoneDealCopiesEquallyAndKeepsThePlayerSittingOut() {
        startPlayAlone(state, Diamonds, 2, null);        // player 0 sits out
        EuchreGameState copy = (EuchreGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertTrue(copy.isAlone());
        assertEquals(0, copy.getSittingOut());
        assertEquals(0, copy.getCurrentTrick().getSittingOut());
        EuchreGameState redeterminised = (EuchreGameState) state.copy(1);
        assertEquals(0, redeterminised.getCurrentTrick().getSittingOut());
    }

    @Test
    public void theSittingOutPartnersHandIsStillHiddenFromTheOthers() {
        startPlayAlone(state, Diamonds, 2, null);        // player 0 sits out, holding 9S 10S JS AH 9D
        int p0Changed = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(1);
            assertKeepsWhatTheObserverKnows(copy, 1);
            if (!Set.copyOf(cardsOf(copy.getPlayerHand(0))).equals(Set.copyOf(cardsOf(state.getPlayerHand(0)))))
                p0Changed++;
            // the partner sitting out keeps their own hand
            assertKeepsWhatTheObserverKnows((EuchreGameState) state.copy(0), 0);
        }
        assertTrue("the sitting-out partner's hand was never redeterminised", p0Changed > 0);
    }
}
