package games.gofish;

import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * Known voids: the ranks each player is publicly known not to hold. The GoFishKnownVoids class on its own, then the
 * rules that record and forget them during an ask. Three players; P0 to play.
 */
public class GoFishKnownVoidsTest {

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
        for (int p = 0; p < 3; p++)
            assertEquals("nothing is known after the deal", Set.of(), state.getKnownVoids().get(p));
    }

    // ---- the class ----

    @Test
    public void recordedRanksAreKeptPerPlayer() {
        GoFishKnownVoids kv = new GoFishKnownVoids(3);
        kv.record(1, 5);
        kv.record(1, 9);
        kv.record(2, 5);
        assertEquals(Set.of(5, 9), kv.get(1));
        assertEquals(Set.of(5), kv.get(2));
        assertEquals(Set.of(), kv.get(0));
    }

    @Test
    public void drewForgetsOnlyThatPlayersVoids() {
        GoFishKnownVoids kv = new GoFishKnownVoids(3);
        kv.record(1, 5);
        kv.record(1, 9);
        kv.record(2, 5);
        kv.drew(1);
        assertEquals(Set.of(), kv.get(1));
        assertEquals(Set.of(5), kv.get(2));
    }

    @Test
    public void permitsRefusesACardOfAVoidRankOnlyInThatPlayersHand() {
        GoFishKnownVoids kv = new GoFishKnownVoids(3);
        kv.record(1, 5);
        Deck<FrenchCard> hand1 = new PartialObservableDeck<>("hand_1", 1, 3, VISIBLE_TO_OWNER);
        Deck<FrenchCard> hand2 = new PartialObservableDeck<>("hand_2", 2, 3, VISIBLE_TO_OWNER);
        Deck<FrenchCard> drawDeck = new Deck<>("DrawDeck", HIDDEN_TO_ALL);   // no owner (-1)
        assertFalse(kv.permits(hand1, card("5H")));
        assertTrue(kv.permits(hand1, card("9H")));
        assertTrue(kv.permits(hand2, card("5H")));
        assertTrue(kv.permits(drawDeck, card("5H")));
    }

    @Test
    public void aCopyIsEqualAndIndependent() {
        GoFishKnownVoids kv = new GoFishKnownVoids(3);
        kv.record(1, 5);
        GoFishKnownVoids copy = kv.copy();
        assertEquals(kv, copy);
        assertEquals(kv.hashCode(), copy.hashCode());
        copy.record(2, 9);
        assertEquals(Set.of(), kv.get(2));
        assertNotEquals(kv, copy);
    }

    // ---- rules 5-9 during play ----

    @Test
    public void aTargetWhoSaysGoFishIsKnownVoidInTheRank() {
        fm.next(state, new GoFishAsk(2, 5));     // P2 holds no 5
        assertEquals(Set.of(5), state.getKnownVoids().get(2));
    }

    @Test
    public void aTargetWhoHandsOverTheRankIsKnownVoidInItAndKeepsOtherVoids() {
        state.getKnownVoids().record(1, 7);      // P1 holds no 7
        fm.next(state, new GoFishAsk(1, 5));     // P1 hands over 5S 5C
        assertEquals(Set.of(7, 5), state.getKnownVoids().get(1));
    }

    @Test
    public void drawingForgetsAllTheDrawersVoidsAndNobodyElses() {
        state.getKnownVoids().record(0, 9);      // P0 holds no 9, no 7
        state.getKnownVoids().record(0, 7);
        state.getKnownVoids().record(1, 7);      // P1 holds no 7
        fm.next(state, new GoFishAsk(2, 5));     // Go fish: P0 draws the 8D (private)
        assertEquals(Set.of(), state.getKnownVoids().get(0));
        assertEquals(Set.of(7), state.getKnownVoids().get(1));
        assertEquals(Set.of(5), state.getKnownVoids().get(2));
    }

    @Test
    public void receivingCardsKeepsTheAskersOtherVoids() {
        state.getKnownVoids().record(0, 9);      // P0 holds no 9
        fm.next(state, new GoFishAsk(1, 5));     // success: no draw
        assertEquals(Set.of(9), state.getKnownVoids().get(0));
    }

    @Test
    public void receivingCardsOfARankClearsThatVoid() {
        // Artificial: in play an asker cannot be known void in the rank they ask for (they would have had to draw
        // it, which forgets their voids). The rule is defensive: cards received are public, so r is not a void.
        state.getKnownVoids().record(0, 5);
        state.getKnownVoids().record(0, 9);
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(Set.of(9), state.getKnownVoids().get(0));
    }

    @Test
    public void layingDownABookChangesNoVoids() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        state.getKnownVoids().record(0, 9);
        fm.next(state, new GoFishAsk(1, 5));     // 5H 5D + 5S 5C: a book of 5s for P0
        assertEquals(cards("5H", "5D", "5S", "5C"), cardSet(state.playerBooks.get(0)));
        assertEquals("P0's voids: the 9 kept, the book's rank not added", Set.of(9), state.getKnownVoids().get(0));
        assertEquals("P1 handed over the 5s", Set.of(5), state.getKnownVoids().get(1));
    }
}
