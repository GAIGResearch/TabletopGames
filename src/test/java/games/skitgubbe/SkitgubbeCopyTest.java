package games.skitgubbe;


import core.components.FrenchCard;
import games.skitgubbe.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/** Copy, equals, hashCode and redeterminisation in phase one. */
public class SkitgubbeCopyTest {

    SkitgubbeGameState state;
    SkitgubbeForwardModel fm;

    /**
     * A phase-one position with something in every location: player 1 has collected 5H, 9C; players 0 and 2 hold
     * bounced 7S / 7D; player 0 has led 2S to the trick; player 2 holds the trump card 9D; 20 cards in the draw deck.
     */
    @Before
    public void setup() {
        state = newState(3, 42, new SkitgubbeParameters());
        fm = new SkitgubbeForwardModel();
        moveTo(state, state.collectedCards.get(1), "5H", "9C");
        moveTo(state, state.heldCards.get(0), "7S");
        moveTo(state, state.heldCards.get(2), "7D");
        moveTo(state, state.trick, "2S");
        giveTrumpCard(state, 2, "9D");
        while (state.drawDeck.getSize() > 20)
            state.collectedCards.get(1).add(state.drawDeck.draw());
        state.setTurnOwner(1);
        assertAllCardsPresent(state);
    }

    @Test
    public void aFaithfulCopyEqualsTheOriginalAndHashesTheSame() {
        SkitgubbeGameState copy = (SkitgubbeGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void changingACopyLeavesTheOriginalUnchanged() {
        SkitgubbeGameState before = (SkitgubbeGameState) state.copy();
        // snapshots taken without copying the state, in case a copy shares a deck with the original
        List<FrenchCard> trickBefore = new ArrayList<>(state.trick.getComponents());
        List<FrenchCard> hand1Before = new ArrayList<>(state.playerHands.get(1).getComponents());
        List<FrenchCard> held0Before = new ArrayList<>(state.heldCards.get(0).getComponents());
        SkitgubbeGameState copy = (SkitgubbeGameState) state.copy();
        copy.trick.add(copy.playerHands.get(1).draw());
        copy.heldCards.get(0).draw();
        copy.trumpPlayer = 0;
        assertNotEquals(state, copy);
        assertEquals(before, state);
        assertEquals(trickBefore, state.trick.getComponents());
        assertEquals(hand1Before, state.playerHands.get(1).getComponents());
        assertEquals(held0Before, state.heldCards.get(0).getComponents());
    }

    /** The copy for observer keeps what the observer can see and keeps sizes and the union of hidden cards. */
    private void checkRedeterminisedCopy(SkitgubbeGameState original, SkitgubbeGameState copy, int observer) {
        int n = original.getNPlayers();
        assertEquals(original.playerHands.get(observer).getComponents(), copy.playerHands.get(observer).getComponents());
        for (int p = 0; p < n; p++) {
            assertEquals(original.collectedCards.get(p).getComponents(), copy.collectedCards.get(p).getComponents());
            assertEquals(original.heldCards.get(p).getComponents(), copy.heldCards.get(p).getComponents());
            assertEquals(original.playerHands.get(p).getSize(), copy.playerHands.get(p).getSize());
        }
        assertEquals(original.trick.getComponents(), copy.trick.getComponents());
        assertEquals(original.drawDeck.getSize(), copy.drawDeck.getSize());
        assertEquals(original.trumpCard.getSize(), copy.trumpCard.getSize());
        assertEquals(hiddenUnion(original, observer), hiddenUnion(copy, observer));
    }

    private static Set<FrenchCard> hiddenUnion(SkitgubbeGameState s, int observer) {
        Set<FrenchCard> cards = new HashSet<>();
        for (int p = 0; p < s.getNPlayers(); p++)
            if (p != observer) cards.addAll(s.playerHands.get(p).getComponents());
        cards.addAll(s.drawDeck.getComponents());
        cards.addAll(s.trumpCard.getComponents());
        return cards;
    }

    @Test
    public void aCopyForAPlayerRedeterminisesOtherHandsTheDrawDeckAndATrumpCardTheyDidNotDraw() {
        int handChanged = 0, drawChanged = 0, trumpChanged = 0;
        for (int i = 0; i < 20; i++) {
            SkitgubbeGameState copy = (SkitgubbeGameState) state.copy(0);
            checkRedeterminisedCopy(state, copy, 0);
            if (!copy.playerHands.get(1).getComponents().equals(state.playerHands.get(1).getComponents())) handChanged++;
            if (!copy.drawDeck.getComponents().equals(state.drawDeck.getComponents())) drawChanged++;
            if (!copy.trumpCard.getComponents().equals(state.trumpCard.getComponents())) trumpChanged++;
        }
        assertTrue(handChanged > 0);
        assertTrue(drawChanged > 0);
        assertTrue("player 0 did not draw the trump card, so it is hidden from them", trumpChanged > 0);
    }

    @Test
    public void aCopyForTheTrumpDrawerKeepsTheirTrumpCard() {
        for (int i = 0; i < 20; i++) {
            SkitgubbeGameState copy = (SkitgubbeGameState) state.copy(2);
            checkRedeterminisedCopy(state, copy, 2);
            assertEquals(List.of(card("9D")), copy.trumpCard.getComponents());
        }
    }

    @Test
    public void aTrumpCardDrawnInPlayIsKeptForItsDrawerAndHiddenFromOthers() {
        // a fresh position: player 0 draws the last draw-deck card, KD, as the trump card
        SkitgubbeGameState s = newState(3, 42, new SkitgubbeParameters());
        giveHand(s, 0, "5H", "KS", "2C");
        leaveDrawDeck(s, 2, "KD");
        fm.next(s, new PlayCard(card("5H")));
        assertEquals(List.of(card("KD")), s.trumpCard.getComponents());

        int othersSawAnotherCard = 0;
        for (int i = 0; i < 20; i++) {
            SkitgubbeGameState mine = (SkitgubbeGameState) s.copy(0);
            checkRedeterminisedCopy(s, mine, 0);
            assertEquals(List.of(card("KD")), mine.trumpCard.getComponents());
            SkitgubbeGameState theirs = (SkitgubbeGameState) s.copy(1);
            checkRedeterminisedCopy(s, theirs, 1);
            if (!theirs.trumpCard.getComponents().equals(List.of(card("KD")))) othersSawAnotherCard++;
        }
        assertTrue("player 1 did not draw the trump card, so it is hidden from them", othersSawAnotherCard > 0);
    }

    @Test
    public void aFaithfulCopyAfterPlayEqualsTheOriginal() {
        SkitgubbeGameState s = newState(3, 42, new SkitgubbeParameters());
        giveHand(s, 0, "7S", "2S", "3S");
        giveHand(s, 1, "7H", "2D", "3D");
        fm.next(s, new PlayCard(card("7S")));
        fm.next(s, new PlayCard(card("7H")));         // a bounce, so held cards are non-empty
        SkitgubbeGameState copy = (SkitgubbeGameState) s.copy();
        assertEquals(s, copy);
        assertEquals(s.hashCode(), copy.hashCode());
        assertEquals(List.of(card("7S")), copy.heldCards.get(0).getComponents());
    }

    @Test
    public void inPhaseTwoACopyForAnyPlayerEqualsTheOriginalAsNothingIsHidden() {
        core.Game game = newGame(3, 7);
        SkitgubbeGameState s = (SkitgubbeGameState) game.getGameState();
        core.AbstractForwardModel gfm = game.getForwardModel();
        java.util.Random rnd = new java.util.Random(7);
        int steps = 0;
        while (s.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE && steps++ < 200) {
            List<core.actions.AbstractAction> actions = gfm.computeAvailableActions(s);
            gfm.next(s, actions.get(rnd.nextInt(actions.size())));
        }
        assertEquals(SkitgubbeGameState.Phase.PHASE_TWO, s.getGamePhase());
        // two phase-two actions (a lead, then a beat or a pick-up), so a trick may be on the table
        for (int i = 0; i < 2; i++) {
            List<core.actions.AbstractAction> actions = gfm.computeAvailableActions(s);
            assertFalse("no phase-two actions", actions.isEmpty());
            gfm.next(s, actions.get(rnd.nextInt(actions.size())));
        }
        assertEquals(2, s.phaseTwoActions);
        for (int p = 0; p < 3; p++) {
            SkitgubbeGameState copy = (SkitgubbeGameState) s.copy(p);
            assertEquals("copy for " + p, s, copy);
            assertEquals(s.hashCode(), copy.hashCode());
            for (int q = 0; q < 3; q++)
                assertEquals(s.collectedCards.get(q).getComponents(), copy.collectedCards.get(q).getComponents());
            assertEquals(s.trick.getComponents(), copy.trick.getComponents());
        }
    }
}
