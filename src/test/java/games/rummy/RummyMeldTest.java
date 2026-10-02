package games.rummy;

import core.actions.AbstractAction;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import games.rummy.actions.Meld;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Laying down a meld in the PLAY phase: which sets and runs are offered, the meld's effect on the hand and the
 * table, and the limit of one meld a turn. The table is empty unless a test adds a meld, so no lay-offs appear.
 * Cards within a set are ordered by suit: Diamonds, Hearts, Clubs, Spades.
 */
public class RummyMeldTest {

    RummyForwardModel fm;
    RummyGameState state;

    @Before
    public void setup() {
        fm = new RummyForwardModel();
        state = newState(3, 41, fm);
    }

    private Set<AbstractAction> meldActions() {
        return actionSet(state, fm).stream().filter(a -> a instanceof Meld).collect(Collectors.toSet());
    }

    @Test
    public void threeOfAKindIsOfferedAsOneSet() {
        giveHand(state, 0, "7H", "7D", "7C", "2S", "10D");
        arrangePlay(state, 0, null);
        assertEquals(union(discards("7H", "7D", "7C", "2S", "10D"), meld("7D", "7H", "7C")), actionSet(state, fm));
    }

    @Test
    public void fourOfAKindIsOfferedAsFourThreeCardSetsAndTheFourCardSet() {
        giveHand(state, 0, "9H", "9D", "9C", "9S", "KD");
        arrangePlay(state, 0, null);
        // C(4,3) = 4 three-card sets + 1 four-card set
        assertEquals(union(discards("9H", "9D", "9C", "9S", "KD"),
                        meld("9D", "9H", "9C"), meld("9D", "9H", "9S"), meld("9D", "9C", "9S"), meld("9H", "9C", "9S"),
                        meld("9D", "9H", "9C", "9S")),
                actionSet(state, fm));
    }

    @Test
    public void aRunOfFourIsOfferedWithEachOfItsSegments() {
        giveHand(state, 0, "4H", "5H", "6H", "7H", "JC");
        arrangePlay(state, 0, null);
        // segments of 3 or more: 4-5-6, 5-6-7, 4-5-6-7
        assertEquals(union(discards("4H", "5H", "6H", "7H", "JC"),
                        meld("4H", "5H", "6H"), meld("5H", "6H", "7H"), meld("4H", "5H", "6H", "7H")),
                actionSet(state, fm));
    }

    @Test
    public void anAceIsLowInARun() {
        giveHand(state, 0, "3S", "AS", "2S", "9D");
        arrangePlay(state, 0, null);
        assertEquals(Set.of(meld("AS", "2S", "3S")), meldActions());
    }

    @Test
    public void anAceIsNotHighAndRunsDoNotWrap() {
        // Q-K-A is not a run (Ace low), nor K-A-2 (no wrap)
        giveHand(state, 0, "QH", "KH", "AH", "2H", "6C");
        arrangePlay(state, 0, null);
        assertEquals(discards("QH", "KH", "AH", "2H", "6C"), actionSet(state, fm));
    }

    @Test
    public void cardsInSequenceOfMixedSuitsAreNotARun() {
        giveHand(state, 0, "4H", "5D", "6H", "7C", "8C");
        arrangePlay(state, 0, null);
        assertEquals(Set.of(), meldActions());
    }

    @Test
    public void aCardCanBeOfferedInBothASetAndARun() {
        giveHand(state, 0, "5H", "6H", "7H", "7D", "7C");
        arrangePlay(state, 0, null);
        assertEquals(Set.of(meld("5H", "6H", "7H"), meld("7D", "7H", "7C")), meldActions());
    }

    @Test
    public void aMeldBuiltInAnyOrderIsTheSameAction() {
        assertEquals(meld("5H", "6H", "7H"), meld("7H", "5H", "6H"));
        assertEquals(cards("5H", "6H", "7H"), meld("7H", "5H", "6H").cards);
        assertEquals(cards("QD", "QH", "QS"), meld("QS", "QH", "QD").cards);
    }

    @Test
    public void aMeldMovesItsCardsToANewMeldAtTheEndOfTheTable() {
        addMeld(state, "JD", "JC", "JS");
        giveHand(state, 0, "7H", "5H", "6H", "3C", "KD");
        arrangePlay(state, 0, null);
        fm.next(state, meld("7H", "6H", "5H"));

        assertEquals(Set.copyOf(cards("3C", "KD")), setOf(state.getPlayerHand(0)));
        assertEquals(2, state.getMelds().size());
        assertEquals(cards("JD", "JC", "JS"), meldCards(state, 0));
        // the new meld in order, lowest rank first
        assertEquals(cards("5H", "6H", "7H"), meldCards(state, 1));
        assertTrue(state.hasMeldedThisTurn());
        // cards left in hand: the player goes on
        assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getTurnCounter());
        assertAllCardsPresent(state);
    }

    @Test
    public void aSetIsPutOnTheTableInSuitOrder() {
        giveHand(state, 0, "2S", "2C", "2D", "KD");
        arrangePlay(state, 0, null);
        fm.next(state, meld("2S", "2C", "2D"));
        // Diamonds, Hearts, Clubs, Spades
        assertEquals(List.of(cards("2D", "2C", "2S")), state.getMelds().stream().map(m -> m.getComponents()).toList());
        assertEquals(List.of(card("KD")), state.getPlayerHand(0).getComponents());
    }

    @Test
    public void onlyOneMeldCanBeLaidDownInATurn() {
        giveHand(state, 0, "5H", "6H", "7H", "2C", "2D", "2S", "KD");
        arrangePlay(state, 0, null);
        fm.next(state, meld("5H", "6H", "7H"));
        // the set of 2s is still in hand but no second meld is offered; nothing fits 5H-6H-7H (no 4H or 8H)
        assertEquals(discards("2C", "2D", "2S", "KD"), actionSet(state, fm));
    }

    @Test
    public void theMeldLimitIsResetForTheNextTurn() {
        giveHand(state, 0, "5H", "6H", "7H", "KD", "QC");
        giveHand(state, 1, "8S", "8D", "4C");
        setDrawDeck(state, "8C", "JH", "3D");
        arrangePlay(state, 0, null);
        fm.next(state, meld("5H", "6H", "7H"));
        fm.next(state, new Discard(card("KD")));
        assertFalse(state.hasMeldedThisTurn());
        assertEquals(1, state.getCurrentPlayer());

        // player 1 draws the 8C and can meld the set of 8s
        fm.next(state, new DrawCard(false));
        assertEquals(Set.of(meld("8D", "8C", "8S")), meldActions());
    }

    @Test
    public void theTakenCardCanBeMelded() {
        giveHand(state, 0, "8D", "8C", "KS", "8S");
        arrangePlay(state, 0, card("8S"));
        // the 8S may not be discarded, but may be melded
        assertEquals(union(discards("8D", "8C", "KS"), meld("8D", "8C", "8S")), actionSet(state, fm));
        fm.next(state, meld("8D", "8C", "8S"));
        assertEquals(cards("8D", "8C", "8S"), meldCards(state, 0));
        assertEquals(List.of(card("KS")), state.getPlayerHand(0).getComponents());
    }
}
