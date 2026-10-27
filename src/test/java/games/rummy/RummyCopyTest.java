package games.rummy;

import core.components.Deck;
import core.components.FrenchCard;
import games.rummy.actions.DrawCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Full and per-player copies of a position in the PLAY phase: player 0 has taken the top discard (known to all),
 * and a meld (5C 6C 7C, moved from wherever it was) is on the table.
 */
public class RummyCopyTest {

    RummyForwardModel fm;
    RummyGameState state;
    FrenchCard taken;

    @Before
    public void setup() {
        fm = new RummyForwardModel();
        state = newState(3, 31, fm);
        RummyMeld meld = new RummyMeld();
        for (FrenchCard c : cards("7C", "6C", "5C")) {
            take(state, c);
            meld.add(c);
        }
        state.melds.add(meld);
        // player 0 takes the top discard (so it is known to all); a second discard stays on the pile
        state.getDiscardPile().addToBottom(state.getDrawDeck().draw());
        taken = state.getDiscardPile().peek();
        new DrawCard(true).execute(state);
        arrangePlay(state, 0, taken);
        assertAllCardsPresent(state);
    }

    @Test
    public void aFullCopyIsEqualAndIndependent() {
        RummyGameState copy = (RummyGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // change the copy: discard a card from player 0's hand
        FrenchCard c = copy.getPlayerHand(0).get(0);
        copy.getPlayerHand(0).remove(c);
        copy.getDiscardPile().add(c);
        assertNotEquals(state, copy);
        assertTrue(state.getPlayerHand(0).contains(c));
        assertEquals(1, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void copiesDifferingInTheTakenCardPhaseOrMeldLimitAreNotEqual() {
        RummyGameState copy = (RummyGameState) state.copy();
        copy.takenCard = null;
        assertNotEquals(state, copy);
        copy = (RummyGameState) state.copy();
        copy.setGamePhase(RummyGameState.Phase.DRAW);
        assertNotEquals(state, copy);
        copy = (RummyGameState) state.copy();
        copy.setMeldedThisTurn(true);
        assertNotEquals(state, copy);
    }

    @Test
    public void aCopysMeldsAreIndependentOfTheOriginal() {
        // for a full copy and for player 0's copy, changing the melds of either leaves the other's unchanged
        for (int observer : new int[]{-1, 0}) {
            RummyGameState copy = (RummyGameState) state.copy(observer);
            // lay off the 8C from wherever it is in the copy onto the copy's run
            take(copy, card("8C"));
            copy.melds.get(0).addToBottom(card("8C"));
            assertEquals(cards("5C", "6C", "7C"), meldCards(state, 0));

            copy = (RummyGameState) state.copy(observer);
            state.melds.get(0).remove(card("7C"));
            assertEquals("observer " + observer, cards("5C", "6C", "7C"), meldCards(copy, 0));
            state.melds.get(0).addToBottom(card("7C"));
        }
    }

    @Test
    public void copiesWithEqualMeldsAreEqualAndDifferentMeldsAreNot() {
        RummyGameState copy = (RummyGameState) state.copy();
        // a new meld component with the same cards has a new component ID, but the states are still equal
        RummyMeld rebuilt = new RummyMeld();
        rebuilt.add(card("7C"));
        rebuilt.add(card("6C"));
        rebuilt.add(card("5C"));
        copy.melds.set(0, rebuilt);
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        copy.melds.get(0).remove(card("7C"));
        assertNotEquals(state, copy);
    }

    @Test
    public void aPlayersCopyKeepsWhatThePlayerCanSeeAndReshufflesTheRest() {
        Set<FrenchCard> hiddenFromZero = new HashSet<>(state.getDrawDeck().getComponents());
        for (int p = 1; p < 3; p++)
            hiddenFromZero.addAll(state.getPlayerHand(p).getComponents());

        int handsChanged = 0, drawDeckChanged = 0;
        for (int i = 0; i < 10; i++) {
            RummyGameState copy = (RummyGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
            assertEquals(state.getMelds(), copy.getMelds());
            assertEquals(taken, copy.getTakenCard());
            assertEquals(RummyGameState.Phase.PLAY, copy.getGamePhase());
            assertEquals(0, copy.getCurrentPlayer());

            Set<FrenchCard> copyHidden = new HashSet<>(copy.getDrawDeck().getComponents());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            if (!state.getDrawDeck().getComponents().equals(copy.getDrawDeck().getComponents())) drawDeckChanged++;
            for (int p = 1; p < 3; p++) {
                Deck<FrenchCard> hand = copy.getPlayerHand(p);
                assertEquals(state.getPlayerHand(p).getSize(), hand.getSize());
                copyHidden.addAll(hand.getComponents());
                if (!state.getPlayerHand(p).getComponents().equals(hand.getComponents())) handsChanged++;
            }
            // the same cards are hidden from player 0, only redistributed
            assertEquals(hiddenFromZero, copyHidden);
            assertAllCardsPresent(copy);
        }
        assertTrue("the other hands were never reshuffled", handsChanged > 0);
        assertTrue("the draw deck was never reshuffled", drawDeckChanged > 0);
    }

    @Test
    public void anotherPlayersCopyKeepsThatPlayersHand() {
        RummyGameState copy = (RummyGameState) state.copy(2);
        assertEquals(state.getPlayerHand(2).getComponents(), copy.getPlayerHand(2).getComponents());
        // player 0's taken card is known to player 2, so it stays in player 0's hand at its index
        int takenIndex = state.getPlayerHand(0).getComponents().indexOf(taken);
        assertEquals(taken, copy.getPlayerHand(0).get(takenIndex));
        assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
        assertEquals(state.getMelds(), copy.getMelds());
        for (int p = 0; p < 3; p++)
            assertEquals(state.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
        assertAllCardsPresent(copy);
    }
}
