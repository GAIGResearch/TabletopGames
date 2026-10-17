package games.skitgubbe;

import core.actions.AbstractAction;
import games.skitgubbe.actions.PickUp;
import games.skitgubbe.actions.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/**
 * SkitgubbeParameters.completerLeads = true (pagat): after a phase-two action that completes a trick, the
 * completer leads the next trick, or the next holder after them when they are out. 3 players A = 0, B = 1, C = 2;
 * trumps Hearts. The default (false) positions are asserted in SkitgubbePhaseTwoTest (aTrickOfTrickSize...,
 * pagatExampleOne..., pagatExampleTwo..., pagatExampleThree...).
 */
public class SkitgubbeCompleterLeadsTest {

    SkitgubbeForwardModel fm = new SkitgubbeForwardModel();

    private SkitgubbeGameState newThree() {
        SkitgubbeParameters params = SkitgubbeTestUtils.valetParams();
        params.setParameterValue("completerLeads", true);
        return newState(3, 42, params);
    }

    private void play(SkitgubbeGameState state, String code) {
        fm.next(state, new PlayCard(card(code)));
    }

    private void pickUp(SkitgubbeGameState state) {
        fm.next(state, new PickUp());
    }

    private static void assertActions(List<AbstractAction> actual, AbstractAction... expected) {
        assertEquals(Set.of(expected), new HashSet<>(actual));
        assertEquals(expected.length, actual.size());
    }

    private static PlayCard pc(String code) {
        return new PlayCard(card(code));
    }

    @Test
    public void thePlayerWhoCompletesATrickLeadsTheNext() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "9S 3C", "JS 4C");
        play(state, "5S");     // A leads
        assertEquals(1, state.getCurrentPlayer());   // not a completion: the next holder acts as usual
        play(state, "9S");     // B beats (2 < trickSize 3)
        assertEquals(2, state.getCurrentPlayer());
        play(state, "JS");     // C beats: 3 cards = trickSize 3, complete

        assertEquals(0, state.trick.getSize());
        assertEquals(2, state.getCurrentPlayer());   // C, the completer (default: A)
        assertEquals(3, state.trickSize);
        // C leads a new trick: any card, no pick-up
        assertActions(fm.computeAvailableActions(state), pc("4C"));
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void pagatExampleOneWithCompleterLeadsTheBeaterWhoCompletesLeads() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 7S 2C", "6S 10S 3C", "4C 5C");
        play(state, "5S");     // A leads
        play(state, "6S");     // B beats
        pickUp(state);         // C picks up B's 6S: not a completion
        assertEquals(0, state.getCurrentPlayer());   // the next holder after C is A, as by default
        play(state, "7S");     // A beats his own 5S (2 < 3)
        assertEquals(1, state.getCurrentPlayer());
        play(state, "10S");    // B beats: 5S, 7S, 10S = trickSize 3, complete

        assertEquals(0, state.trick.getSize());
        assertEquals(1, state.getCurrentPlayer());   // B, the completer (default: C)
        assertActions(fm.computeAvailableActions(state), pc("3C"));
        assertEquals(5, state.phaseTwoActions);
    }

    @Test
    public void pagatExampleTwoWithCompleterLeadsAPickUpThatEmptiesTheTrickIsNotACompletion() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "6S 3C", "4C");
        play(state, "5S");     // A leads
        play(state, "6S");     // B beats
        pickUp(state);         // C picks up 6S
        pickUp(state);         // A picks up his own 5S: the trick is empty but was never completed
        assertEquals(0, state.trick.getSize());
        assertEquals(1, state.getCurrentPlayer());   // the next holder after A (the picker) is B, who leads
        assertActions(fm.computeAvailableActions(state), pc("3C"));
    }

    @Test
    public void pagatExampleThreeWithCompleterLeadsTheCompleterLeadsAfterAPlayerWentOut() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S", "6S 8S 3C", "9S 4C");
        play(state, "5S");     // A leads his last card: out, exitScore 3; trickSize stays 3
        play(state, "6S");     // B beats
        pickUp(state);         // C picks up 6S
        assertEquals(1, state.getCurrentPlayer());   // not a completion: after C, A is out -> B
        play(state, "8S");     // B beats A's 5S (2 < 3)
        assertEquals(2, state.getCurrentPlayer());
        play(state, "9S");     // C beats: 5S, 8S, 9S = trickSize 3, complete

        assertEquals(0, state.trick.getSize());
        assertEquals(2, state.trickSize);            // B {3C}, C {4C 6S}
        assertEquals(2, state.getCurrentPlayer());   // C, the completer (default: B)
        assertActions(fm.computeAvailableActions(state), pc("4C"), pc("6S"));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void aCompleterWhoPlayedTheirLastCardPassesTheLeadToTheNextHolder() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "9S 3C", "JS");
        play(state, "5S");     // A leads
        play(state, "9S");     // B beats
        play(state, "JS");     // C beats with his last card: out, exitScore 3; the trick (3) is complete

        assertEquals(3, state.getExitScore(2));
        assertTrue(state.isNotTerminal());           // A {2C} and B {3C} still hold cards
        assertEquals(0, state.trick.getSize());
        assertEquals(2, state.trickSize);
        assertEquals(0, state.getCurrentPlayer());   // C holds nothing: the next holder after C, A, leads
        assertActions(fm.computeAvailableActions(state), pc("2C"));
    }
}
