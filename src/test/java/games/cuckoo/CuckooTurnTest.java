package games.cuckoo;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.cuckoo.actions.KeepCard;
import games.cuckoo.actions.SwapCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * A single decision within a round: the actions offered, keeping, swapping with the neighbour (and the King refusal),
 * and the dealer's cut. The end of the round is in CuckooRoundEndTest.
 */
public class CuckooTurnTest {

    CuckooGameState state;
    CuckooForwardModel fm;

    @Before
    public void setup() {
        // 4 players, 3 lives each; player 3 deals, player 0 decides first
        state = newState(4, 3, 42);
        fm = new CuckooForwardModel();
    }

    private void assertCards(String... codes) {
        for (int p = 0; p < codes.length; p++)
            assertEquals("card of player " + p, codes[p] == null ? null : card(codes[p]), state.getPlayerCard(p));
    }

    @Test
    public void bothActionsAreAlwaysAvailableIncludingToTheDealerAndAKingHolder() {
        Set<AbstractAction> both = Set.of(new KeepCard(), new SwapCard());
        dealCards(state, card("5H"), card("9C"), card("JD"), card("3S"));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals("player 0", both, new HashSet<>(actions));

        // a player holding a King may still swap it away
        dealCards(state, card("KH"), card("9C"), card("JD"), card("3S"));
        assertEquals("King holder", both, new HashSet<>(fm.computeAvailableActions(state)));

        // the dealer (player 3) decides last, with the same two choices
        setDealerAndTurn(state, 3, 3);
        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals("dealer", both, new HashSet<>(actions));
    }

    @Test
    public void keepingMovesNoCardAndPassesTheTurnLeft() {
        dealCards(state, card("5H"), card("9C"), card("JD"), card("3S"));
        List<FrenchCard> drawBefore = new ArrayList<>(state.drawDeck.getComponents());
        fm.next(state, new KeepCard());
        assertCards("5H", "9C", "JD", "3S");
        assertEquals(drawBefore, state.drawDeck.getComponents());
        assertEquals(1, state.getCurrentPlayer());
        assertArrayEquals(new int[]{3, 3, 3, 3}, state.lives);
    }

    @Test
    public void theTurnPassesOverPlayersOutOfTheGame() {
        knockOut(state, 1, 0);
        dealCards(state, card("5H"), null, card("JD"), card("3S"));
        fm.next(state, new KeepCard());
        // player 1 is out, so player 2 is next on player 0's left
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void swappingExchangesCardsWithTheLeftNeighbour() {
        dealCards(state, card("5H"), card("9C"), card("JD"), card("3S"));
        List<FrenchCard> drawBefore = new ArrayList<>(state.drawDeck.getComponents());
        fm.next(state, new SwapCard());
        // player 0 and player 1 exchange; nobody else's card and not the draw deck changes
        assertCards("9C", "5H", "JD", "3S");
        assertEquals(drawBefore, state.drawDeck.getComponents());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void theNeighbourIsTheNextPlayerStillInTheGame() {
        knockOut(state, 1, 0);
        dealCards(state, card("5H"), null, card("JD"), card("3S"));
        fm.next(state, new SwapCard());
        // player 1 is out: player 0 exchanges with player 2
        assertCards("JD", null, "5H", "3S");
        assertEquals(2, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void theNeighbourOfTheHighestNumberedPlayerIsPlayerZero() {
        // player 1 deals, so the order is 2, 3, 0, 1: player 3's left-hand neighbour is player 0
        dealCards(state, card("5H"), card("9C"), card("JD"), card("3S"));
        setDealerAndTurn(state, 1, 3);
        fm.next(state, new SwapCard());
        assertCards("3S", "9C", "JD", "5H");
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void aNeighbourHoldingAKingRefusesTheSwap() {
        dealCards(state, card("5H"), card("KS"), card("JD"), card("3S"));
        List<FrenchCard> drawBefore = new ArrayList<>(state.drawDeck.getComponents());
        fm.next(state, new SwapCard());
        assertCards("5H", "KS", "JD", "3S");
        assertEquals(drawBefore, state.drawDeck.getComponents());
        assertEquals("the turn still passes on", 1, state.getCurrentPlayer());
    }

    @Test
    public void aKingCanBeSwappedAway() {
        // only the neighbour's King stops an exchange; the player's own King does not
        dealCards(state, card("KH"), card("5D"), card("JD"), card("3S"));
        fm.next(state, new SwapCard());
        assertCards("5D", "KH", "JD", "3S");
    }

    // The dealer's decision ends the round, and fm.next then deals again, so the cut itself is checked by executing
    // the action alone. Its effect on the round's result is in CuckooRoundEndTest.

    @Test
    public void theDealerSwapsWithTheTopCardOfTheDrawDeck() {
        dealCards(state, card("5H"), card("9C"), card("JD"), card("3S"));
        putOnTopOfDrawDeck(state, card("8D"));
        setDealerAndTurn(state, 3, 3);
        new SwapCard().execute(state);
        // the dealer takes the 8D and their 3S goes into the draw deck; the dealer's neighbour (player 0) is untouched
        assertCards("5H", "9C", "JD", "8D");
        assertTrue("dealer's old card is in the draw deck", state.drawDeck.contains(card("3S")));
        assertFalse(state.drawDeck.contains(card("8D")));
        assertEquals(52 - 4, state.drawDeck.getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealerMustKeepTheirCardWhenTheCutIsAKing() {
        dealCards(state, card("5H"), card("9C"), card("JD"), card("3S"));
        putOnTopOfDrawDeck(state, card("KC"));
        setDealerAndTurn(state, 3, 3);
        List<FrenchCard> drawBefore = new ArrayList<>(state.drawDeck.getComponents());
        new SwapCard().execute(state);
        // the King stays on top of the draw deck and nothing moves
        assertCards("5H", "9C", "JD", "3S");
        assertEquals(drawBefore, state.drawDeck.getComponents());
        assertEquals(card("KC"), state.drawDeck.peek());
    }
}
