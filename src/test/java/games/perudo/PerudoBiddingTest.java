package games.perudo;

import core.actions.AbstractAction;
import games.perudo.actions.Bid;
import games.perudo.actions.Dudo;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * The legal bids (opening and raises), what a bid records, and the count of matching dice.
 */
public class PerudoBiddingTest {

    PerudoForwardModel fm;
    PerudoGameState state;

    @Before
    public void setup() {
        fm = new PerudoForwardModel();
        state = newState(2, 3);   // 2 players, 5 dice each: 10 dice on the table
    }

    @Test
    public void openingBidIsAnyQuantityUpToTotalDiceOnFacesTwoToSix() {
        // no bid yet: quantities 1..10 on faces 2..6 = 10 * 5 = 50 bids; no Paco bid, no Dudo
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(50, actions.size());
        assertEquals(bidRange(1, 10, 2, 6), new HashSet<>(actions));
        assertFalse(actions.contains(new Dudo()));
    }

    @Test
    public void openingBidQuantityIsLimitedByTheDiceStillOnTheTable() {
        // player 1 is down to 2 dice: 5 + 2 = 7 on the table, so 7 * 5 = 35 opening bids
        state.setDice(1, faces(0, 1, 0, 0, 1, 0));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(1, 7, 2, 6), new HashSet<>(actions));
        assertEquals(35, actions.size());
    }

    @Test
    public void raisesAfterFiveThreesAreMoreDiceOrTheSameNumberOfAHigherFace() {
        // player 1 bid five threes; player 0 to act, 10 dice on the table
        state.setBid(5, 3, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // five fours, fives, sixes (same quantity, higher face): 3 bids;
        // six to ten of any face 2..6: 5 * 5 = 25 bids; 28 face bids in all
        Set<Bid> expected = bidRange(5, 5, 4, 6);
        expected.addAll(bidRange(6, 10, 2, 6));
        assertEquals(28, expected.size());
        assertEquals(expected, faceBids(actions));
        assertTrue(actions.contains(new Dudo()));

        // Wikipedia's examples
        assertTrue(actions.contains(new Bid(5, 4)));
        assertTrue(actions.contains(new Bid(5, 6)));    // five sixes
        assertTrue(actions.contains(new Bid(6, 2)));    // six twos
        assertTrue(actions.contains(new Bid(6, 3)));    // six threes
        assertTrue(actions.contains(new Bid(10, 6)));
        assertFalse(actions.contains(new Bid(5, 3)));   // the same bid
        assertFalse(actions.contains(new Bid(5, 2)));   // same quantity, lower face
        assertFalse(actions.contains(new Bid(4, 6)));   // fewer dice
        assertFalse(actions.contains(new Bid(11, 2)));  // more dice than the table holds
    }

    @Test
    public void raisesStopAtTheDiceOnTheTable() {
        // 5 + 3 = 8 dice; from seven sixes the only face bids left are eight of each face 2..6
        state.setDice(1, faces(1, 0, 1, 0, 0, 1));
        state.setBid(7, 6, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(8, 8, 2, 6), faceBids(actions));
        assertTrue(actions.contains(new Dudo()));
    }

    @Test
    public void atTheHighestFaceBidOnlyDudoRemainsAmongTheFaceBids() {
        // ten sixes: no more dice, no higher face
        state.setBid(10, 6, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(), faceBids(actions));
        assertTrue(actions.contains(new Dudo()));
    }

    @Test
    public void bidRecordsTheBidAndTheBidderAndPassesTheTurn() {
        fm.next(state, new Bid(3, 4));
        assertTrue(state.hasBid());
        assertEquals(3, state.getBidQuantity());
        assertEquals(4, state.getBidFace());
        assertEquals(0, state.getBidder());
        assertEquals(1, state.getCurrentPlayer());

        fm.next(state, new Bid(4, 2));
        assertEquals(4, state.getBidQuantity());
        assertEquals(2, state.getBidFace());
        assertEquals(1, state.getBidder());
        assertEquals(0, state.getCurrentPlayer());   // wraps round to player 0
        assertEquals(0, state.getRoundCounter());    // a bid does not end the round
    }

    @Test
    public void countMatchingCountsTheFaceAndThePacos() {
        // player 0: two Pacos, a three, two fives; player 1: a Paco, two threes, a four, a six
        setAllDice(state, faces(2, 0, 1, 0, 2, 0), faces(1, 0, 2, 1, 0, 1));
        // Pacos: 2 + 1 = 3 on the table, counted for every face
        assertEquals(1 + 2 + 3, state.countMatching(3));   // "five threes" counts threes and ones: 6
        assertEquals(0 + 0 + 3, state.countMatching(2));   // no twos, just the Pacos
        assertEquals(0 + 1 + 3, state.countMatching(4));
        assertEquals(2 + 0 + 3, state.countMatching(5));
        assertEquals(0 + 1 + 3, state.countMatching(6));
    }

    @Test
    public void countMatchingWithoutPacosIsTheFaceAlone() {
        setAllDice(state, faces(0, 0, 1, 0, 4, 0), faces(0, 0, 2, 1, 0, 2));
        assertEquals(1 + 2, state.countMatching(3));
        assertEquals(4, state.countMatching(5));
        assertEquals(0, state.countMatching(2));
    }
}
