package games.rummy;

import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import games.rummy.actions.LayOff;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The visibility of hand cards, in particular of a card taken from the discard pile while it stays in the taker's
 * hand, and its place in redeterminised copies.
 */
public class RummyKnownCardsTest {

    RummyForwardModel fm;

    @Before
    public void setup() {
        fm = new RummyForwardModel();
    }

    /** The first card of the hand that is not `not`. */
    private static FrenchCard firstOtherCard(Deck<FrenchCard> hand, FrenchCard not) {
        for (FrenchCard c : hand.getComponents())
            if (!c.equals(not)) return c;
        throw new AssertionError("no other card in hand");
    }

    private static void assertVisibleToAll(PartialObservableDeck<FrenchCard> hand, FrenchCard c, int nPlayers) {
        int i = hand.getComponents().indexOf(c);
        assertTrue(c + " is in the hand", i >= 0);
        for (int q = 0; q < nPlayers; q++)
            assertTrue(c + " visible to player " + q, hand.getVisibilityForPlayer(i, q));
    }

    @Test
    public void takingTheDiscardMakesThatCardVisibleToAllPlayers() {
        RummyGameState state = newState(3, 41, fm);
        assertHandVisibility(state, Set.of());   // the deal: every card visible to its owner only
        FrenchCard top = state.getDiscardPile().peek();
        fm.next(state, new DrawCard(true));
        assertVisibleToAll(state.getPlayerHand(0), top, 3);
        // the rest of player 0's hand and the other hands are unchanged: owner only
        assertHandVisibility(state, Set.of(top));
    }

    @Test
    public void drawingFromTheDrawDeckKeepsTheCardVisibleOnlyToItsOwner() {
        RummyGameState state = newState(3, 41, fm);
        FrenchCard top = state.getDrawDeck().peek();
        fm.next(state, new DrawCard(false));
        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(0);
        int i = hand.getComponents().indexOf(top);
        assertTrue(hand.getVisibilityForPlayer(i, 0));
        assertFalse(hand.getVisibilityForPlayer(i, 1));
        assertFalse(hand.getVisibilityForPlayer(i, 2));
        assertHandVisibility(state, Set.of());
    }

    @Test
    public void aTakenCardStaysVisibleToAllAcrossLaterTurnsAndDiscards() {
        RummyGameState state = newState(2, 42, fm);
        FrenchCard known = state.getDiscardPile().peek();
        fm.next(state, new DrawCard(true));
        // player 0 discards a different card: the turn ends, the known card is still visible to all
        fm.next(state, new Discard(firstOtherCard(state.getPlayerHand(0), known)));
        assertEquals(1, state.getCurrentPlayer());
        assertHandVisibility(state, Set.of(known));

        // player 1's turn: draw from the draw deck and discard it
        fm.next(state, new DrawCard(false));
        assertHandVisibility(state, Set.of(known));
        fm.next(state, new Discard(state.getPlayerHand(1).get(0)));
        assertEquals(0, state.getCurrentPlayer());
        assertHandVisibility(state, Set.of(known));

        // player 0 draws from the draw deck, then discards a card that sits BEFORE the known card in the hand, so the
        // known card moves down one index, and its visibility must move with it
        fm.next(state, new DrawCard(false));
        assertHandVisibility(state, Set.of(known));
        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(0);
        int knownIndex = hand.getComponents().indexOf(known);
        assertTrue("guard: a card before the known card", knownIndex > 0);
        FrenchCard before = hand.get(knownIndex - 1);
        fm.next(state, new Discard(before));
        assertEquals(knownIndex - 1, hand.getComponents().indexOf(known));
        assertVisibleToAll(hand, known, 2);
        assertHandVisibility(state, Set.of(known));
    }

    @Test
    public void meldingOrLayingOffAKnownCardRemovesItAndLeavesTheOthersVisibility() {
        RummyGameState state = newState(2, 43, fm);
        addMeld(state, "KD", "KH", "KC");
        giveHand(state, 0, "5H", "6H", "8H", "2C", "9D", "QC", "4S");
        giveHand(state, 1, "7H", "3D", "10C", "JD");
        setDiscardPile(state, "KS");
        assertAllCardsPresent(state);

        // player 0 takes the KS and discards the 2C; player 1 draws and discards the 7H; player 0 takes the 7H
        fm.next(state, new DrawCard(true));
        fm.next(state, new Discard(card("2C")));
        fm.next(state, new DrawCard(false));
        fm.next(state, new Discard(card("7H")));
        fm.next(state, new DrawCard(true));
        assertEquals(0, state.getCurrentPlayer());
        assertHandVisibility(state, Set.of(card("KS"), card("7H")));

        // meld the known 7H: it leaves the hand; the KS is still known, the others still owner only
        fm.next(state, meld("5H", "6H", "7H"));
        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(0);
        assertEquals(Set.copyOf(cards("KS", "8H", "9D", "QC", "4S")), setOf(hand));
        assertHandVisibility(state, Set.of(card("KS")));

        // lay off the known KS onto the Kings: no known card is left in the hand
        fm.next(state, layOff("KS", LayOff.Position.SET));
        assertEquals(Set.copyOf(cards("8H", "9D", "QC", "4S")), setOf(hand));
        assertHandVisibility(state, Set.of());
        assertEquals(4, hand.getVisibleComponents(0).stream().filter(c -> c != null).count());
        assertEquals(0, hand.getVisibleComponents(1).stream().filter(c -> c != null).count());
        assertAllCardsPresent(state);
    }

    @Test
    public void redeterminisationKeepsAnOpponentsKnownCardInPlace() {
        RummyGameState state = newState(3, 44, fm);
        FrenchCard known = state.getDiscardPile().peek();
        fm.next(state, new DrawCard(true));
        fm.next(state, new Discard(firstOtherCard(state.getPlayerHand(0), known)));
        assertEquals(1, state.getCurrentPlayer());
        List<FrenchCard> hand0 = new ArrayList<>(state.getPlayerHand(0).getComponents());
        int knownIndex = hand0.indexOf(known);
        List<FrenchCard> hand0Others = new ArrayList<>(hand0);
        hand0Others.remove(known);

        for (int observer : new int[]{1, 2}) {
            int third = 3 - observer;   // the player who is neither the observer nor player 0
            // hidden from the observer: player 0's cards other than the known one, the third hand and the draw deck
            Set<FrenchCard> hidden = new HashSet<>(hand0Others);
            hidden.addAll(state.getPlayerHand(third).getComponents());
            hidden.addAll(state.getDrawDeck().getComponents());

            int othersChanged = 0, drawDeckChanged = 0;
            for (int i = 0; i < 20; i++) {
                RummyGameState copy = (RummyGameState) state.copy(observer);
                PartialObservableDeck<FrenchCard> copyHand0 = copy.getPlayerHand(0);
                assertEquals("observer " + observer + " copy " + i + ": known card kept in place",
                        known, copyHand0.get(knownIndex));
                assertVisibleToAll(copyHand0, known, 3);
                assertEquals(hand0.size(), copyHand0.getSize());
                assertEquals(state.getPlayerHand(observer).getComponents(), copy.getPlayerHand(observer).getComponents());
                assertEquals(state.getPlayerHand(third).getSize(), copy.getPlayerHand(third).getSize());
                assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
                assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
                assertEquals(state.getMelds(), copy.getMelds());

                List<FrenchCard> copyOthers = new ArrayList<>(copyHand0.getComponents());
                copyOthers.remove(known);
                Set<FrenchCard> copyHidden = new HashSet<>(copyOthers);
                copyHidden.addAll(copy.getPlayerHand(third).getComponents());
                copyHidden.addAll(copy.getDrawDeck().getComponents());
                assertEquals("the same cards are hidden, only redistributed", hidden, copyHidden);
                if (!copyOthers.equals(hand0Others)) othersChanged++;
                if (!copy.getDrawDeck().getComponents().equals(state.getDrawDeck().getComponents())) drawDeckChanged++;
                assertAllCardsPresent(copy);
            }
            assertTrue("player 0's unknown cards were never reshuffled (observer " + observer + ")", othersChanged > 0);
            assertTrue("the draw deck was never reshuffled (observer " + observer + ")", drawDeckChanged > 0);
        }

        // player 0's own copy keeps the whole hand; the full copy is exact
        assertEquals(hand0, ((RummyGameState) state.copy(0)).getPlayerHand(0).getComponents());
        RummyGameState full = (RummyGameState) state.copy(-1);
        assertEquals(state, full);
        for (int p = 0; p < 3; p++)
            assertEquals(state.getPlayerHand(p).getComponents(), full.getPlayerHand(p).getComponents());
        assertEquals(state.getDrawDeck().getComponents(), full.getDrawDeck().getComponents());
        assertHandVisibility(full, Set.of(known));
    }

    @Test
    public void statesDifferingOnlyInAHandCardsVisibilityAreNotEqual() {
        RummyGameState state = newState(3, 45, fm);
        FrenchCard known = state.getDiscardPile().peek();
        fm.next(state, new DrawCard(true));
        RummyGameState copy = (RummyGameState) state.copy();
        assertEquals(state, copy);

        // in the copy, player 1 also sees one of player 0's unknown cards
        FrenchCard other = firstOtherCard(copy.getPlayerHand(0), known);
        int j = copy.getPlayerHand(0).getComponents().indexOf(other);
        copy.getPlayerHand(0).setVisibilityOfComponent(j, 1, true);
        assertFalse("the original is independent of the copy", state.getPlayerHand(0).getVisibilityForPlayer(j, 1));
        assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
        assertNotEquals(state, copy);
    }

    @Test
    public void aNewDealStartsWithNoKnownCards() {
        RummyParameters params = new RummyParameters();
        params.setParameterValue("targetScore", 50);
        RummyGameState state = newState(params, 2, 46, fm);
        FrenchCard known = state.getDiscardPile().peek();
        fm.next(state, new DrawCard(true));
        assertVisibleToAll(state.getPlayerHand(0), known, 2);

        // the next deal (player 1 first) deals fresh hands: nothing is known
        fm.deal(state, 1);
        assertHandVisibility(state, Set.of());
        assertAllCardsPresent(state);
    }
}
