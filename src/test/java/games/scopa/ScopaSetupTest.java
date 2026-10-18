package games.scopa;

import org.junit.Test;

import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

public class ScopaSetupTest {

    ScopaForwardModel fm = new ScopaForwardModel();

    @Test
    public void setupDealsFourToTheTableThreeToEachPlayerAndLeavesThirty() {
        ScopaGameState state = newState(3, fm);
        assertAllCardsPresent(state);
        assertEquals(4, state.getTable().getSize());
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(3, state.getPlayerHand(1).getSize());
        // 40 - 4 - 3 - 3 = 30
        assertEquals(30, state.getDrawDeck().getSize());
        assertEquals(0, state.getCapturedCards(0).getSize());
        assertEquals(0, state.getCapturedCards(1).getSize());
    }

    @Test
    public void playerZeroPlaysFirstAndPlayerOneDeals() {
        ScopaGameState state = newState(3, fm);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getDealer());
        assertEquals(0, state.getScopas(0));
        assertEquals(0, state.getScopas(1));
        assertEquals(-1, state.getLastCapturer());
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
    }

    @Test
    public void theFirstDealIsShuffled() {
        // two set-ups differing only in the seed deal different tables and hands
        ScopaGameState a = newState(1, fm);
        ScopaGameState b = newState(2, fm);
        assertNotEquals(a.getDrawDeck().getComponents(), b.getDrawDeck().getComponents());
    }
}
