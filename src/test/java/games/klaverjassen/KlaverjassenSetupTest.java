package games.klaverjassen;

import org.junit.Test;

import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.cardsOf;
import static org.junit.Assert.*;

public class KlaverjassenSetupTest {

    @Test
    public void setupDealsEightCardsEachFromThe32CardPackAndPlayerZeroIsToChooseTrumps() {
        KlaverjassenGameState state = newState(42);
        for (int p = 0; p < 4; p++)
            assertEquals(8, state.getPlayerHand(p).getSize());
        assertAllCardsPresent(state);   // 32 distinct cards, Seven to Ace: no 2-6
        assertEquals(0, state.getDiscardPile().getSize());

        // player 3 deals the first hand; player 0, on the dealer's left, chooses trumps and leads
        assertEquals(3, state.getDealer());
        assertEquals(0, state.getTrumpChooser());
        assertNull(state.getTrumpSuit());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getCurrentTrick().getLeader());

        for (int team = 0; team < 2; team++) {
            assertEquals(0, state.getHandPoints(team));
            assertEquals(0, state.getHandRoem(team));
            assertEquals(0, state.getTricksWon(team));
            assertEquals(0, state.getTeamScore(team));
        }
        for (int p = 0; p < 4; p++)
            assertTrue(state.getKnownVoids().get(p).isEmpty());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void differentSeedsGiveDifferentDeals() {
        assertNotEquals(cardsOf(newState(1).getPlayerHand(0)), cardsOf(newState(2).getPlayerHand(0)));
    }
}
