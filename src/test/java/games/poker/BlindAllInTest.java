package games.poker;

import core.actions.AbstractAction;
import games.poker.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * With two players left, a blind may put a player All In, so that only the other player can act in that round.
 * They still take a turn, and one action never ends more than one round.
 */
public class BlindAllInTest {

    PokerGameState state;
    PokerForwardModel fm;

    @Before
    public void setup() {
        fm = new PokerForwardModel();
        state = new PokerGameState(new PokerGameParameters(), 2);
        fm.setup(state);
        // Round 0: player 0 is the Small Blind (and acts first), player 1 the Big Blind
        assertEquals(0, state.getSmallId());
        assertEquals(1, state.getBigId());
        assertEquals(0, state.getCurrentPlayer());
    }

    // The round has ended: either the next one has started, or the All In player lost everything and the game is over
    private void assertRoundOneEnded() {
        if (state.isNotTerminal())
            assertEquals(2, state.getRoundCounter());
        else
            assertEquals(1, state.getRoundCounter());
    }

    private void transferMoney(int from, int to, int amount) {
        state.playerMoney[from].decrement(amount);
        state.playerMoney[to].increment(amount);
    }

    @Test
    public void bigBlindAllInForLessThanSmallBlindLeavesSmallBlindJustToCheck() {
        // player 0 is left with 2 after folding, so cannot cover the Big Blind of 10 (or even the Small Blind of 5)
        transferMoney(0, 1, 43);
        fm.next(state, new Fold(0));

        // Round 1: player 1 is the Small Blind, player 0 goes All In on the Big Blind for 2
        assertEquals(1, state.getRoundCounter());
        assertTrue(state.playerAllIn[0]);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(new Check(1)), fm.computeAvailableActions(state));

        fm.next(state, new Check(1));
        assertRoundOneEnded();
    }

    @Test
    public void bigBlindAllInForMoreThanSmallBlindLeavesSmallBlindToCallOrFold() {
        // player 0 is left with 7 after folding, which is more than the Small Blind of 5 but less than the Big Blind
        transferMoney(0, 1, 38);
        fm.next(state, new Fold(0));

        assertEquals(1, state.getRoundCounter());
        assertTrue(state.playerAllIn[0]);
        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new Call(1)));
        assertTrue(actions.contains(new Fold(1)));
        assertFalse(actions.contains(new Check(1)));
        assertTrue(actions.stream().noneMatch(a -> a instanceof Raise || a instanceof Bet));

        fm.next(state, new Fold(1));
        assertEquals(2, state.getRoundCounter());
        // player 0 won the pot of 12 (and has now put some of it in as a blind for round 2)
        assertEquals(12, state.playerMoney[0].getValue() + state.getPlayerBet()[0].getValue());
    }

    @Test
    public void smallBlindAllInPassesTheTurnToTheBigBlind() {
        // player 1 (Big Blind) is left with 2 after player 0 calls and player 1 folds
        transferMoney(1, 0, 38);
        fm.next(state, new Call(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Fold(1));

        // Round 1: player 1 is the first player and Small Blind, but goes All In for 2; player 0 must act
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getFirstPlayer());
        assertTrue(state.playerAllIn[1]);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new Check(0)), fm.computeAvailableActions(state));

        fm.next(state, new Check(0));
        assertRoundOneEnded();
    }
}
