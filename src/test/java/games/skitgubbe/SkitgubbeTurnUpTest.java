package games.skitgubbe;

import core.actions.AbstractAction;
import games.skitgubbe.actions.PickUp;
import games.skitgubbe.actions.PlayCard;
import games.skitgubbe.actions.TurnUpCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Phase one: turning up the top card of the draw deck instead of playing from hand. Offered only while the draw
 * deck holds more than one card (as on pagat; RECYCLE also allows it with one). Three players unless stated.
 */
public class SkitgubbeTurnUpTest {

    SkitgubbeGameState state;
    SkitgubbeForwardModel fm;

    @Before
    public void setup() {
        state = newState(3, 42, new SkitgubbeParameters());
        fm = new SkitgubbeForwardModel();
    }

    private void play(String code) {
        fm.next(state, new PlayCard(card(code)));
    }

    private void turnUp() {
        fm.next(state, new TurnUpCard());
    }

    private static PlayCard pc(String code) {
        return new PlayCard(card(code));
    }

    // ---- availability ----

    @Test
    public void theLeaderMayTurnUpWhenTheDrawDeckHoldsTwoCards() {
        giveHand(state, 0, "5H", "KS", "2C");
        leaveDrawDeck(state, 2, "9D", "4S");
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("5H"), pc("KS"), pc("2C"), new TurnUpCard()), new HashSet<>(actions));
        assertEquals(4, actions.size());              // 3 hand cards + 1 turn-up
    }

    @Test
    public void theFollowerMayTurnUpWhileTheDrawDeckIsLarge() {
        moveTo(state, state.trick, "5H");            // player 0 has led the 5 of Hearts
        giveHand(state, 1, "7D", "7C", "AS");
        state.setTurnOwner(1);
        assertEquals(42, state.drawDeck.getSize());   // 52 - 3 * 3 in hands - 1 on the trick
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("7D"), pc("7C"), pc("AS"), new TurnUpCard()), new HashSet<>(actions));
        assertEquals(4, actions.size());
    }

    @Test
    public void theFollowerMayNotTurnUpTheLastCardOfTheDrawDeck() {
        moveTo(state, state.trick, "5H");
        giveHand(state, 1, "7D", "7C", "AS");
        leaveDrawDeck(state, 2, "9D");                // one card: it will be the trump card, not a turn-up
        state.setTurnOwner(1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("7D"), pc("7C"), pc("AS")), new HashSet<>(actions));
        assertEquals(3, actions.size());
    }

    @Test
    public void theLeaderMayNotTurnUpFromAnEmptyDrawDeck() {
        giveTrumpCard(state, 2, "9D");
        giveHand(state, 0, "5H", "KS");
        leaveDrawDeck(state, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("5H"), pc("KS")), new HashSet<>(actions));
        assertEquals(2, actions.size());
    }

    @Test
    public void thereIsNoTurnUpInPhaseTwoEvenWithCardsInTheDrawDeck() {
        arrangePhaseTwo(state, "H", 0, "5S 9S", "KD", "QC");
        moveTo(state, state.drawDeck, "2C", "3C");   // an impossible position: only the phase rules out the turn-up
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("5S"), pc("9S")), new HashSet<>(actions));
        assertEquals(2, actions.size());

        moveTo(state, state.trick, "7S");            // to beat: 9S beats 7S, 5S does not
        actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("9S"), new PickUp()), new HashSet<>(actions));
        assertEquals(2, actions.size());
    }

    // ---- execution ----

    @Test
    public void aTurnUpPlaysTheTopDrawDeckCardAndTheHandIsUnchangedWithNoDraw() {
        giveHand(state, 0, "5H", "KS", "2C");
        stackDrawDeck(state, "9D", "4S");
        assertEquals(43, state.drawDeck.getSize());   // 52 - 3 * 3

        turnUp();

        assertEquals(List.of(card("9D")), state.trick.getComponents());
        assertEquals(setOf("5H", "KS", "2C"), asSet(state.playerHands.get(0)));   // still 3: no draw
        assertEquals(42, state.drawDeck.getSize());   // 43 - 1 turned up, 0 drawn
        assertEquals(card("4S"), state.drawDeck.peek());
        assertEquals(0, state.trumpCard.getSize());
        assertEquals(1, state.getCurrentPlayer());    // the follower, 0 + 1
        assertAllCardsPresent(state);
    }

    @Test
    public void withHandSizeTwoATurnUpLeavesTheHandAtTwoWithNoDraw() {
        SkitgubbeParameters params = new SkitgubbeParameters();
        params.setParameterValue("handSize", 2);
        state = newState(3, 42, params);
        giveHand(state, 0, "5H", "KS");
        stackDrawDeck(state, "9D", "4S");
        assertEquals(46, state.drawDeck.getSize());   // 52 - 3 * 2

        turnUp();

        // 2 is not below handSize 2, so no draw (a hard-coded 3 would draw the 4 of Spades)
        assertEquals(setOf("5H", "KS"), asSet(state.playerHands.get(0)));
        assertEquals(45, state.drawDeck.getSize());   // 46 - 1 turned up, 0 drawn
        assertEquals(card("4S"), state.drawDeck.peek());
        assertEquals(List.of(card("9D")), state.trick.getComponents());
        assertAllCardsPresent(state);
    }

    @Test
    public void aTurnedUpCardThatWinsCollectsTheTrickForTheFollowerWhoLeadsNext() {
        giveHand(state, 0, "5H", "2S", "3S");
        giveHand(state, 1, "7D", "2D", "3D");
        stackDrawDeck(state, "4C", "9C", "JS");      // player 0 draws 4C after leading; player 1 turns up 9C

        play("5H");
        turnUp();                                     // 9 > 5: the follower's turned-up card wins

        assertEquals(setOf("5H", "9C"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.collectedCards.get(0).getSize());
        assertEquals(0, state.trick.getSize());
        assertEquals(setOf("7D", "2D", "3D"), asSet(state.playerHands.get(1)));   // unchanged, no draw
        assertEquals(setOf("2S", "3S", "4C"), asSet(state.playerHands.get(0)));
        assertEquals(41, state.drawDeck.getSize());   // 43 - 1 drawn - 1 turned up
        assertEquals(card("JS"), state.drawDeck.peek());
        assertEquals(1, state.getCurrentPlayer());    // the winner leads
        assertAllCardsPresent(state);
    }

    @Test
    public void aTurnedUpLeadThatWinsCollectsForTheLeaderWhoLeadsAgain() {
        giveHand(state, 0, "5H", "2S", "3S");
        giveHand(state, 1, "7D", "2D", "3D");
        stackDrawDeck(state, "AS", "4C", "JS");      // player 0 turns up AS; player 1 draws 4C after following

        turnUp();
        play("7D");                                   // Ace 14 > 7: the leader's turned-up card wins

        assertEquals(setOf("AS", "7D"), asSet(state.collectedCards.get(0)));
        assertEquals(0, state.collectedCards.get(1).getSize());
        assertEquals(setOf("5H", "2S", "3S"), asSet(state.playerHands.get(0)));
        assertEquals(setOf("2D", "3D", "4C"), asSet(state.playerHands.get(1)));
        assertEquals(41, state.drawDeck.getSize());   // 43 - 1 turned up - 1 drawn
        assertEquals(0, state.getCurrentPlayer());    // the leader leads again
        assertAllCardsPresent(state);
    }

    @Test
    public void aTurnedUpCardThatTiesGoesToTheTurningPlayersHeldCards() {
        giveHand(state, 0, "7S", "2S", "3S");
        giveHand(state, 1, "5D", "2D", "3D");
        stackDrawDeck(state, "4C", "7H", "JS");      // player 0 draws 4C; player 1 turns up 7H

        play("7S");
        turnUp();                                     // 7 = 7: a bounce

        assertEquals(List.of(card("7S")), state.heldCards.get(0).getComponents());
        assertEquals(List.of(card("7H")), state.heldCards.get(1).getComponents());
        assertEquals(0, state.trick.getSize());
        for (int p = 0; p < 3; p++)
            assertEquals("collected of " + p, 0, state.collectedCards.get(p).getSize());
        assertEquals(3, state.playerHands.get(1).getSize());
        assertEquals(41, state.drawDeck.getSize());   // 43 - 1 drawn - 1 turned up
        assertEquals(0, state.getCurrentPlayer());    // the same leader
        assertAllCardsPresent(state);
    }

    @Test
    public void aTurnUpWithTwoCardsLeftLeavesTheTrumpCardForTheNextPlayerFromHand() {
        giveHand(state, 0, "5H", "KS", "2C");
        giveHand(state, 1, "7D", "2D", "3D");
        leaveDrawDeck(state, 2, "9D", "4S");

        turnUp();                                     // 9D to the trick; 4S is left
        assertEquals(List.of(card("4S")), state.drawDeck.getComponents());
        assertEquals(setOf("5H", "KS", "2C"), asSet(state.playerHands.get(0)));
        assertEquals(0, state.trumpCard.getSize());
        assertEquals(-1, state.getTrumpPlayer());

        // player 1 follows; one card left, so no turn-up
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("7D"), pc("2D"), pc("3D")), new HashSet<>(actions));

        play("2D");                                   // 9 > 2: player 0 wins; player 1 draws the trump card

        assertEquals(List.of(card("4S")), state.trumpCard.getComponents());
        assertEquals(1, state.trumpCard.getOwnerId());
        assertEquals(1, state.getTrumpPlayer());
        assertEquals(setOf("7D", "3D"), asSet(state.playerHands.get(1)));   // one short of 3
        assertEquals(0, state.drawDeck.getSize());
        assertEquals(setOf("9D", "2D"), asSet(state.collectedCards.get(0)));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(fm.computeAvailableActions(state).contains(new TurnUpCard()));
        assertAllCardsPresent(state);
    }
}
