package games.scarto;

import org.junit.Test;

import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * The state after setup: the deal, the scarto, empty cardsWon, dealer and first player.
 */
public class ScartoSetupTest {

    ScartoForwardModel fm = new ScartoForwardModel();

    @Test
    public void eachPlayerIsDealtTwentyFiveAndThreeFormTheScarto() {
        ScartoGameState state = newState(1, fm);
        for (int p = 0; p < 3; p++) {
            assertEquals("hand " + p, 25, state.getPlayerHand(p).getSize());
            assertEquals("cards won " + p, 0, state.getCardsWon(p).getSize());
        }
        // 78 - 3 x 25 = 3
        assertEquals(3, state.getScarto().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void playerTwoDealsAndPlayerZeroLeads() {
        ScartoGameState state = newState(1, fm);
        assertEquals(2, state.getDealer());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());
    }

    @Test
    public void theDealDependsOnTheRandomSeed() {
        // two setups differing only in the seed deal player 0 different hands
        ScartoGameState a = newState(1, fm);
        ScartoGameState b = newState(2, fm);
        assertNotEquals(setOf(a.getPlayerHand(0)), setOf(b.getPlayerHand(0)));
    }
}
