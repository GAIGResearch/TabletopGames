package games.whist;

import org.junit.Test;

import static games.tricktaking.TrickTakingTestUtils.cardsOf;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

public class WhistSetupTest {

    @Test
    public void setupDealsThirteenCardsEachAndTurnsUpTheDealersLastCard() {
        WhistGameState state = newState(42);
        for (int p = 0; p < 4; p++)
            assertEquals(13, state.getPlayerHand(p).getSize());
        assertAllCardsPresent(state);   // 52 distinct cards
        assertEquals(0, state.getDiscardPile().getSize());

        // player 3 deals the first deal and holds the turned-up card; its suit is trumps
        assertEquals(3, state.getDealer());
        assertNotNull(state.getTrumpCard());
        assertTrue(state.getPlayerHand(3).contains(state.getTrumpCard()));
        assertEquals(state.getTrumpCard().suite, state.getTrumpSuit());

        // player 0, on the dealer's left, leads an empty trick
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getCurrentTrick().getLeader());
        for (int p = 0; p < 4; p++)
            assertEquals(0, state.getTricksTaken(p));
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(0, state.getTeamPoints(1));
        assertEquals(0, state.getTeamTricks(0));
        assertEquals(0, state.getTeamTricks(1));
    }

    @Test
    public void differentSeedsGiveDifferentDeals() {
        assertNotEquals(cardsOf(newState(1).getPlayerHand(0)), cardsOf(newState(2).getPlayerHand(0)));
    }
}
