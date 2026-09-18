package games.crazyeights;

import core.components.Deck;
import core.components.FrenchCard;
import games.crazyeights.actions.DrawCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.crazyeights.CZETestUtils.*;
import static org.junit.Assert.*;

/**
 * Drawing when the stock is empty reshuffles the discards under the top card into a new stock.
 */
public class CZEStockTest {

    CZEGameState state;
    CZEForwardModel fm;

    @Before
    public void setup() {
        CZEParameters params = new CZEParameters();
        params.setRandomSeed(42);
        state = new CZEGameState(params, 3);
        fm = new CZEForwardModel();
        fm.setup(state);
    }

    private Deck<FrenchCard> hand(int player) {
        return state.getPlayerHands().get(player);
    }

    @Test
    public void canDrawWhileTheStockOrTheDiscardsUnderTheTopCardHaveCards() {
        setTopDiscard(state, card("5H"));
        assertTrue("stock has cards", state.canDraw());

        moveStockUnderTopDiscard(state);
        assertTrue("stock empty, discards under the top", state.canDraw());

        leaveNothingToDraw(state, 1);
        assertFalse("stock empty, only the top discard", state.canDraw());

        putUnderTopDiscard(state, card("2D"));
        assertTrue("stock empty, one card under the top", state.canDraw());
    }

    @Test
    public void drawIsOfferedWhenTheStockIsEmptyButOneCardLiesUnderTheTopDiscard() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));
        leaveNothingToDraw(state, 1);
        putUnderTopDiscard(state, card("2D"));
        assertEquals(0, state.getDrawDeck().getSize());

        assertEquals(List.of(new DrawCard()), fm.computeAvailableActions(state));
    }

    @Test
    public void drawingFromAnEmptyStockReshufflesTheDiscardsUnderTheTopCardIntoANewStock() {
        // Eight of Clubs on top with Hearts nominated: the suit to match must survive the reshuffle
        setTopDiscard(state, card("8C"), Hearts);
        giveHand(state, 0, card("9C"), card("KS"));
        moveStockUnderTopDiscard(state);
        List<FrenchCard> discards = state.getDiscardPile().getComponents();
        Set<FrenchCard> underTop = new HashSet<>(discards.subList(1, discards.size()));
        int handSize1 = hand(1).getSize();
        assertEquals(0, state.getDrawDeck().getSize());
        assertTrue("enough discards to shuffle", underTop.size() > 10);

        fm.next(state, new DrawCard());

        assertEquals("only the top card stays on the discard pile", List.of(card("8C")), state.getDiscardPile().getComponents());
        assertEquals(Hearts, state.getCurrentSuit());
        // one of the former discards went to the hand, the rest form the new stock
        assertEquals(underTop.size() - 1, state.getDrawDeck().getSize());
        assertEquals(3, hand(0).getSize());
        List<FrenchCard> drawn = new ArrayList<>(hand(0).getComponents());
        drawn.removeAll(List.of(card("9C"), card("KS")));
        assertEquals(1, drawn.size());
        Set<FrenchCard> stockAndDrawn = new HashSet<>(state.getDrawDeck().getComponents());
        stockAndDrawn.addAll(drawn);
        assertEquals(underTop, stockAndDrawn);
        assertEquals(handSize1, hand(1).getSize());
        assertEquals("the turn ends after drawing", 1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void withOneCardUnderTheTopDiscardThatCardIsDrawn() {
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));
        leaveNothingToDraw(state, 1);
        putUnderTopDiscard(state, card("2D"));

        fm.next(state, new DrawCard());

        assertEquals(Set.of(card("9C"), card("KS"), card("2D")), new HashSet<>(hand(0).getComponents()));
        assertEquals(List.of(card("5H")), state.getDiscardPile().getComponents());
        assertEquals(0, state.getDrawDeck().getSize());
        assertEquals(Hearts, state.getCurrentSuit());
        assertAllCardsPresent(state);
    }

    @Test
    public void redeterminisedCopyAfterAReshuffleKeepsTheTopDiscardAndStockSize() {
        setTopDiscard(state, card("8C"), Hearts);
        giveHand(state, 0, card("9C"), card("KS"));
        moveStockUnderTopDiscard(state);
        fm.next(state, new DrawCard());

        CZEGameState copy = (CZEGameState) state.copy(0);
        assertEquals(List.of(card("8C")), copy.getDiscardPile().getComponents());
        assertEquals(Hearts, copy.getCurrentSuit());
        assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
        assertEquals(hand(0).getComponents(), copy.getPlayerHands().get(0).getComponents());
        assertAllCardsPresent(copy);
    }
}
