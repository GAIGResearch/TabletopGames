package games.schwimmen;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.actions.ExchangeAll;
import games.schwimmen.actions.ExchangeOne;
import games.schwimmen.actions.Pass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * A normal turn: the actions offered, the effect of each exchange (cards and who can see them), and the turn passing
 * clockwise. Three players; the dealer is player 2.
 */
public class SchwimmenTurnTest {

    SchwimmenForwardModel fm;
    SchwimmenGameState state;

    static final String[] TABLE = h("AH", "7C", "10D");
    static final String[] HAND0 = h("7H", "9H", "KS");
    static final String[] HAND1 = h("8C", "9D", "JS");
    static final String[] HAND2 = h("QD", "10C", "8S");

    @Before
    public void setup() {
        fm = new SchwimmenForwardModel();
        state = newState(3, ordinarySeed(3, 0), fm);
        arrangePlay(state, 0, TABLE, HAND0, HAND1, HAND2);
    }

    @Test
    public void aTurnOffersNineSingleExchangesExchangeAllAndPass() {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // 3 hand cards x 3 table cards = 9 single exchanges, plus exchange all and pass
        assertEquals(11, actions.size());
        assertEquals(turnActions(HAND0, TABLE), new HashSet<>(actions));
    }

    @Test
    public void theDealerIsOfferedTheSameTurnWhenPlayComesRound() {
        state.setTurnOwner(2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(11, actions.size());
        assertEquals(turnActions(HAND2, TABLE), new HashSet<>(actions));

        takeTurn(state, fm, new Pass());
        assertEquals("after the dealer (player 2) comes player 0", 0, state.getCurrentPlayer());
    }

    @Test
    public void exchangeOneSwapsExactlyTheNamedCards() {
        takeTurn(state, fm, new ExchangeOne(card("KS"), card("AH")));

        assertEquals(Set.copyOf(cards("7H", "9H", "AH")), new HashSet<>(state.getPlayerHand(0).getComponents()));
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(Set.copyOf(cards("KS", "7C", "10D")), new HashSet<>(state.getTable().getComponents()));
        assertEquals(3, state.getTable().getSize());
        assertEquals(cards(HAND1), state.getPlayerHand(1).getComponents());
        assertEquals(cards(HAND2), state.getPlayerHand(2).getComponents());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aCardTakenFromTheTableIsSeenByEveryoneAndTheRestOfTheHandIsNot() {
        takeTurn(state, fm, new ExchangeOne(card("KS"), card("AH")));

        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(0);
        assertTrue("AH was taken face up", visibleToAll(hand, indexOf(hand, "AH"), 3));
        assertTrue(visibleOnlyToOwner(hand, indexOf(hand, "7H"), 3));
        assertTrue(visibleOnlyToOwner(hand, indexOf(hand, "9H"), 3));
    }

    @Test
    public void exchangeAllSwapsTheWholeHandWithTheTableAndShowsTheNewHand() {
        takeTurn(state, fm, new ExchangeAll());

        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(0);
        assertEquals(Set.copyOf(cards(TABLE)), new HashSet<>(hand.getComponents()));
        assertEquals(3, hand.getSize());
        for (int i = 0; i < 3; i++)
            assertTrue("all three taken cards are seen by everyone", visibleToAll(hand, i, 3));
        assertEquals(Set.copyOf(cards(HAND0)), new HashSet<>(state.getTable().getComponents()));
        assertEquals(3, state.getTable().getSize());
        assertEquals(cards(HAND1), state.getPlayerHand(1).getComponents());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPassChangesNoCardsAndPassesTheTurn() {
        takeTurn(state, fm, new Pass());

        assertEquals(cards(HAND0), state.getPlayerHand(0).getComponents());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(1, state.getConsecutivePasses());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void theNextPlayerCanTakeBackACardJustGivenToTheTable() {
        // player 0 gives KS for AH; player 1 then takes KS for JS
        takeTurn(state, fm, new ExchangeOne(card("KS"), card("AH")));
        assertEquals(turnActions(HAND1, h("KS", "7C", "10D")), new HashSet<>(fm.computeAvailableActions(state)));

        takeTurn(state, fm, new ExchangeOne(card("JS"), card("KS")));
        PartialObservableDeck<FrenchCard> hand1 = state.getPlayerHand(1);
        assertEquals(Set.copyOf(cards("8C", "9D", "KS")), new HashSet<>(hand1.getComponents()));
        assertTrue(visibleToAll(hand1, indexOf(hand1, "KS"), 3));
        assertEquals(Set.copyOf(cards("JS", "7C", "10D")), new HashSet<>(state.getTable().getComponents()));
        assertEquals(2, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }
}
