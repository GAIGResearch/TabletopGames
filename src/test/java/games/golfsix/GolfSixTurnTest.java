package games.golfsix;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.golfsix.actions.DiscardCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * A normal turn after the reveal: draw from either pile, then replace a grid card or discard; refilling the draw
 * deck.
 */
public class GolfSixTurnTest {

    GolfSixGameState state;
    GolfSixForwardModel fm;
    int drawDeckSize;

    static final Set<AbstractAction> ALL_REPLACES = Set.of(new ReplaceCard(0), new ReplaceCard(1), new ReplaceCard(2),
            new ReplaceCard(3), new ReplaceCard(4), new ReplaceCard(5));

    @Before
    public void setup() {
        state = newState(3, 31);
        fm = new GolfSixForwardModel();
        // player 0: 5H KS 2C / 5D 9H 2D with positions 0 and 1 face-up; everyone past the reveal
        setGrid(state, 0, "5H", "KS", "2C", "5D", "9H", "2D");
        skipReveal(state);
        stackDrawDeck(state, "7S");
        setTopDiscard(state, "8C");
        drawDeckSize = state.getDrawDeck().getSize();
        assertEquals(52 - 18 - 1, drawDeckSize);
        assertEquals(0, state.getCurrentPlayer());
    }

    private Set<AbstractAction> actions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void aTurnStartsWithADrawFromEitherPile() {
        assertEquals(Set.of(new DrawCard(false), new DrawCard(true)), actions());
    }

    @Test
    public void drawingFromTheDrawDeckShowsTheCardOnlyToTheCurrentPlayer() {
        fm.next(state, new DrawCard(false));
        assertEquals(card("7S"), state.getDrawnCard());
        assertFalse(state.isDrawnFromDiscard());
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertEquals(List.of(card("8C")), state.getDiscardPile().getComponents());
        assertTrue(state.getDrawnCardDeck().getVisibilityForPlayer(0, 0));
        assertFalse(state.getDrawnCardDeck().getVisibilityForPlayer(0, 1));
        assertFalse(state.getDrawnCardDeck().getVisibilityForPlayer(0, 2));
        assertEquals("the player still has to place it", 0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aCardFromTheDrawDeckMayReplaceAnyGridCardOrBeDiscarded() {
        fm.next(state, new DrawCard(false));
        Set<AbstractAction> expected = new HashSet<>(ALL_REPLACES);
        expected.add(new DiscardCard());
        assertEquals(expected, actions());
    }

    @Test
    public void drawingFromTheDiscardPileShowsTheCardToAll() {
        fm.next(state, new DrawCard(true));
        assertEquals(card("8C"), state.getDrawnCard());
        assertTrue(state.isDrawnFromDiscard());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(drawDeckSize, state.getDrawDeck().getSize());
        assertTrue(visibleToAll(state.getDrawnCardDeck(), 0, 3));
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aCardFromTheDiscardPileMustReplaceAGridCard() {
        fm.next(state, new DrawCard(true));
        assertEquals(ALL_REPLACES, actions());
    }

    @Test
    public void replacingAFaceDownCardTurnsTheNewCardUpAndDiscardsTheOldOne() {
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));
        assertEquals(cards("5H", "KS", "2C", "5D", "7S", "2D"), state.getGrid(0).getComponents());
        assertEquals("UUDDUD", faceUpPattern(state, 0));
        // the face-down 9H goes face-up on top of the discard pile
        assertEquals(cards("9H", "8C"), state.getDiscardPile().getComponents());
        assertNull(state.getDrawnCard());
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertEquals("placing ends the turn", 1, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void replacingAFaceUpCardDiscardsIt() {
        fm.next(state, new DrawCard(true));
        fm.next(state, new ReplaceCard(0));
        assertEquals(cards("8C", "KS", "2C", "5D", "9H", "2D"), state.getGrid(0).getComponents());
        assertEquals("UUDDDD", faceUpPattern(state, 0));
        assertEquals(cards("5H"), state.getDiscardPile().getComponents());
        assertNull(state.getDrawnCard());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void discardingPutsTheDrawnCardOnTheDiscardPileAndLeavesTheGrid() {
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        assertEquals(cards("5H", "KS", "2C", "5D", "9H", "2D"), state.getGrid(0).getComponents());
        assertEquals("UUDDDD", faceUpPattern(state, 0));
        // the draw deck is not empty, so nothing is refilled
        assertEquals(cards("7S", "8C"), state.getDiscardPile().getComponents());
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertNull(state.getDrawnCard());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void anEmptyDrawDeckIsRefilledFromTheDiscardPileAtTheEndOfTheTurn() {
        // 7S is the only card left to draw; the other 32 are moved under the 8C: discard pile 1 + 32 = 33 cards
        leaveInDrawDeck(state, 1);
        assertEquals(33, state.getDiscardPile().getSize());
        List<FrenchCard> oldDiscards = new ArrayList<>(state.getDiscardPile().getComponents());

        fm.next(state, new DrawCard(false));
        assertEquals("the refill waits for the end of the turn", 0, state.getDrawDeck().getSize());
        fm.next(state, new DiscardCard());

        // the discard pile keeps only its new top card, the 7S; the other 33 form the new draw deck
        assertEquals(cards("7S"), state.getDiscardPile().getComponents());
        assertEquals(33, state.getDrawDeck().getSize());
        assertEquals(new HashSet<>(oldDiscards), new HashSet<>(state.getDrawDeck().getComponents()));
        List<FrenchCard> reversed = new ArrayList<>(oldDiscards);
        Collections.reverse(reversed);
        List<FrenchCard> newDrawDeck = state.getDrawDeck().getComponents();
        assertTrue("the new draw deck is shuffled", !newDrawDeck.equals(oldDiscards) && !newDrawDeck.equals(reversed));
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void theDrawDeckIsAlsoRefilledAfterAReplacement() {
        leaveInDrawDeck(state, 1);
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));
        // the replaced 9H is the top discard and stays; the 33 cards under it form the draw deck
        assertEquals(cards("9H"), state.getDiscardPile().getComponents());
        assertEquals(33, state.getDrawDeck().getSize());
        assertEquals(card("7S"), state.getGrid(0).get(4));
        assertAllCardsPresent(state);
    }
}
