package games.rummy;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.rummy.actions.LayOff;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static games.rummy.RummyTestUtils.*;
import static games.rummy.actions.LayOff.Position.*;
import static org.junit.Assert.*;

/**
 * Laying off a card from the hand onto a meld on the table: which lay-offs are offered and where the card goes.
 * Melds on the table belong to nobody, so no test distinguishes the player's own melds from another player's.
 * Unless a test melds, hands are arranged with no set or run, so no Meld actions appear.
 */
public class RummyLayOffTest {

    RummyForwardModel fm;
    RummyGameState state;

    @Before
    public void setup() {
        fm = new RummyForwardModel();
        state = newState(3, 51, fm);
    }

    private Set<AbstractAction> layOffActions() {
        return actionSet(state, fm).stream().filter(a -> a instanceof LayOff).collect(Collectors.toSet());
    }

    @Test
    public void aCardOfASetsRankCanBeLaidOffOntoTheSet() {
        addMeld(state, "9D", "9H", "9C");
        giveHand(state, 0, "9S", "4D", "KC");
        arrangePlay(state, 0, null);
        assertEquals(union(discards("9S", "4D", "KC"), layOff("9S", SET)), actionSet(state, fm));
    }

    @Test
    public void theCardsNextToARunsEndsCanBeLaidOffLowAndHigh() {
        addMeld(state, "5D", "6D", "7D");
        giveHand(state, 0, "4D", "8D", "QS");
        arrangePlay(state, 0, null);
        // 4D is one below the lowest (5D), 8D one above the highest (7D)
        assertEquals(union(discards("4D", "8D", "QS"), layOff("4D", LOW), layOff("8D", HIGH)), actionSet(state, fm));
    }

    @Test
    public void cardsOfAnotherSuitOrNotAdjacentDoNotFit() {
        addMeld(state, "5D", "6D", "7D");
        addMeld(state, "9D", "9H", "9C");
        // 4H and 8C: wrong suit; 3D and 10D: a rank gap; 8H: not the set's rank
        giveHand(state, 0, "4H", "8C", "3D", "10D", "8H");
        arrangePlay(state, 0, null);
        assertEquals(Set.of(), layOffActions());
    }

    @Test
    public void anAceGoesBelowATwoButNothingGoesAboveAKing() {
        addMeld(state, "2C", "3C", "4C");
        addMeld(state, "JS", "QS", "KS");
        giveHand(state, 0, "AC", "AS", "10S", "7H");
        arrangePlay(state, 0, null);
        // AC is one below 2C; AS is not one above KS (Aces are low); 10S is one below JS
        assertEquals(union(discards("AC", "AS", "10S", "7H"), layOff("AC", LOW), layOff("10S", LOW)),
                actionSet(state, fm));
    }

    @Test
    public void aCardCanFitBothASetAndARun() {
        addMeld(state, "5D", "5C", "5S");
        addMeld(state, "6H", "7H", "8H");
        giveHand(state, 0, "5H", "2C");
        arrangePlay(state, 0, null);
        assertEquals(union(discards("5H", "2C"), layOff("5H", SET), layOff("5H", LOW)), actionSet(state, fm));
    }

    @Test
    public void aCardCanFitTheEndsOfTwoRuns() {
        addMeld(state, "2H", "3H", "4H");
        addMeld(state, "6H", "7H", "8H");
        giveHand(state, 0, "5H", "KC");
        arrangePlay(state, 0, null);
        assertEquals(union(discards("5H", "KC"), layOff("5H", HIGH), layOff("5H", LOW)), actionSet(state, fm));
    }

    @Test
    public void theTakenCardCanBeLaidOff() {
        addMeld(state, "5D", "6D", "7D");
        giveHand(state, 0, "4D", "QS", "JC");
        arrangePlay(state, 0, card("4D"));
        assertEquals(union(discards("QS", "JC"), layOff("4D", LOW)), actionSet(state, fm));
    }

    @Test
    public void aLowLayOffGoesAtTheStartOfTheRun() {
        addMeld(state, "5D", "6D", "7D");
        giveHand(state, 0, "4D", "QS", "JC");
        arrangePlay(state, 0, null);
        fm.next(state, layOff("4D", LOW));
        assertEquals(cards("4D", "5D", "6D", "7D"), meldCards(state, 0));
        assertEquals(Set.copyOf(cards("QS", "JC")), setOf(state.getPlayerHand(0)));
        // cards left: the player goes on, and may still discard
        assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertEquals(discards("QS", "JC"), actionSet(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void aHighLayOffGoesAtTheEndOfTheRun() {
        addMeld(state, "5D", "6D", "7D");
        giveHand(state, 0, "8D", "QS", "JC");
        arrangePlay(state, 0, null);
        fm.next(state, layOff("8D", HIGH));
        assertEquals(cards("5D", "6D", "7D", "8D"), meldCards(state, 0));
        assertEquals(Set.copyOf(cards("QS", "JC")), setOf(state.getPlayerHand(0)));
    }

    @Test
    public void aSetLayOffIsPutInSuitOrder() {
        addMeld(state, "9D", "9C", "9S");
        giveHand(state, 0, "9H", "QS", "JC");
        arrangePlay(state, 0, null);
        fm.next(state, layOff("9H", SET));
        // Diamonds, Hearts, Clubs, Spades: the 9H goes second
        assertEquals(cards("9D", "9H", "9C", "9S"), meldCards(state, 0));
    }

    @Test
    public void layOffsCanBeMadeBeforeTheMeldAndAfterIt() {
        addMeld(state, "6H", "7H", "8H");
        giveHand(state, 0, "5H", "4H", "JS", "JD", "JC", "10D", "2C");
        arrangePlay(state, 0, null);
        // the hand holds no run: the 4H and 5H are only two cards, as 6H-8H are on the table
        fm.next(state, layOff("5H", LOW));
        assertFalse("a lay-off is not a meld", state.hasMeldedThisTurn());
        assertEquals(union(discards("4H", "JS", "JD", "JC", "10D", "2C"),
                meld("JD", "JC", "JS"), layOff("4H", LOW)), actionSet(state, fm));
        fm.next(state, layOff("4H", LOW));
        fm.next(state, meld("JD", "JC", "JS"));
        // after the meld: no second meld, and 10D and 2C fit no meld
        assertEquals(discards("10D", "2C"), actionSet(state, fm));
        assertEquals(List.of(cards("4H", "5H", "6H", "7H", "8H"), cards("JD", "JC", "JS")),
                state.getMelds().stream().map(m -> m.getComponents()).toList());
    }

    @Test
    public void fitsChecksTheMeldKindAsWellAsTheCard() {
        List<FrenchCard> set = cards("8D", "8H", "8C");
        List<FrenchCard> run = cards("6H", "7H", "8H");
        assertTrue(RummyUtils.fits(set, card("8S"), SET));
        // a set is not a run: no card goes at its ends
        assertFalse(RummyUtils.fits(set, card("9D"), HIGH));
        assertFalse(RummyUtils.fits(set, card("7H"), LOW));
        // a run is not a set: a card of the run's highest rank does not go onto it
        assertFalse(RummyUtils.fits(run, card("8S"), SET));
        assertTrue(RummyUtils.fits(run, card("5H"), LOW));
        assertTrue(RummyUtils.fits(run, card("9H"), HIGH));
        // the ends are not interchangeable
        assertFalse(RummyUtils.fits(run, card("5H"), HIGH));
        assertFalse(RummyUtils.fits(run, card("9H"), LOW));
        // no Ace above a King, no card below an Ace
        assertFalse(RummyUtils.fits(cards("JC", "QC", "KC"), card("AC"), HIGH));
        assertFalse(RummyUtils.fits(cards("AC", "2C", "3C"), card("KC"), LOW));
    }
}
