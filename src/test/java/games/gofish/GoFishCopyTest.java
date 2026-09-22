package games.gofish;

import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.actions.GoFishAsk;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies of the parameters, faithful copies of the state, and redeterminisation (copy(p)): what p knows is kept,
 * the hidden cards are reshuffled, and no card goes to a player known to be void in its rank.
 */
public class GoFishCopyTest {

    @Test
    public void aParametersCopyKeepsEveryValue() {
        GoFishParameters params = new GoFishParameters();
        params.setParameterValue("startingHandSize", 6);
        params.setParameterValue("twoPlayerHandSize", 8);
        params.setParameterValue("continueOnSuccess", false);
        params.setParameterValue("continueOnDrawingSameRank", false);
        GoFishParameters copy = (GoFishParameters) params.copy();
        assertEquals(6, copy.startingHandSize);
        assertEquals(8, copy.twoPlayerHandSize);
        assertFalse(copy.continueOnSuccess);
        assertFalse(copy.continueOnDrawingSameRank);
        assertEquals(params, copy);
        assertEquals(params.hashCode(), copy.hashCode());
        // and a difference in one boolean alone makes them unequal
        GoFishParameters other = (GoFishParameters) params.copy();
        other.setParameterValue("continueOnDrawingSameRank", true);
        assertNotEquals(params, other);
    }

    private static List<List<FrenchCard>> snapshot(GoFishGameState state) {
        List<List<FrenchCard>> decks = new ArrayList<>();
        for (var d : allDecks(state)) decks.add(new ArrayList<>(d.getComponents()));
        return decks;
    }

    @Test
    public void aFullCopyIsEqualAndIndependent() {
        GoFishGameState state = newState(3, 17);
        GoFishForwardModel fm = new GoFishForwardModel();
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"), card("9C"));
        giveHand(state, 2, card("7H"), card("2C"));
        fm.next(state, new GoFishAsk(1, 5));     // success: P0 is to go again
        assertTrue(state.isExtraTurn());

        GoFishGameState copy = (GoFishGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertTrue("extraTurn is copied", copy.isExtraTurn());

        // an action on the copy leaves the original unchanged
        List<List<FrenchCard>> before = snapshot(state);
        GoFishGameState reference = (GoFishGameState) state.copy();
        fm.next(copy, new GoFishAsk(2, 13));     // Go fish: P0 draws
        assertEquals(before, snapshot(state));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(reference, state);
        assertNotEquals(state, copy);
    }

    @Test
    public void statesDifferingOnlyInExtraTurnAreNotEqual() {
        GoFishGameState state = newState(3, 17);
        GoFishGameState copy = (GoFishGameState) state.copy();
        copy.setExtraTurn(!state.isExtraTurn());
        assertNotEquals(state, copy);
    }

    // ---- known voids in copies and equality ----

    @Test
    public void knownVoidsAreCopiedDeeplyByFullAndRedeterminisedCopies() {
        GoFishGameState state = newState(3, 17);
        state.getKnownVoids().record(1, 5);
        for (int observer = -1; observer < 3; observer++) {
            GoFishGameState copy = (GoFishGameState) state.copy(observer);
            assertEquals("observer " + observer, Set.of(5), copy.getKnownVoids().get(1));
            copy.getKnownVoids().record(2, 9);
            assertEquals("observer " + observer + ": the original is unchanged", Set.of(), state.getKnownVoids().get(2));
        }
    }

    @Test
    public void statesDifferingOnlyInKnownVoidsAreNotEqual() {
        GoFishGameState state = newState(3, 17);
        GoFishGameState copy = (GoFishGameState) state.copy();
        assertEquals(state, copy);
        copy.getKnownVoids().record(1, 5);
        assertNotEquals(state, copy);
        assertNotEquals("known voids are part of the hash", state.hashCode(), copy.hashCode());
    }

    // ---- redeterminisation ----

    /**
     * P0 has asked P1 for 5s and received them: P0's 5H, 5S, 5C are visible to all; P0's KD is private.
     * P1 holds 9D (private), P2 holds 7H 2C (private).
     */
    private static GoFishGameState afterASuccessfulAsk() {
        GoFishGameState state = newState(3, 17);
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"), card("5C"), card("9D"));
        giveHand(state, 2, card("7H"), card("2C"));
        new GoFishForwardModel().next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "KD", "5S", "5C"), cardSet(state.playerHands.get(0)));
        assertEquals(0, state.getCurrentPlayer());
        return state;
    }

    @Test
    public void aRedeterminisedCopyKeepsTheObserversHandEveryVisibleCardAndTheSizes() {
        GoFishGameState state = afterASuccessfulAsk();
        for (int p = 0; p < 3; p++) {
            for (int i = 0; i < 20; i++) {
                GoFishGameState copy = (GoFishGameState) state.copy(p);
                String id = "observer " + p + ", copy " + i;
                assertEquals(id + ": own hand, card for card", state.playerHands.get(p).getComponents(),
                        copy.playerHands.get(p).getComponents());
                for (int q = 0; q < 3; q++) {
                    PartialObservableDeck<FrenchCard> orig = state.playerHands.get(q);
                    PartialObservableDeck<FrenchCard> cp = copy.playerHands.get(q);
                    assertEquals(id + ": size of hand " + q, orig.getSize(), cp.getSize());
                    for (int j = 0; j < orig.getSize(); j++)
                        if (orig.isComponentVisible(j, p))
                            assertEquals(id + ": visible card " + j + " of hand " + q, orig.get(j), cp.get(j));
                    assertEquals(id + ": books of " + q, state.playerBooks.get(q).getComponents(),
                            copy.playerBooks.get(q).getComponents());
                }
                assertEquals(id + ": draw deck size", state.drawDeck.getSize(), copy.drawDeck.getSize());
                assertAllCardsPresent(copy);
            }
        }
        // P0's shown 5s in particular are seen by P1 in every copy (at the same positions, checked above)
        assertEquals(3, visibleToAllOfRank(state, 0, 5));
    }

    @Test
    public void aRedeterminisedCopyReshufflesTheHiddenCards() {
        GoFishGameState state = afterASuccessfulAsk();
        // from P1's view, P0's KD, P2's 7H 2C and the whole draw deck are hidden
        boolean changed = false;
        for (int i = 0; i < 20 && !changed; i++) {
            GoFishGameState copy = (GoFishGameState) state.copy(1);
            changed = !copy.playerHands.get(0).getComponents().equals(state.playerHands.get(0).getComponents())
                    || !copy.playerHands.get(2).getComponents().equals(state.playerHands.get(2).getComponents())
                    || !copy.drawDeck.getComponents().equals(state.drawDeck.getComponents());
        }
        assertTrue("some hidden card changes over 20 copies", changed);
    }

    @Test
    public void aRedeterminisedCopyNeverGivesAPlayerACardOfARankTheyAreKnownVoidIn() {
        GoFishGameState state = newState(3, 17);
        // P1 holds only picture cards and aces, and is known void in every rank 2-10; P2 is known void in aces
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("JH"), card("QH"), card("KH"), card("AH"), card("JD"));
        giveHand(state, 2, card("7H"), card("2C"));
        for (int r = 2; r <= 10; r++) state.getKnownVoids().record(1, r);
        state.getKnownVoids().record(2, 14);
        // without the constraint P1's 5 hidden cards would come from a pool of 2 + 5 + 43 = 50 hidden to P0, of
        // which 36 are ranks 2-10: a void-breaking deal would appear almost every copy
        for (int observer : new int[]{0, 2}) {
            for (int i = 0; i < 200; i++) {
                GoFishGameState copy = (GoFishGameState) state.copy(observer);
                for (FrenchCard c : copy.playerHands.get(1).getComponents())
                    assertTrue("observer " + observer + " copy " + i + ": P1 is void in " + c.number, c.number > 10);
                for (FrenchCard c : copy.playerHands.get(2).getComponents())
                    assertNotEquals("observer " + observer + " copy " + i + ": P2 is void in aces", 14, c.number);
                assertEquals(5, copy.playerHands.get(1).getSize());
                assertEquals(2, copy.playerHands.get(2).getSize());
            }
        }
    }
}
