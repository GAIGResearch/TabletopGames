package games.poker;

import core.actions.AbstractAction;
import games.poker.actions.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * When a player goes All In and only one other player can still act, that player must still Call or Fold if the
 * All In is bigger than their own bet. The round only ends when no-one has a decision left.
 */
public class AllInResponseTest {

    PokerGameState state;
    PokerForwardModel fm = new PokerForwardModel();

    private void setup(int nPlayers) {
        state = new PokerGameState(new PokerGameParameters(), nPlayers);
        fm.setup(state);
    }

    // The round has ended: either the next one has started, or a player lost everything and the game is over
    private void assertRoundZeroEnded() {
        if (state.isNotTerminal())
            assertEquals(1, state.getRoundCounter());
        else
            assertEquals(0, state.getRoundCounter());
    }

    private void assertFacesCallOrFold(int player) {
        assertEquals(0, state.getRoundCounter());
        assertEquals(player, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new Call(player)));
        assertTrue(actions.contains(new Fold(player)));
        assertFalse(actions.contains(new Check(player)));
        assertTrue(actions.stream().noneMatch(a -> a instanceof Raise || a instanceof Bet));
    }

    @Test
    public void headsUpPlayerCanCallAnAllIn() {
        setup(2);
        // player 0 is the Small Blind and acts first; player 1 the Big Blind
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new AllIn(0));
        assertFacesCallOrFold(1);

        fm.next(state, new Call(1));
        assertRoundZeroEnded();
    }

    @Test
    public void headsUpPlayerCanFoldToAnAllIn() {
        setup(2);
        fm.next(state, new AllIn(0));
        assertFacesCallOrFold(1);

        fm.next(state, new Fold(1));
        assertEquals(1, state.getRoundCounter());
        // player 0 wins the Big Blind of 10 (and has now put some of their money in as a blind for round 1)
        assertEquals(60, state.getPlayerMoney()[0].getValue() + state.getPlayerBet()[0].getValue());
    }

    @Test
    public void lastPlayerAbleToActCanCallAnAllIn() {
        setup(3);
        // player 0 acts first, player 1 is the Small Blind, player 2 the Big Blind
        assertEquals(1, state.getSmallId());
        assertEquals(2, state.getBigId());
        fm.next(state, new Fold(0));
        fm.next(state, new AllIn(1));
        assertFacesCallOrFold(2);

        fm.next(state, new Call(2));
        assertRoundZeroEnded();
    }

    @Test
    public void roundEndsAtOnceIfTheAllInIsNoBiggerThanTheOtherBet() {
        setup(2);
        // player 0 has only 2 left after the Small Blind, so their All In (7 in all) does not cover the Big Blind of 10
        state.getPlayerMoney()[0].decrement(43);
        state.getPlayerMoney()[1].increment(43);
        fm.next(state, new AllIn(0));
        assertRoundZeroEnded();
    }
}
