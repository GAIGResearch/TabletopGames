package games.schwimmen;

import core.components.FrenchCard;
import games.schwimmen.actions.ExchangeOne;
import games.schwimmen.actions.Pass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Consecutive passes: a full round of passes replaces the table from the draw deck, or ends the deal (and, in the
 * single-deal game, the game) when the draw deck has fewer than 3 cards. Three players; the dealer is player 2.
 */
public class SchwimmenPassesTest {

    SchwimmenForwardModel fm;
    SchwimmenGameState state;

    static final String[] TABLE = h("AH", "7C", "10D");

    @Before
    public void setup() {
        fm = new SchwimmenForwardModel();
        state = newState(3, ordinarySeed(3, 0), fm);
        arrangePlay(state, 0, TABLE, h("7H", "9H", "KS"), h("8C", "9D", "JS"), h("QD", "10C", "8S"));
    }

    private void passes(int n) {
        for (int i = 0; i < n; i++)
            takeTurn(state, fm, new Pass());
    }

    @Test
    public void fewerPassesThanPlayersLeaveTheTableAlone() {
        int drawSize = state.getDrawDeck().getSize();
        passes(2);
        assertEquals(2, state.getConsecutivePasses());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(drawSize, state.getDrawDeck().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void aFullRoundOfPassesReplacesTheTableFromTheDrawDeck() {
        // 32 - 3 x 3 hands - 3 table = 20 in the draw deck
        assertEquals(20, state.getDrawDeck().getSize());
        Set<FrenchCard> topThree = new HashSet<>(state.getDrawDeck().getComponents().subList(0, 3));

        passes(3);

        assertEquals("the old table cards are discarded", Set.copyOf(cards(TABLE)),
                new HashSet<>(state.getDiscardPile().getComponents()));
        assertEquals("three new cards from the top of the draw deck", topThree,
                new HashSet<>(state.getTable().getComponents()));
        assertEquals(3, state.getTable().getSize());
        assertEquals(20 - 3, state.getDrawDeck().getSize());
        assertEquals(0, state.getConsecutivePasses());
        assertEquals("play continues with the next player", 0, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void theRoundOfPassesCanStartWithAnyPlayer() {
        state.setTurnOwner(1);
        passes(2);
        assertEquals(cards(TABLE), state.getTable().getComponents());
        passes(1);
        // players 1, 2 and 0 passed: the table is replaced and player 1 plays next
        assertEquals(Set.copyOf(cards(TABLE)), new HashSet<>(state.getDiscardPile().getComponents()));
        assertEquals(0, state.getConsecutivePasses());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void anExchangeResetsTheCountOfPasses() {
        passes(2);
        // player 2 exchanges QD for AH
        takeTurn(state, fm, new ExchangeOne(card("QD"), card("AH")));
        assertEquals(0, state.getConsecutivePasses());
        List<FrenchCard> tableAfterExchange = new ArrayList<>(state.getTable().getComponents());

        passes(2);
        // players 0 and 1 passed: only 2 passes since the exchange, so the table stays
        assertEquals(2, state.getConsecutivePasses());
        assertEquals(tableAfterExchange, state.getTable().getComponents());
        assertEquals(0, state.getDiscardPile().getSize());

        passes(1);
        // player 2 completes the round: the table (7C, 10D, QD) is replaced
        assertEquals(Set.copyOf(cards("7C", "10D", "QD")), new HashSet<>(state.getDiscardPile().getComponents()));
        assertEquals(0, state.getConsecutivePasses());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void withTwoCardsLeftAFullRoundOfPassesEndsTheGame() {
        leaveInDrawDeck(state, 2);
        int discards = state.getDiscardPile().getSize();

        passes(2);
        assertTrue(state.isNotTerminal());
        passes(1);

        assertFalse("fewer than 3 cards cannot replace the table: the deal ends", state.isNotTerminal());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(2, state.getDrawDeck().getSize());
        assertEquals(discards, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void withExactlyThreeCardsLeftTheTableIsReplacedOnceMore() {
        leaveInDrawDeck(state, 3);
        Set<FrenchCard> lastThree = new HashSet<>(state.getDrawDeck().getComponents());

        passes(3);
        assertTrue(state.isNotTerminal());
        assertEquals(lastThree, new HashSet<>(state.getTable().getComponents()));
        assertEquals(0, state.getDrawDeck().getSize());

        // the next full round of passes cannot replace the table from an empty draw deck
        passes(2);
        assertTrue(state.isNotTerminal());
        passes(1);
        assertFalse(state.isNotTerminal());
        assertEquals(lastThree, new HashSet<>(state.getTable().getComponents()));
        assertAllCardsPresent(state);
    }
}
