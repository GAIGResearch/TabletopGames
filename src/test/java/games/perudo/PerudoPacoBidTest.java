package games.perudo;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.perudo.actions.Bid;
import games.perudo.actions.Dudo;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.perudo.PerudoParameters.PACO;
import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Bidding on Pacos: switching from a face to Pacos and back, raising Pacos, and a Dudo on a Paco bid.
 */
public class PerudoPacoBidTest {

    PerudoForwardModel fm;
    PerudoGameState state;

    @Before
    public void setup() {
        fm = new PerudoForwardModel();
        state = newState(2, 3);   // 2 players, 5 dice each: 10 dice on the table; player 0 to act
    }

    private List<AbstractAction> actionsAfter(int quantity, int face) {
        state.setBid(quantity, face, 1);
        return fm.computeAvailableActions(state);
    }

    @Test
    public void fromFiveOfAValueTheLowestPacoBidIsThree() {
        // BGA: "5 of a value -> 3 Pacos". ceil(5 / 2) = 3, so 3..10 Pacos: 8 bids
        List<AbstractAction> actions = actionsAfter(5, 4);
        assertEquals(bidRange(3, 10, PACO, PACO), pacoBids(actions));
        assertTrue(actions.contains(new Bid(3, PACO)));
        assertFalse(actions.contains(new Bid(2, PACO)));    // 2 < 3
        // the face bids are unchanged by the Paco rule: five fives, five sixes, six to ten of any face 2..6
        Set<Bid> faces = bidRange(5, 5, 5, 6);
        faces.addAll(bidRange(6, 10, 2, 6));
        assertEquals(faces, faceBids(actions));
        assertTrue(actions.contains(new Dudo()));
    }

    @Test
    public void fiveThreesCanBeFollowedByThreePacos() {
        // Wikipedia: "five threes -> at least three aces"; ceil(5 / 2) = 3
        List<AbstractAction> actions = actionsAfter(5, 3);
        assertEquals(bidRange(3, 10, PACO, PACO), pacoBids(actions));
        assertFalse(actions.contains(new Bid(2, PACO)));
    }

    @Test
    public void halvingToPacosRoundsUp() {
        // Q = 4 -> ceil(4 / 2) = 2; Q = 5 -> ceil(5 / 2) = 3 (not floor = 2); Q = 1 -> ceil(1 / 2) = 1 (not 0);
        // Q = 2 -> 1; Q = 3 -> 2; Q = 6 -> 3
        int[][] lowest = {{1, 1}, {2, 1}, {3, 2}, {4, 2}, {5, 3}, {6, 3}};
        for (int[] ql : lowest) {
            List<AbstractAction> actions = actionsAfter(ql[0], 6);
            assertEquals("from " + ql[0] + " sixes", bidRange(ql[1], 10, PACO, PACO), pacoBids(actions));
        }
    }

    @Test
    public void pacoBidsDoNotExceedTheDiceOnTheTable() {
        // player 1 is down to 3 dice: 5 + 3 = 8 on the table; from seven sixes ceil(7 / 2) = 4, so 4..8 Pacos
        state.setDice(1, faces(1, 0, 1, 0, 0, 1));
        List<AbstractAction> actions = actionsAfter(7, 6);
        assertEquals(bidRange(4, 8, PACO, PACO), pacoBids(actions));
        assertFalse(actions.contains(new Bid(9, PACO)));
        assertEquals(bidRange(8, 8, 2, 6), faceBids(actions));   // eight of each face 2..6
    }

    @Test
    public void fromTenSixesOnlyPacosAndDudoRemain() {
        // no face bid above ten sixes; ceil(10 / 2) = 5, so 5..10 Pacos, and Dudo: 6 + 1 = 7 actions
        List<AbstractAction> actions = actionsAfter(10, 6);
        Set<AbstractAction> expected = new HashSet<>(bidRange(5, 10, PACO, PACO));
        expected.add(new Dudo());
        assertEquals(expected, new HashSet<>(actions));
        assertEquals(7, actions.size());
    }

    @Test
    public void fromThreePacosTheRaisesAreMorePacosOrSevenOfAValue() {
        // BGA: "3 Pacos -> 7 of a value" (2 * 3 + 1 = 7), 6 not enough. Wikipedia: "three aces -> four aces, or at
        // least seven fours". Pacos 4..10: 7 bids; faces 2..6 with 7..10 dice: 5 * 4 = 20 bids; Dudo: 28 actions
        List<AbstractAction> actions = actionsAfter(3, PACO);
        Set<AbstractAction> expected = new HashSet<>(bidRange(4, 10, PACO, PACO));
        expected.addAll(bidRange(7, 10, 2, 6));
        expected.add(new Dudo());
        assertEquals(28, expected.size());
        assertEquals(expected, new HashSet<>(actions));
        assertEquals(28, actions.size());

        assertTrue(actions.contains(new Bid(4, PACO)));
        assertTrue(actions.contains(new Bid(7, 4)));      // seven fours
        assertTrue(actions.contains(new Bid(7, 2)));
        assertFalse(actions.contains(new Bid(6, 4)));     // 6 < 7
        assertFalse(actions.contains(new Bid(4, 5)));     // more dice than 3, but not 2 * 3 + 1
        assertFalse(actions.contains(new Bid(3, 6)));     // same quantity, "higher face" does not apply from Pacos
        assertFalse(actions.contains(new Bid(3, PACO)));  // the same bid
        assertFalse(actions.contains(new Bid(2, PACO)));
    }

    @Test
    public void fromOnePacoFaceBidsNeedThreeDice() {
        // 2 * 1 + 1 = 3: faces 2..6 with 3..10 dice (5 * 8 = 40), Pacos 2..10 (9), Dudo: 50 actions
        List<AbstractAction> actions = actionsAfter(1, PACO);
        assertEquals(bidRange(3, 10, 2, 6), faceBids(actions));
        assertEquals(bidRange(2, 10, PACO, PACO), pacoBids(actions));
        assertTrue(actions.contains(new Dudo()));
        assertEquals(50, actions.size());
    }

    @Test
    public void fromFivePacosOnTenDiceOnlyMorePacosAndDudoRemain() {
        // 2 * 5 + 1 = 11 > 10 dice: no face bid; Pacos 6..10 (5) and Dudo
        List<AbstractAction> actions = actionsAfter(5, PACO);
        Set<AbstractAction> expected = new HashSet<>(bidRange(6, 10, PACO, PACO));
        expected.add(new Dudo());
        assertEquals(expected, new HashSet<>(actions));
        assertEquals(6, actions.size());
    }

    @Test
    public void fromFourPacosOnTenDiceOnlyNineOrTenOfAValue() {
        // 2 * 4 + 1 = 9 <= 10: faces 2..6 with 9 or 10 dice (10 bids), Pacos 5..10 (6), Dudo: 17 actions
        List<AbstractAction> actions = actionsAfter(4, PACO);
        assertEquals(bidRange(9, 10, 2, 6), faceBids(actions));
        assertEquals(bidRange(5, 10, PACO, PACO), pacoBids(actions));
        assertEquals(17, actions.size());
    }

    @Test
    public void atTenPacosOnTenDiceOnlyDudoIsLegal() {
        // the highest bid of all: no more Pacos (11 > 10), no face bid (21 > 10)
        List<AbstractAction> actions = actionsAfter(10, PACO);
        assertEquals(List.of(new Dudo()), actions);
    }

    @Test
    public void openingBidIsStillNeverOnPacos() {
        // no bid yet: Pacos are not offered at any quantity
        assertEquals(Set.of(), pacoBids(fm.computeAvailableActions(state)));
    }

    @Test
    public void countMatchingPacosCountsOnlyThePacos() {
        // player 0: two Pacos and three fives; player 1: four fives and a three. Pacos: 2 + 0 = 2
        setAllDice(state, faces(2, 0, 0, 0, 3, 0), faces(0, 0, 1, 0, 4, 0));
        assertEquals(2, state.countMatching(PACO));
    }

    @Test
    public void dudoOnThreePacosWithTwoPacosShowingCostsTheBidder() {
        // 2 Pacos and 4 fives on the table: a bid of 3 Pacos counts only the 2 Pacos (not 2 + 4 = 6), 2 < 3
        setAllDice(state, faces(1, 0, 1, 0, 2, 1), faces(1, 1, 0, 1, 2, 0));
        state.setBid(3, PACO, 1);
        new Dudo().execute(state);
        assertEquals(5, state.getNDice(0));   // player 0 called rightly
        assertEquals(4, state.getNDice(1));   // the bidder loses a die
        assertEquals(1, state.getNextRoundStarter());
    }

    @Test
    public void dudoOnTwoPacosWithTwoPacosShowingCostsTheCaller() {
        // the same dice: 2 Pacos == 2, the bid was right
        setAllDice(state, faces(1, 0, 1, 0, 2, 1), faces(1, 1, 0, 1, 2, 0));
        state.setBid(2, PACO, 1);
        new Dudo().execute(state);
        assertEquals(4, state.getNDice(0));
        assertEquals(5, state.getNDice(1));
        assertEquals(0, state.getNextRoundStarter());
    }

    @Test
    public void aRoundOfBiddingOnAndOffPacosInARealGame() {
        Game g = newGame(2, 21);
        AbstractForwardModel gfm = g.getForwardModel();
        PerudoGameState s = (PerudoGameState) g.getGameState();
        // 10 dice; each step must be offered to the player whose turn it is
        playLegal(gfm, s, 0, new Bid(4, 3));      // opening four threes
        playLegal(gfm, s, 1, new Bid(2, PACO));   // ceil(4 / 2) = 2 Pacos
        playLegal(gfm, s, 0, new Bid(5, 4));      // 2 * 2 + 1 = 5 fours
        playLegal(gfm, s, 1, new Bid(3, PACO));   // ceil(5 / 2) = 3 Pacos
        assertEquals(3, s.getBidQuantity());
        assertEquals(PACO, s.getBidFace());
        assertEquals(1, s.getBidder());
        assertFalse(gfm.computeAvailableActions(s).contains(new Bid(6, 4)));   // 6 < 2 * 3 + 1

        // 2 Pacos and 4 fives: three Pacos is wrong, so player 1 loses a die and starts the next round
        setAllDice(s, faces(1, 0, 1, 0, 2, 1), faces(1, 1, 0, 1, 2, 0));
        playLegal(gfm, s, 0, new Dudo());
        assertEquals(5, s.getNDice(0));
        assertEquals(4, s.getNDice(1));
        assertEquals(1, s.getRoundCounter());
        assertEquals(1, s.getCurrentPlayer());
        assertFalse(s.hasBid());
    }

    private static void playLegal(AbstractForwardModel gfm, PerudoGameState s, int player, AbstractAction action) {
        assertEquals(player, s.getCurrentPlayer());
        assertTrue(action + " should be legal", gfm.computeAvailableActions(s).contains(action));
        gfm.next(s, action);
    }
}
