package games.cuckoo;

import core.components.FrenchCard;
import games.cuckoo.actions.KeepCard;
import games.cuckoo.actions.SwapCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies of the state: a faithful copy, and a redeterminised copy from one player's point of view.
 * The redeterminised copy keeps every card the observer knows where it is (see CuckooKnowledgeTest for what is known).
 */
public class CuckooCopyTest {

    @Test
    public void aFullCopyIsEqualAndIndependent() {
        CuckooGameState state = newState(5, 3, 11);
        knockOut(state, 2, 0);
        dealCards(state, card("5H"), card("9C"), null, card("3S"), card("KD"));
        CuckooGameState copy = (CuckooGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // change the copy: the original is untouched
        copy.drawDeck.add(copy.playerCards.get(0).draw());
        copy.lives[1] = 0;
        copy.dealer = 0;
        assertEquals(card("5H"), state.getPlayerCard(0));
        assertEquals(52 - 4, state.drawDeck.getSize());
        assertEquals(3, state.getLives(1));
        assertEquals(4, state.getDealer());
        assertNotEquals(state, copy);
    }

    @Test
    public void aRedeterminisedCopyKeepsTheObserversCardAndThePublicInformation() {
        CuckooGameState state = newState(5, 3, 11);
        knockOut(state, 2, 0);
        state.lives[4] = 1;
        dealCards(state, card("5H"), card("9C"), null, card("3S"), card("KD"));
        int changed = 0;
        for (int observer : new int[]{0, 1, 3, 4}) {
            for (int i = 0; i < 20; i++) {
                CuckooGameState copy = (CuckooGameState) state.copy(observer);
                assertEquals("observer's own card", state.getPlayerCard(observer), copy.getPlayerCard(observer));
                assertArrayEquals(state.lives, copy.lives);
                assertArrayEquals(state.roundEliminated, copy.roundEliminated);
                assertEquals(state.getDealer(), copy.getDealer());
                for (int p = 0; p < 5; p++)
                    assertEquals("cards held by " + p, state.playerCards.get(p).getSize(), copy.playerCards.get(p).getSize());
                assertEquals(state.drawDeck.getSize(), copy.drawDeck.getSize());
                assertAllCardsPresent(copy);
                for (int p = 0; p < 5; p++)
                    if (p != observer && p != 2 && !state.getPlayerCard(p).equals(copy.getPlayerCard(p)))
                        changed++;
            }
        }
        assertTrue("other players' cards are never redeterminised", changed > 0);
    }

    @Test
    public void twoStatesDifferingOnlyInKnowledgeAreNotEqual() {
        CuckooGameState state = newState(4, 3, 11);
        CuckooForwardModel fm = new CuckooForwardModel();
        dealCards(state, card("5H"), card("KS"), card("3S"), card("8D"));
        CuckooGameState other = (CuckooGameState) state.copy();
        assertEquals(state, other);
        // P0's swap is refused (P1 holds a King) in one, P0 keeps in the other: the cards, turn and round are the
        // same, but in the first everyone has seen P1's King
        fm.next(state, new SwapCard());
        fm.next(other, new KeepCard());
        assertEquals(state.playerCards, other.playerCards);
        assertEquals(state.drawDeck, other.drawDeck);
        assertEquals(state.getCurrentPlayer(), other.getCurrentPlayer());
        assertTrue(state.knowsCard(2, 1));
        assertFalse(other.knowsCard(2, 1));
        assertNotEquals(state, other);
        assertNotEquals("knowledge is part of the hash", state.hashCode(), other.hashCode());
    }

    @Test
    public void aFullCopyKeepsTheKnowledgeAndItsKnowledgeIsIndependent() {
        CuckooGameState state = newState(4, 3, 11);
        CuckooForwardModel fm = new CuckooForwardModel();
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());   // P0 and P1 exchange and know both cards
        CuckooGameState copy = (CuckooGameState) state.copy();
        assertKnowledge(copy, "1100", "1100", "0010", "0001");
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        fm.next(copy, new SwapCard());    // P1 and P2 exchange in the copy only
        assertKnowledge(copy, "1010", "1110", "0110", "0001");
        assertKnowledge(state, "1100", "1100", "0010", "0001");
    }

    /**
     * Makes many redeterminised copies for the observer and checks that the given players' cards never move, every
     * other player's card changes in some copy, and the deck sizes, the 52 cards and the knowledge are kept.
     */
    private static void assertRedeterminisationKeeps(CuckooGameState state, int observer, int... knownHolders) {
        Set<Integer> known = new HashSet<>();
        for (int h : knownHolders) known.add(h);
        int n = state.getNPlayers();
        int[] changed = new int[n];
        for (int i = 0; i < 50; i++) {
            CuckooGameState copy = (CuckooGameState) state.copy(observer);
            assertAllCardsPresent(copy);
            assertEquals("knowledge is copied", state.getKnowledge(), copy.getKnowledge());
            assertEquals(state.drawDeck.getSize(), copy.drawDeck.getSize());
            for (int p = 0; p < n; p++) {
                FrenchCard original = state.getPlayerCard(p);
                if (known.contains(p))
                    assertEquals("observer " + observer + " knows the card of " + p, original, copy.getPlayerCard(p));
                else if (original != null && !original.equals(copy.getPlayerCard(p)))
                    changed[p]++;
            }
        }
        for (int p = 0; p < n; p++)
            if (!known.contains(p) && state.getPlayerCard(p) != null)
                assertTrue("observer " + observer + ": the unknown card of " + p + " never changes", changed[p] > 0);
    }

    @Test
    public void aRedeterminisedCopyLeavesACardTheObserverGaveAwayWithTheNeighbour() {
        CuckooGameState state = newState(4, 3, 11);
        CuckooForwardModel fm = new CuckooForwardModel();
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());   // P0 gives the 5H to P1 for the 9C
        // P0 knows its own 9C and P1's 5H; P2 and P3 are unknown to P0
        assertRedeterminisationKeeps(state, 0, 0, 1);
        // likewise for P1; but P2 knows only its own card, so P0's and P1's cards are unknown to P2
        assertRedeterminisationKeeps(state, 1, 0, 1);
        assertRedeterminisationKeeps(state, 2, 2);
    }

    @Test
    public void aRedeterminisedCopyLeavesAnExposedKingWithItsHolder() {
        CuckooGameState state = newState(4, 3, 11);
        CuckooForwardModel fm = new CuckooForwardModel();
        dealCards(state, card("5H"), card("KS"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());   // refused: every player has seen P1's KS
        assertRedeterminisationKeeps(state, 2, 1, 2);
        assertRedeterminisationKeeps(state, 3, 1, 3);
        assertRedeterminisationKeeps(state, 0, 0, 1);
    }

    @Test
    public void aRedeterminisedCopyFollowsACardAlongAChainOfSwaps() {
        CuckooGameState state = newState(4, 3, 11);
        CuckooForwardModel fm = new CuckooForwardModel();
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());   // P0 9C, P1 5H
        fm.next(state, new SwapCard());   // P1 3S, P2 5H
        // P0 knows the 5H it gave away is now with P2, but not P1's 3S (knowledge 1010, see CuckooKnowledgeTest)
        assertRedeterminisationKeeps(state, 0, 0, 2);
        // P1 knows P0's 9C and P2's 5H (1110); only P3's card is unknown
        assertRedeterminisationKeeps(state, 1, 0, 1, 2);
    }
}
