package games.schwimmen;

import core.components.FrenchCard;
import games.schwimmen.actions.Close;
import games.schwimmen.actions.CloseDecision;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Faithful copies, equality and hashing, and redeterminised copies from one player's view.
 */
public class SchwimmenCopyTest {

    SchwimmenForwardModel fm;
    SchwimmenGameState state;

    /**
     * 4 players (dealer 3), mid-deal: player 1 holds 8C 9D and AH, which they took from the table (seen by all);
     * the table is KD 7S 10H; 10 cards left in the draw deck, the rest discarded; 2 passes; player 2 to play.
     */
    @Before
    public void setup() {
        fm = new SchwimmenForwardModel();
        state = newState(4, ordinarySeed(4, 0), fm);
        arrangePlay(state, 2, h("KD", "7S", "10H"), h("7H", "9H", "KS"), h("8C", "9D", "JS"),
                h("QD", "10C", "8S"), h("QC", "JC", "7D"));
        giveHand(state, 1, "8C", "9D");
        giveTakenCard(state, 1, "AH");
        leaveInDrawDeck(state, 10);
        state.consecutivePasses = 2;
        assertEquals(3, state.getPlayerHand(1).getSize());
        assertTrue(state.getDiscardPile().getSize() > 0);
        assertAllCardsPresent(state);
    }

    @Test
    public void aFullCopyIsEqualAndIndependent() {
        SchwimmenGameState copy = (SchwimmenGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        FrenchCard c = copy.getPlayerHand(0).get(0);
        copy.getPlayerHand(0).remove(c);
        copy.getTable().add(c);
        copy.consecutivePasses = 3;
        assertNotEquals(state, copy);

        assertTrue(state.getPlayerHand(0).contains(c));
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(cards("KD", "7S", "10H"), state.getTable().getComponents());
        assertEquals(2, state.getConsecutivePasses());
        assertAllCardsPresent(state);
    }

    @Test
    public void copiesDifferingInOneFieldAreNotEqual() {
        SchwimmenGameState copy = (SchwimmenGameState) state.copy();
        copy.consecutivePasses = 0;
        assertNotEquals(state, copy);
        copy = (SchwimmenGameState) state.copy();
        copy.dealer = 0;
        assertNotEquals(state, copy);
        copy = (SchwimmenGameState) state.copy();
        // the same cards, but AH no longer seen by everyone
        copy.getPlayerHand(1).setVisibilityOfComponent(indexOf(copy.getPlayerHand(1), "AH"), 0, false);
        assertNotEquals(state, copy);
    }

    @Test
    public void aCopyCarriesTheCloserAndTheCloserIsPartOfEquality() {
        state.closer = 1;
        SchwimmenGameState copy = (SchwimmenGameState) state.copy();
        assertEquals(1, copy.getCloser());
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals("a player's copy keeps the closer", 1, ((SchwimmenGameState) state.copy(0)).getCloser());

        copy.closer = -1;
        assertNotEquals("states differing only in the closer", state, copy);
        copy.closer = 3;
        assertNotEquals(state, copy);
    }

    @Test
    public void aCopyDuringTheCloseDecisionCarriesItAndPlaysOnIndependently() {
        // player 2 has just passed (third pass) and must decide whether to close
        state.consecutivePasses = 3;
        state.setActionInProgress(new CloseDecision(2));
        SchwimmenGameState copy = (SchwimmenGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(2, copy.getCurrentPlayer());
        assertEquals(CLOSE_CHOICES, new java.util.HashSet<>(fm.computeAvailableActions(copy)));

        fm.next(copy, new Close(true));
        assertEquals(2, copy.getCloser());
        assertEquals(0, copy.getConsecutivePasses());
        assertEquals(3, copy.getCurrentPlayer());
        assertNotEquals(state, copy);

        // the original still waits for player 2's decision
        assertEquals(-1, state.getCloser());
        assertEquals(3, state.getConsecutivePasses());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(CLOSE_CHOICES, new java.util.HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void aStateWaitingForTheCloseDecisionDiffersFromOneWithoutIt() {
        SchwimmenGameState copy = (SchwimmenGameState) state.copy();
        copy.setActionInProgress(new CloseDecision(2));
        assertNotEquals(state, copy);
    }

    @Test
    public void aPlayersCopyKeepsWhatThePlayerCanSeeAndShufflesTheRest() {
        // hidden from player 0: player 1's 8C and 9D, the hands of 2 and 3, and the draw deck
        Set<FrenchCard> hidden = new HashSet<>(cards("8C", "9D"));
        hidden.addAll(state.getPlayerHand(2).getComponents());
        hidden.addAll(state.getPlayerHand(3).getComponents());
        hidden.addAll(state.getDrawDeck().getComponents());
        int ahIndex = indexOf(state.getPlayerHand(1), "AH");

        int handsChanged = 0, drawChanged = 0;
        for (int i = 0; i < 20; i++) {
            SchwimmenGameState copy = (SchwimmenGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            assertEquals(state.getTable().getComponents(), copy.getTable().getComponents());
            assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
            assertEquals("the card taken from the table stays put", card("AH"), copy.getPlayerHand(1).get(ahIndex));
            for (int p = 0; p < 4; p++)
                assertEquals(3, copy.getPlayerHand(p).getSize());
            assertEquals(10, copy.getDrawDeck().getSize());
            assertEquals(0, copy.getExtraHand().getSize());

            Set<FrenchCard> copyHidden = new HashSet<>(copy.getDrawDeck().getComponents());
            copyHidden.addAll(copy.getPlayerHand(2).getComponents());
            copyHidden.addAll(copy.getPlayerHand(3).getComponents());
            for (FrenchCard c : copy.getPlayerHand(1).getComponents())
                if (!c.equals(card("AH"))) copyHidden.add(c);
            assertEquals("the same cards are hidden, only redistributed", hidden, copyHidden);

            for (int p = 1; p < 4; p++)
                if (!state.getPlayerHand(p).getComponents().equals(copy.getPlayerHand(p).getComponents()))
                    handsChanged++;
            if (!state.getDrawDeck().getComponents().equals(copy.getDrawDeck().getComponents()))
                drawChanged++;
            assertEquals(state.getConsecutivePasses(), copy.getConsecutivePasses());
            assertEquals(state.getCurrentPlayer(), copy.getCurrentPlayer());
            assertAllCardsPresent(copy);
        }
        assertTrue("the other hands were never redeterminised", handsChanged > 0);
        assertTrue("the draw deck was never redeterminised", drawChanged > 0);
    }

    @Test
    public void theExtraHandIsRedeterminisedEvenForTheDealer() {
        SchwimmenGameState fresh = newState(4, ordinarySeed(4, 0), fm);
        List<FrenchCard> dealerHand = fresh.getPlayerHand(3).getComponents();
        int extraChanged = 0;
        for (int i = 0; i < 20; i++) {
            SchwimmenGameState copy = (SchwimmenGameState) fresh.copy(3);
            assertEquals(dealerHand, copy.getPlayerHand(3).getComponents());
            assertEquals(3, copy.getExtraHand().getSize());
            assertTrue(copy.isDealerChoicePending());
            if (!fresh.getExtraHand().getComponents().equals(copy.getExtraHand().getComponents()))
                extraChanged++;
            assertAllCardsPresent(copy);
        }
        assertTrue("the dealer has not seen the extra hand, so it is redeterminised", extraChanged > 0);
    }
}
