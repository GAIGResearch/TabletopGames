package games.pitch;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

public class PitchSetupTest {

    PitchForwardModel fm;
    PitchGameState state;

    @Before
    public void setup() {
        fm = new PitchForwardModel();
        state = newState(3, fm);
    }

    @Test
    public void setupDealsSixCardsEachAndPlayerZeroBidsFirst() {
        for (int p = 0; p < 4; p++)
            assertEquals(6, state.getPlayerHand(p).getSize());
        // 52 - 4 * 6 = 28 cards not dealt
        assertEquals(28, state.getUndealtDeck().getSize());
        assertAllCardsPresent(state);

        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getTeamTricks(0).getSize());
        assertEquals(0, state.getTeamTricks(1).getSize());
        for (int p = 0; p < 4; p++)
            assertEquals(-1, state.getPlayerBid(p));
        assertEquals(-1, state.getPitcher());
        assertNull(state.getTrumpSuit());
        assertEquals(0, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));

        assertEquals(PitchGameState.Phase.BIDDING, state.getGamePhase());
        assertEquals(3, state.getDealer());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void theDealIsShuffled() {
        // two set-ups differing only in the seed deal different hands
        PitchGameState other = newState(4, fm);
        Set<FrenchCard> hand0 = new HashSet<>(state.getPlayerHand(0).getComponents());
        Set<FrenchCard> otherHand0 = new HashSet<>(other.getPlayerHand(0).getComponents());
        assertNotEquals(hand0, otherHand0);
    }
}
