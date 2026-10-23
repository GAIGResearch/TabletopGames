package games.monopoly;

import core.actions.AbstractAction;
import games.monopoly.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Auctions: of a property the lander declines, and of each property of a player bankrupt to the Bank. Four players;
 * player 1 lands on The Angel Islington (price 100) from GO, so player 2 bids first. MonopolyParameters.minimumBid is
 * 10 and bidIncrements 10, 50, 100 unless stated.
 */
public class MonopolyAuctionTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare angel = sq("The Angel Islington"), oldKent = sq("Old Kent Road"),
            whitechapel = sq("Whitechapel Road");

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(4, 7, params);
        fm = new MonopolyForwardModel();
        startTurn(state, 1, sq("GO"));
    }

    /** Player 1 rolls 2 + 4 from GO to The Angel Islington and declines it. */
    private void declineAngel() {
        roll(state, fm, 2, 4);
        fm.next(state, new DeclineProperty(angel));
    }

    private Auction auction() {
        assertTrue("no auction in progress: " + state.currentActionInProgress(),
                state.currentActionInProgress() instanceof Auction);
        return (Auction) state.currentActionInProgress();
    }

    // ---- starting an auction, and the bidders ----

    @Test
    public void decliningStartsAnAuctionWithThePlayerAfterTheLanderBiddingFirst() {
        declineAngel();
        assertEquals(new Auction(state, angel, 2), state.currentActionInProgress());
        Auction auction = auction();
        assertEquals(angel, auction.square);
        assertEquals(-1, auction.getHighBidder());
        for (int p = 0; p < 4; p++)
            assertFalse(auction.isOut(p));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(-1, state.getOwner(angel));
        // the opening bid is minimumBid (10)
        assertEquals(Set.of(new Bid(10), new PassBid()), actionSet(fm, state));
    }

    @Test
    public void biddersTakeTurnsRoundTheTableFromThePlayerAfterTheLander() {
        declineAngel();
        List<Integer> order = new ArrayList<>();
        order.add(state.getCurrentPlayer());
        for (AbstractAction a : new AbstractAction[]{new Bid(10), new PassBid(), new Bid(20), new Bid(30), new PassBid()}) {
            play(state, fm, a);
            order.add(state.getCurrentPlayer());
        }
        // 2 bids, 3 passes, 0 and the lander 1 raise, 2 passes; then 0 (3 is out) - 1 is high bidder
        assertEquals(List.of(2, 3, 0, 1, 2, 0), order);
        assertEquals(30, auction().getHighBid());
        assertEquals(1, auction().getHighBidder());
    }

    @Test
    public void theOpeningBidIsTheMinimumBidParameter() {
        params.setParameterValue("minimumBid", 50);
        declineAngel();
        assertEquals(Set.of(new Bid(50), new PassBid()), actionSet(fm, state));
        // after a pass, still no bid: the next bidder too may only open at 50
        play(state, fm, new PassBid());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new Bid(50), new PassBid()), actionSet(fm, state));
    }

    @Test
    public void raisesAreByTheBidIncrementsOnly() {
        declineAngel();
        play(state, fm, new Bid(10));
        // 10 + 10 / 50 / 100
        assertEquals(Set.of(new Bid(20), new Bid(60), new Bid(110), new PassBid()), actionSet(fm, state));
        play(state, fm, new Bid(60));
        // 60 + 10 / 50 / 100
        assertEquals(Set.of(new Bid(70), new Bid(110), new Bid(160), new PassBid()), actionSet(fm, state));
    }

    @Test
    public void raisesFollowTheBidIncrementsParameter() {
        params.setParameterValue("bidIncrements", "5,25");
        declineAngel();
        play(state, fm, new Bid(10));
        // 10 + 5 / 25
        assertEquals(Set.of(new Bid(15), new Bid(35), new PassBid()), actionSet(fm, state));
    }

    @Test
    public void noBidIsOfferedAboveTheBiddersCash() {
        state.setCash(3, 59);
        state.setCash(0, 70);
        declineAngel();
        play(state, fm, new Bid(10));
        // player 3 (cash 59): 20 only - 60 and 110 are beyond the cash
        assertEquals(Set.of(new Bid(20), new PassBid()), actionSet(fm, state));
        play(state, fm, new Bid(20));
        // player 0 (cash 70): 30, and 70 exactly the cash; not 120
        assertEquals(Set.of(new Bid(30), new Bid(70), new PassBid()), actionSet(fm, state));
    }

    @Test
    public void aBidderWithLessThanTheOpeningBidCanOnlyPass() {
        state.setCash(2, 10 - 1);
        declineAngel();
        assertEquals(Set.of(new PassBid()), actionSet(fm, state));
    }

    @Test
    public void aPassIsFinal() {
        declineAngel();
        play(state, fm, new PassBid()); // player 2 out
        assertTrue(auction().isOut(2));
        // players 3, 0, 1 raise round the table twice; player 2 is never asked again
        for (int bid = 10; bid <= 60; bid += 10) {
            assertNotEquals(2, state.getCurrentPlayer());
            play(state, fm, new Bid(bid));
        }
        assertEquals(3, state.getCurrentPlayer());
        assertTrue(auction().isOut(2));
    }

    // ---- the end of the auction ----

    @Test
    public void theLastBidderLeftWinsAndPaysTheBank() {
        declineAngel();
        play(state, fm, new Bid(10), new Bid(60), new PassBid(), new PassBid(), new Bid(70));
        // 2: 10, 3: 60, 0 and 1 out, 2: 70; player 3 to raise or pass
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(70, auction().getHighBid());
        assertEquals(2, auction().getHighBidder());
        play(state, fm, new PassBid());
        assertFalse(state.isActionInProgress());
        assertEquals(2, state.getOwner(angel));
        assertEquals(1500 - 70, state.getCash(2));
        assertEquals(1500, state.getCash(3));
        assertEquals(1500, state.getCash(1));
        assertEquals(4 * 1500 - 70 /* to the Bank */, totalCash(state));
    }

    @Test
    public void theHighBidderIsNotAskedAgainWhenEveryoneElseHasPassed() {
        declineAngel();
        play(state, fm, new Bid(10), new PassBid(), new PassBid(), new PassBid());
        // 3, 0 and 1 passed: player 2 is the only one in, with the high bid - the auction is over
        assertFalse(state.isActionInProgress());
        assertEquals(2, state.getOwner(angel));
        assertEquals(1500 - 10, state.getCash(2));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void whenEveryonePassesTheBankKeepsTheProperty() {
        declineAngel();
        play(state, fm, new PassBid(), new PassBid(), new PassBid());
        // the lander, last with no bid made, still decides
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        play(state, fm, new PassBid());
        assertFalse(state.isActionInProgress());
        assertEquals(-1, state.getOwner(angel));
        assertEquals(4 * 1500, totalCash(state));
        for (int p = 0; p < 4; p++)
            assertTrue(state.getProperties(p).isEmpty());
    }

    @Test
    public void theLanderMayBidAndBuyBelowThePrice() {
        declineAngel();
        play(state, fm, new PassBid(), new PassBid(), new PassBid());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new Bid(10), new PassBid()), actionSet(fm, state));
        play(state, fm, new Bid(10));
        // the only player in, with the high bid
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getOwner(angel));
        assertEquals(1500 - 10 /* not the price, 100 */, state.getCash(1));
    }

    @Test
    public void aPlayerInJailMayBid() {
        putInJail(state, 3, 1);
        declineAngel();
        play(state, fm, new PassBid());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new Bid(10), new PassBid()), actionSet(fm, state));
        play(state, fm, new Bid(10), new PassBid(), new PassBid());
        assertFalse(state.isActionInProgress());
        assertEquals(3, state.getOwner(angel));
        assertEquals(1500 - 10, state.getCash(3));
        assertTrue(state.isInJail(3));
        assertEquals(1, state.getJailRolls(3));
    }

    @Test
    public void bankruptPlayersAreNotBidders() {
        eliminate(state, 2, 4);
        declineAngel();
        // player 2 is out of the game: player 3 bids first
        assertEquals(new Auction(state, angel, 3), state.currentActionInProgress());
        assertEquals(3, state.getCurrentPlayer());
        assertTrue(auction().isOut(2));
        play(state, fm, new Bid(10), new Bid(20), new PassBid());
        // 3, 0 bid, 1 passes; back to 3, skipping 2
        assertEquals(3, state.getCurrentPlayer());
        play(state, fm, new PassBid());
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getOwner(angel));
        assertEquals(1500 - 20, state.getCash(0));
    }

    // ---- the turn after the auction ----

    @Test
    public void theLandersTurnContinuesInManageAfterTheAuction() {
        declineAngel();
        play(state, fm, new Bid(10), new PassBid(), new PassBid(), new PassBid());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void afterAnAuctionOnADoubleTheLanderRollsAgain() {
        roll(state, fm, 3, 3); // GO + 6 = The Angel Islington
        fm.next(state, new DeclineProperty(angel));
        play(state, fm, new PassBid(), new Bid(10), new PassBid(), new PassBid());
        assertEquals(3, state.getOwner(angel));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    // ---- bankruptcy to the Bank ----

    @Test
    public void bankruptcyToTheBankAuctionsEachPropertyInBoardOrder() {
        startTurn(state, 3, sq("GO"));
        state.setCash(3, 50);
        give(state, 3, angel, oldKent, whitechapel);
        state.setMortgaged(whitechapel, true);
        // cash 50 + mortgage values 30 (Old Kent Road) + 50 (The Angel Islington) = 130 < 200
        roll(state, fm, 1, 3); // GO + 4 = Income Tax, 200
        assertTrue(state.isBankrupt(3));
        assertEquals(4, state.getFinalPlace(3));
        // all back with the Bank, unmortgaged, before the auctions
        for (MonopolySquare s : List.of(oldKent, whitechapel, angel))
            assertEquals(-1, state.getOwner(s));
        assertFalse(state.isMortgaged(whitechapel));

        // Old Kent Road (1) first; player 0, after the bankrupt 3, bids first; 3 is not a bidder
        assertEquals(new Auction(state, oldKent, 0), state.currentActionInProgress());
        assertTrue(auction().isOut(3));
        play(state, fm, new Bid(10), new PassBid(), new PassBid());
        assertEquals(0, state.getOwner(oldKent));

        // Whitechapel Road (3): nobody bids
        assertEquals(new Auction(state, whitechapel, 0), state.currentActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        play(state, fm, new PassBid(), new PassBid(), new PassBid());
        assertEquals(-1, state.getOwner(whitechapel));

        // The Angel Islington (6)
        assertEquals(new Auction(state, angel, 0), state.currentActionInProgress());
        play(state, fm, new PassBid(), new Bid(10), new Bid(60), new PassBid());
        assertEquals(2, state.getOwner(angel));

        assertFalse(state.isActionInProgress());
        assertEquals(1500 - 10, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        assertEquals(1500 - 60, state.getCash(2));
        // then the turn passes to the next player, as after any bankruptcy
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(GAME_ONGOING, state.getGameStatus());
    }

    @Test
    public void bankruptcyToTheBankLeavingOnePlayerEndsTheGameWithoutAnAuction() {
        state = newState(2, 7, params);
        startTurn(state, 0, sq("GO"));
        state.setCash(0, 50);
        give(state, 0, oldKent);
        roll(state, fm, 1, 3); // Income Tax 200 > 50 + 30
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertFalse(state.isActionInProgress());
        assertEquals(-1, state.getOwner(oldKent));
    }

    // ---- copy and equality ----

    @Test
    public void theAuctionCopiesIndependentlyAndEqualsByValue() {
        declineAngel();
        play(state, fm, new Bid(10));
        Auction auction = auction();
        Auction copy = auction.copy();
        assertNotSame(auction, copy);
        assertEquals(auction, copy);
        assertEquals(auction.hashCode(), copy.hashCode());

        MonopolyGameState stateCopy = (MonopolyGameState) state.copy();
        MonopolyGameState sameMoves = (MonopolyGameState) state.copy();
        assertEquals(state, stateCopy);
        play(stateCopy, fm, new Bid(60), new PassBid()); // 3 raises to 60, 0 passes
        Auction moved = (Auction) stateCopy.currentActionInProgress();
        assertEquals(60, moved.getHighBid());
        assertEquals(3, moved.getHighBidder());
        assertTrue(moved.isOut(0));
        assertEquals(1, stateCopy.getCurrentPlayer());
        // the original is untouched
        assertEquals(10, auction().getHighBid());
        assertEquals(2, auction().getHighBidder());
        assertFalse(auction().isOut(0));
        assertEquals(3, state.getCurrentPlayer());
        assertNotEquals(auction(), moved);
        // the same moves in another copy give an equal auction
        play(sameMoves, fm, new Bid(60), new PassBid());
        assertEquals(moved, sameMoves.currentActionInProgress());
        assertEquals(moved.hashCode(), sameMoves.currentActionInProgress().hashCode());
    }
}
