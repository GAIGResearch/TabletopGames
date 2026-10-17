package games.pitch;

import core.actions.AbstractAction;
import games.pitch.actions.Bid;
import games.pitch.actions.Pass;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.pitch.PitchGameState.Phase.BIDDING;
import static games.pitch.PitchGameState.Phase.PLAYING;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * The auction: one round of bidding by players 0, 1, 2, then the dealer (3).
 */
public class PitchBiddingTest {

    PitchForwardModel fm;
    PitchGameState state;

    @Before
    public void setup() {
        fm = new PitchForwardModel();
        state = newState(11, fm);
    }

    private Set<AbstractAction> available() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    // ---------------------------------------------------------------- legal bids (arranged, unit)

    @Test
    public void firstBidderMayPassOrBidTwoToFive() {
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new Bid(2), new Bid(3), new Bid(4), new Bid(5)), available());
    }

    @Test
    public void aNonDealerMustBeatTheHighestBid() {
        // player 0 bid 3: player 1 may bid only 4 or 5
        arrangeBids(state, 3);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new Bid(4), new Bid(5)), available());

        // player 0 passed and player 1 bid 3: player 2 may not equal it either
        arrangeBids(state, 0, 3);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new Bid(4), new Bid(5)), available());
    }

    @Test
    public void aNonDealerMayPassWhenEveryoneBeforeHasPassed() {
        // only the dealer is stuck with a bid: player 2 after two passes has the full choice
        arrangeBids(state, 0, 0);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new Bid(2), new Bid(3), new Bid(4), new Bid(5)), available());
    }

    @Test
    public void theDealerMayEqualTheHighestBid() {
        // bids 0: 3, 1: pass, 2: pass - the dealer may pass, steal at 3, or bid higher
        arrangeBids(state, 3, 0, 0);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new Bid(3), new Bid(4), new Bid(5)), available());
    }

    @Test
    public void theDealerMustBidWhenTheOtherThreePassed() {
        arrangeBids(state, 0, 0, 0);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new Bid(2), new Bid(3), new Bid(4), new Bid(5)), available());
    }

    @Test
    public void afterABidOfFiveOnlyTheDealerMayStillBid() {
        // player 0 bid 5: player 1 can only pass
        arrangeBids(state, 5);
        assertEquals(Set.of(new Pass()), available());
        // 5, pass, pass: the dealer may pass or steal at 5
        arrangeBids(state, 5, 0, 0);
        assertEquals(Set.of(new Pass(), new Bid(5)), available());
    }

    // ---------------------------------------------------------------- effects of Pass and Bid (fm.next)

    @Test
    public void aBidIsRecordedAndMakesThePitcher() {
        fm.next(state, new Bid(3));
        assertEquals(3, state.getPlayerBid(0));
        assertEquals(0, state.getPitcher());
        assertEquals(3, state.getHighestBid());
        assertEquals(BIDDING, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());

        // a higher bid takes over as pitcher; the earlier bid stays recorded
        fm.next(state, new Bid(4));
        assertEquals(3, state.getPlayerBid(0));
        assertEquals(4, state.getPlayerBid(1));
        assertEquals(1, state.getPitcher());
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void aPassIsRecordedAsZero() {
        fm.next(state, new Pass());
        assertEquals(0, state.getPlayerBid(0));
        assertEquals(-1, state.getPitcher());
        assertEquals(BIDDING, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    // ---------------------------------------------------------------- whole auctions (fm.next, integration)

    @Test
    public void theDealerStealsByEqualling() {
        fm.next(state, new Bid(2));   // player 0
        fm.next(state, new Bid(3));   // player 1
        fm.next(state, new Pass());   // player 2
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(BIDDING, state.getGamePhase());
        fm.next(state, new Bid(3));   // the dealer equals player 1's 3
        assertArrayEquals(new int[]{2, 3, 0, 3}, state.playerBids);
        assertEquals(3, state.getPitcher());
        assertEquals(PLAYING, state.getGamePhase());
        assertEquals("the pitcher leads", 3, state.getCurrentPlayer());
        assertNull(state.getTrumpSuit());
    }

    @Test
    public void aNonDealerPitcherLeadsWhenTheDealerPasses() {
        fm.next(state, new Pass());   // player 0
        fm.next(state, new Bid(4));   // player 1
        fm.next(state, new Pass());   // player 2
        fm.next(state, new Pass());   // the dealer
        assertArrayEquals(new int[]{0, 4, 0, 0}, state.playerBids);
        assertEquals(1, state.getPitcher());
        assertEquals(PLAYING, state.getGamePhase());
        assertEquals("the pitcher leads", 1, state.getCurrentPlayer());
    }

    @Test
    public void aStuckDealerBecomesThePitcher() {
        fm.next(state, new Pass());
        fm.next(state, new Pass());
        fm.next(state, new Pass());
        fm.next(state, new Bid(2));   // the dealer, who could not pass
        assertArrayEquals(new int[]{0, 0, 0, 2}, state.playerBids);
        assertEquals(3, state.getPitcher());
        assertEquals(PLAYING, state.getGamePhase());
        assertEquals(3, state.getCurrentPlayer());
    }
}
