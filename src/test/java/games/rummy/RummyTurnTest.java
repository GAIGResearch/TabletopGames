package games.rummy;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * One turn: the legal draws, drawing from each source, the legal discards (with the taken-card rule and its
 * last-card exception) and the discard that passes the turn. Hands are arranged with no set or run in them, so no
 * Meld actions appear.
 */
public class RummyTurnTest {

    RummyForwardModel fm;
    RummyGameState state;

    @Before
    public void setup() {
        fm = new RummyForwardModel();
        state = newState(3, 11, fm);
    }

    @Test
    public void bothSourcesCanBeDrawnFromWhenTheDiscardPileHasACard() {
        assertEquals(Set.of(new DrawCard(false), new DrawCard(true)), actionSet(state, fm));
    }

    @Test
    public void onlyTheDrawDeckCanBeDrawnFromWhenTheDiscardPileIsEmpty() {
        setDiscardPile(state);
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(Set.of(new DrawCard(false)), actionSet(state, fm));
    }

    @Test
    public void drawingFromTheDrawDeckTakesItsTopCard() {
        // the discard pile is arranged last, so it is exact; the draw deck has the arranged cards on top
        giveHand(state, 0, "2H", "5D", "9C", "JS");
        setDrawDeck(state, "8S", "3C", "10D");
        setDiscardPile(state, "QH");
        int drawDeckSize = state.getDrawDeck().getSize();
        fm.next(state, new DrawCard(false));

        assertEquals(Set.of(card("2H"), card("5D"), card("9C"), card("JS"), card("8S")), setOf(state.getPlayerHand(0)));
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertEquals(cards("3C", "10D"), state.getDrawDeck().peek(0, 2));
        assertEquals(cards("QH"), state.getDiscardPile().getComponents());
        assertNull("a card from the draw deck is not a taken card", state.getTakenCard());
        assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void takingTheDiscardTakesItsTopCardAndRecordsIt() {
        giveHand(state, 0, "2H", "5D", "9C", "JS");
        setDrawDeck(state, "8S", "3C", "10D");
        setDiscardPile(state, "QH", "4C");
        int drawDeckSize = state.getDrawDeck().getSize();
        fm.next(state, new DrawCard(true));

        assertEquals(Set.of(card("2H"), card("5D"), card("9C"), card("JS"), card("QH")), setOf(state.getPlayerHand(0)));
        assertEquals(cards("4C"), state.getDiscardPile().getComponents());
        assertEquals(drawDeckSize, state.getDrawDeck().getSize());
        assertEquals(cards("8S", "3C", "10D"), state.getDrawDeck().peek(0, 3));
        assertEquals(card("QH"), state.getTakenCard());
        assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void anyCardCanBeDiscardedAfterDrawingFromTheDrawDeck() {
        giveHand(state, 1, "2H", "5D", "9C", "JS", "7H");
        arrangePlay(state, 1, null);
        assertEquals(Set.of(new Discard(card("2H")), new Discard(card("5D")), new Discard(card("9C")),
                new Discard(card("JS")), new Discard(card("7H"))), actionSet(state, fm));
    }

    @Test
    public void theTakenCardCannotBeDiscarded() {
        giveHand(state, 1, "2H", "5D", "9C", "JS", "7H");
        arrangePlay(state, 1, card("7H"));
        assertEquals(Set.of(new Discard(card("2H")), new Discard(card("5D")), new Discard(card("9C")),
                new Discard(card("JS"))), actionSet(state, fm));
    }

    @Test
    public void theTakenCardCanBeDiscardedWhenItIsTheOnlyCard() {
        giveHand(state, 1, "7H");
        arrangePlay(state, 1, card("7H"));
        assertEquals(Set.of(new Discard(card("7H"))), actionSet(state, fm));
    }

    @Test
    public void aDiscardGoesOnTopOfThePileAndPassesTheTurn() {
        giveHand(state, 1, "2H", "5D", "9C", "JS", "7H");
        setDiscardPile(state, "QH", "4C");
        arrangePlay(state, 1, card("7H"));
        fm.next(state, new Discard(card("9C")));

        assertEquals(Set.of(card("2H"), card("5D"), card("JS"), card("7H")), setOf(state.getPlayerHand(1)));
        assertEquals(cards("9C", "QH", "4C"), state.getDiscardPile().getComponents());
        assertNull("the taken card is forgotten at the end of the turn", state.getTakenCard());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void theLastPlayersDiscardPassesTheTurnToPlayerZero() {
        giveHand(state, 2, "2H", "5D", "9C", "JS", "7H");
        arrangePlay(state, 2, null);
        fm.next(state, new Discard(card("JS")));
        // 3 players: (2 + 1) % 3 = 0
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertEquals(card("JS"), state.getDiscardPile().peek());
    }

    @Test
    public void aTakenCardDiscardedByTheNextPlayerCanBeTakenBack() {
        // player 0 takes the QH, discards the 9C; player 1 takes the 9C and may discard anything but it
        giveHand(state, 0, "2H", "5D", "9C", "JS");
        giveHand(state, 1, "3S", "6D", "10C", "KH");
        setDiscardPile(state, "QH");
        fm.next(state, new DrawCard(true));
        fm.next(state, new Discard(card("9C")));
        assertEquals(1, state.getCurrentPlayer());
        assertNull(state.getTakenCard());
        assertEquals(Set.of(new DrawCard(false), new DrawCard(true)), actionSet(state, fm));

        fm.next(state, new DrawCard(true));
        assertEquals(card("9C"), state.getTakenCard());
        Set<AbstractAction> expected = Set.of(new Discard(card("3S")), new Discard(card("6D")),
                new Discard(card("10C")), new Discard(card("KH")));
        assertEquals(expected, actionSet(state, fm));
        List<FrenchCard> pile = state.getDiscardPile().getComponents();
        assertTrue("the discard pile is empty once the 9C is taken back", pile.isEmpty());
    }
}
