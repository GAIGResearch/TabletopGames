package games.schwimmen;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.schwimmen.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Closing: the close decision after each exchange or pass, the last round after a close, and how the close
 * decision interacts with the count of passes. Three players; the dealer is player 2; player 0 to play.
 */
public class SchwimmenCloseTest {

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

    private void act(AbstractAction action) {
        fm.next(state, action);
    }

    private void assertCloseDecisionFor(int player) {
        assertEquals("the same player decides whether to close", player, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(CLOSE_CHOICES, new HashSet<>(actions));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void afterExchangingOneCardThePlayerDecidesWhetherToClose() {
        act(new ExchangeOne(card("KS"), card("AH")));
        assertEquals(Set.copyOf(cards("7H", "9H", "AH")), new HashSet<>(state.getPlayerHand(0).getComponents()));
        assertCloseDecisionFor(0);
    }

    @Test
    public void afterExchangingAllCardsThePlayerDecidesWhetherToClose() {
        act(new ExchangeAll());
        assertCloseDecisionFor(0);
    }

    @Test
    public void afterPassingThePlayerDecidesWhetherToClose() {
        act(new Pass());
        assertEquals(1, state.getConsecutivePasses());
        assertCloseDecisionFor(0);
    }

    @Test
    public void theDealersChoiceIsNotFollowedByACloseDecision() {
        SchwimmenGameState fresh = newState(3, ordinarySeed(3, 0), fm);
        fm.next(fresh, new ChooseHand(false));
        // straight to player 0's turn: 9 single exchanges, exchange all and pass
        assertEquals(0, fresh.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(fresh);
        assertEquals(11, actions.size());
        assertFalse(actions.contains(new Close(true)));
        assertFalse(fresh.isActionInProgress());
    }

    @Test
    public void notClosingPassesTheTurnOn() {
        act(new ExchangeOne(card("KS"), card("AH")));
        act(new Close(false));
        assertEquals(-1, state.getCloser());
        assertEquals(1, state.getCurrentPlayer());
        // player 1's normal turn against the table KS 7C 10D
        assertEquals(turnActions(HAND1, h("KS", "7C", "10D")), new HashSet<>(fm.computeAvailableActions(state)));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void closingAfterAPassInTheFirstRoundRecordsTheCloserAndResetsThePasses() {
        act(new Pass());
        act(new Close(true));
        assertEquals(0, state.getCloser());
        assertEquals("a close resets the count of passes", 0, state.getConsecutivePasses());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(turnActions(HAND1, TABLE), new HashSet<>(fm.computeAvailableActions(state)));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void onceSomeoneHasClosedNobodyElseIsOfferedTheCloseDecision() {
        act(new Pass());
        act(new Close(true));
        // player 1 exchanges JS for AH: no close decision, straight on to player 2
        act(new ExchangeOne(card("JS"), card("AH")));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(turnActions(HAND2, h("JS", "7C", "10D")), new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals(0, state.getCloser());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void afterPlayerZeroClosesTheOtherTwoPlayOnceAndTheGameEnds() {
        act(new ExchangeOne(card("KS"), card("AH")));
        act(new Close(true));
        act(new ExchangeOne(card("JS"), card("KS")));   // player 1's last turn
        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getCurrentPlayer());
        act(new Pass());                                 // player 2's last turn
        // n - 1 = 2 turns after the close; play would return to the closer, who gets no further turn
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertAllCardsPresent(state);
    }

    @Test
    public void whenPlayerOneClosesTheLastTurnsGoRoundToPlayerZero() {
        state.setTurnOwner(1);
        act(new Pass());
        act(new Close(true));
        assertEquals(1, state.getCloser());
        assertEquals(2, state.getCurrentPlayer());
        act(new Pass());                                 // player 2 (the dealer)
        assertTrue(state.isNotTerminal());
        assertEquals("round past the dealer to player 0", 0, state.getCurrentPlayer());
        assertEquals(turnActions(HAND0, TABLE), new HashSet<>(fm.computeAvailableActions(state)));
        act(new Pass());                                 // player 0
        assertFalse("play would return to player 1, the closer", state.isNotTerminal());
    }

    @Test
    public void theTableIsNotReplacedUntilTheRoundCompletingPasserHasDeclinedToClose() {
        takeTurn(state, fm, new Pass());   // player 0: 1 pass
        takeTurn(state, fm, new Pass());   // player 1: 2 passes
        act(new Pass());                   // player 2: 3 passes = n
        // the all-pass check comes after the close decision
        assertCloseDecisionFor(2);
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(0, state.getDiscardPile().getSize());

        act(new Close(false));
        assertEquals("the old table cards are discarded", Set.copyOf(cards(TABLE)),
                new HashSet<>(state.getDiscardPile().getComponents()));
        assertEquals(3, state.getTable().getSize());
        assertEquals(0, state.getConsecutivePasses());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void closingOnTheRoundCompletingPassStopsTheTableBeingReplaced() {
        takeTurn(state, fm, new Pass());
        takeTurn(state, fm, new Pass());
        act(new Pass());                   // player 2: 3 passes = n
        act(new Close(true));
        // the close reset the count to 0, so no replacement
        assertEquals(2, state.getCloser());
        assertEquals(0, state.getConsecutivePasses());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentPlayer());

        act(new Pass());                   // player 0: 1 pass
        act(new Pass());                   // player 1: 2 passes < 3, and the game is over
        assertFalse(state.isNotTerminal());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(0, state.getDiscardPile().getSize());
    }

    @Test
    public void passesBeforeTheCloseDoNotCountTowardsReplacingTheTableInTheLastRound() {
        takeTurn(state, fm, new Pass());   // player 0: 1 pass
        act(new Pass());                   // player 1: 2 passes
        act(new Close(true));              // reset to 0
        act(new Pass());                   // player 2: 1 pass (it would be 3 without the reset)
        assertTrue(state.isNotTerminal());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        act(new Pass());                   // player 0: 2 passes; the last turn after player 1's close
        assertFalse(state.isNotTerminal());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(0, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void withTwoCardsLeftTheRoundCompletingPasserMayStillCloseAndKeepTheDealGoing() {
        leaveInDrawDeck(state, 2);
        takeTurn(state, fm, new Pass());
        takeTurn(state, fm, new Pass());
        act(new Pass());                   // player 2: 3 passes; the draw deck cannot replace the table
        assertCloseDecisionFor(2);
        act(new Close(true));
        // the close resets the count: players 0 and 1 have their last turns
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        act(new ExchangeOne(card("KS"), card("AH")));
        assertTrue(state.isNotTerminal());
        act(new Pass());
        assertFalse(state.isNotTerminal());
        assertEquals(2, state.getDrawDeck().getSize());
    }

    @Test
    public void withTwoCardsLeftDecliningToCloseOnTheRoundCompletingPassEndsTheGame() {
        leaveInDrawDeck(state, 2);
        takeTurn(state, fm, new Pass());
        takeTurn(state, fm, new Pass());
        act(new Pass());
        assertCloseDecisionFor(2);
        act(new Close(false));
        assertFalse(state.isNotTerminal());
        assertEquals(cards(TABLE), state.getTable().getComponents());
        assertEquals(-1, state.getCloser());
    }
}
